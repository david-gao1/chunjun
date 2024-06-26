/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtstack.chunjun.connector.gbase8s.source;

import com.dtstack.chunjun.config.SyncConfig;
import com.dtstack.chunjun.connector.gbase8s.dialect.Gbase8sDialect;
import com.dtstack.chunjun.connector.jdbc.source.JdbcSourceFactory;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import org.apache.commons.lang3.StringUtils;

public class Gbase8sSourceFactory extends JdbcSourceFactory {

    public Gbase8sSourceFactory(SyncConfig syncConfig, StreamExecutionEnvironment env) {
        super(syncConfig, env, new Gbase8sDialect());
        // 避免result.next阻塞
        if (jdbcConfig.isPolling()
                && StringUtils.isEmpty(jdbcConfig.getStartLocation())
                && jdbcConfig.getFetchSize() == 0) {
            jdbcConfig.setFetchSize(1000);
        }
    }
}
