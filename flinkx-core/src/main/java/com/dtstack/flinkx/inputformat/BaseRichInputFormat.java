/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtstack.flinkx.inputformat;

import com.dtstack.flinkx.converter.AbstractRowConverter;

import org.apache.flink.api.common.accumulators.LongCounter;
import org.apache.flink.api.common.io.DefaultInputSplitAssigner;
import org.apache.flink.api.common.io.RichInputFormat;
import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.core.io.InputSplitAssigner;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.streaming.api.operators.StreamingRuntimeContext;
import org.apache.flink.table.data.RowData;

import com.dtstack.flinkx.conf.FlinkxCommonConf;
import com.dtstack.flinkx.constants.Metrics;
import com.dtstack.flinkx.log.DtLogger;
import com.dtstack.flinkx.metrics.AccumulatorCollector;
import com.dtstack.flinkx.metrics.BaseMetric;
import com.dtstack.flinkx.metrics.CustomPrometheusReporter;
import com.dtstack.flinkx.restore.FormatState;
import com.dtstack.flinkx.source.ByteRateLimiter;
import com.dtstack.flinkx.util.ExceptionUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FlinkX里面所有自定义inputFormat的抽象基类
 *
 * 扩展了org.apache.flink.api.common.io.RichInputFormat, 因而可以通过{@link #getRuntimeContext()}获取运行时执行上下文
 * 自动完成
 * 用户只需覆盖openInternal,closeInternal等方法, 无需操心细节
 *
 * @author jiangbo
 */
public abstract class BaseRichInputFormat extends RichInputFormat<RowData, InputSplit> {

    protected final Logger LOG = LoggerFactory.getLogger(getClass());
    protected String jobName = "defaultJobName";
    protected String jobId;
    protected LongCounter numReadCounter;
    protected LongCounter bytesReadCounter;
    protected LongCounter durationCounter;
    protected ByteRateLimiter byteRateLimiter;

    protected FlinkxCommonConf config;

    protected FormatState formatState;

    protected transient BaseMetric inputMetric;

    protected int indexOfSubTask;

    protected long startTime;

    protected AccumulatorCollector accumulatorCollector;

    private boolean inited = false;

    private AtomicBoolean isClosed = new AtomicBoolean(false);

    protected transient CustomPrometheusReporter customPrometheusReporter;

    /** 环境上下文 */
    protected StreamingRuntimeContext context;

    /** 数据类型转换器 */
    protected AbstractRowConverter rowConverter;

    /** 状态恢复 */
    protected FunctionInitializationContext functionInitializationContext;

    /**
     * 有子类实现，打开数据连接
     *
     * @param inputSplit 分片
     * @throws IOException 连接异常
     */
    protected abstract void openInternal(InputSplit inputSplit) throws IOException;

    @Override
    public final void configure(Configuration parameters) {
        // do nothing
    }

    @Override
    public void openInputFormat() {
        initJobInfo();
        initPrometheusReporter();

        startTime = System.currentTimeMillis();
        DtLogger.config(config, jobId);
    }

    @Override
    public final InputSplit[] createInputSplits(int i) {
        try {
            return createInputSplitsInternal(i);
        } catch (Exception e){
            LOG.warn(ExceptionUtil.getErrorMessage(e));

            return createErrorInputSplit(e);
        }
    }

    private ErrorInputSplit[] createErrorInputSplit(Exception e){
        ErrorInputSplit[] inputSplits = new ErrorInputSplit[1];

        ErrorInputSplit errorInputSplit = new ErrorInputSplit(ExceptionUtil.getErrorMessage(e));
        inputSplits[0] = errorInputSplit;

        return inputSplits;
    }

    /**
     * 由子类实现，创建数据分片
     *
     * @param i 分片数量
     * @return 分片数组
     * @throws Exception 可能会出现连接数据源异常
     */
    protected abstract InputSplit[] createInputSplitsInternal(int i) throws Exception;

    @Override
    public void open(InputSplit inputSplit) throws IOException {
        this.context = (StreamingRuntimeContext) getRuntimeContext();
        checkIfCreateSplitFailed(inputSplit);

        if(!inited){
            initAccumulatorCollector();
            initStatisticsAccumulator();
            openByteRateLimiter();
            initRestoreInfo();
            inited = true;
        }

        openInternal(inputSplit);
    }

    private void checkIfCreateSplitFailed(InputSplit inputSplit){
        if (inputSplit instanceof ErrorInputSplit) {
            throw new RuntimeException(((ErrorInputSplit) inputSplit).getErrorMessage());
        }
    }

    private void initPrometheusReporter() {
        if (useCustomPrometheusReporter()) {
            customPrometheusReporter = new CustomPrometheusReporter(getRuntimeContext(), makeTaskFailedWhenReportFailed());
            customPrometheusReporter.open();
        }
    }

    protected boolean useCustomPrometheusReporter() {
        return false;
    }

    protected boolean makeTaskFailedWhenReportFailed(){
        return false;
    }

    private void initAccumulatorCollector(){
        String lastWriteLocation = String.format("%s_%s", Metrics.LAST_WRITE_LOCATION_PREFIX, indexOfSubTask);
        String lastWriteNum = String.format("%s_%s", Metrics.LAST_WRITE_NUM__PREFIX, indexOfSubTask);

        accumulatorCollector = new AccumulatorCollector(context,
                Arrays.asList(Metrics.NUM_READS,
                        Metrics.READ_BYTES,
                        Metrics.READ_DURATION,
                        Metrics.WRITE_BYTES,
                        Metrics.NUM_WRITES,
                        lastWriteLocation,
                        lastWriteNum));
        accumulatorCollector.start();
    }

    private void initJobInfo(){
        Map<String, String> vars = getRuntimeContext().getMetricGroup().getAllVariables();
        if(vars != null && vars.get(Metrics.JOB_NAME) != null) {
            jobName = vars.get(Metrics.JOB_NAME);
        }

        if(vars!= null && vars.get(Metrics.JOB_ID) != null) {
            jobId = vars.get(Metrics.JOB_ID);
        }

        if(vars != null && vars.get(Metrics.SUBTASK_INDEX) != null){
            indexOfSubTask = Integer.parseInt(vars.get(Metrics.SUBTASK_INDEX));
        }
    }

    private void openByteRateLimiter(){
        if (config.getBytes() > 0) {
            this.byteRateLimiter = new ByteRateLimiter(accumulatorCollector, config.getBytes());
            this.byteRateLimiter.start();
        }
    }

    private void initStatisticsAccumulator(){
        numReadCounter = getRuntimeContext().getLongCounter(Metrics.NUM_READS);
        bytesReadCounter = getRuntimeContext().getLongCounter(Metrics.READ_BYTES);
        durationCounter = getRuntimeContext().getLongCounter(Metrics.READ_DURATION);

        inputMetric = new BaseMetric(getRuntimeContext());
        inputMetric.addMetric(Metrics.NUM_READS, numReadCounter, true);
        inputMetric.addMetric(Metrics.READ_BYTES, bytesReadCounter, true);
        inputMetric.addMetric(Metrics.READ_DURATION, durationCounter);
    }

    private void initRestoreInfo(){
        if(formatState == null){
            formatState = new FormatState(indexOfSubTask, null);
        } else {
            numReadCounter.add(formatState.getMetricValue(Metrics.NUM_READS));
            bytesReadCounter.add(formatState.getMetricValue(Metrics.READ_BYTES));
            durationCounter.add(formatState.getMetricValue(Metrics.READ_DURATION));
        }
    }

    @Override
    public RowData nextRecord(RowData rowData) throws IOException {
        if(byteRateLimiter != null) {
            byteRateLimiter.acquire();
        }
        RowData internalRow = nextRecordInternal(rowData);
        if(internalRow != null){
            updateDuration();
            if(numReadCounter !=null ){
                numReadCounter.add(1);
            }
            if(bytesReadCounter!=null){
                bytesReadCounter.add(internalRow.toString().getBytes().length);
            }
        }

        return internalRow;
    }

    /**
     * Get the recover point of current channel
     * @return DataRecoverPoint
     */
    public FormatState getFormatState() {
        if (formatState != null && numReadCounter != null && inputMetric!= null) {
            formatState.setMetric(inputMetric.getMetricCounters());
        }
        return formatState;
    }

    /**
     * 由子类实现，读取一条数据
     *
     * @param rowData 需要创建和填充的数据
     * @return 读取的数据
     * @throws IOException 读取异常
     */
    protected abstract RowData nextRecordInternal(RowData rowData) throws IOException;

    @Override
    public void close() {
        try{
            closeInternal();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public void closeInputFormat() {
        if (isClosed.get()) {
            return;
        }

        if(durationCounter != null){
            updateDuration();
        }

        if(byteRateLimiter != null){
            byteRateLimiter.stop();
        }

        if(accumulatorCollector != null){
            accumulatorCollector.close();
        }

        if (useCustomPrometheusReporter() && null != customPrometheusReporter) {
            customPrometheusReporter.report();
        }

        if(inputMetric != null){
            inputMetric.waitForReportMetrics();
        }

        if (useCustomPrometheusReporter() && null != customPrometheusReporter) {
            customPrometheusReporter.close();
        }

        isClosed.set(true);
        LOG.info("subtask input close finished");
    }

    private void updateDuration(){
        if(durationCounter !=null ){
            durationCounter.resetLocal();
            durationCounter.add(System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 由子类实现，关闭资源
     *
     * @throws IOException 连接关闭异常
     */
    protected abstract  void closeInternal() throws IOException;

    @Override
    public final BaseStatistics getStatistics(BaseStatistics baseStatistics) {
        return null;
    }

    @Override
    public final InputSplitAssigner getInputSplitAssigner(InputSplit[] inputSplits) {
        return new DefaultInputSplitAssigner(inputSplits);
    }

    public void setRestoreState(FormatState formatState) {
        this.formatState = formatState;
    }

    public FlinkxCommonConf getConfig() {
        return config;
    }

    public void setConfig(FlinkxCommonConf config) {
        this.config = config;
    }

    public void setRowConverter(AbstractRowConverter rowConverter) {
        this.rowConverter = rowConverter;
    }

    public void setFunctionInitializationContext(FunctionInitializationContext functionInitializationContext) {
        this.functionInitializationContext = functionInitializationContext;
    }
}
