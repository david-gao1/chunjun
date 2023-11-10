CREATE TABLE source
(   `id` int,
 	`username` varchar,
 	`age` int
) WITH (
  'connector' = 'binlog-x'
      ,'username' = 'root'
      ,'password' = '11111111'
      ,'cat' = 'insert,delete,update'
      ,'url' = 'jdbc:mysql://10.17.31.234:3306/360test'
      ,'host' = '10.17.31.234'
      ,'port' = '3306'
      -- 什么都不加：最新位置消费
      -- 加文件名，从此文件开头消费
       ,'journal-name' = 'binlog.000194'
      --  ,'timestamp'='169944781200'
      ,'table' = '360test.dimension_table'
      ,'timestamp-format.standard' = 'SQL'
      );
CREATE TABLE sink
(   `id` int,
 	`name` varchar,
 	`age` int,
 	PRIMARY KEY (id,name) NOT ENFORCED
) WITH (
        'connector' = 'mysql-x',
           'url' = 'jdbc:mysql://localhost:3306/360test',
           'table-name' = 'test003',
           'username' = 'root',
           'password' = '11111111',
           'sink.buffer-flush.max-rows' = '1024', -- 批量写数据条数，默认：1024
           'sink.buffer-flush.interval' = '10000', -- 批量写时间间隔，默认：10000毫秒
           -- insert时的选项，覆盖或者忽略。
           -- 声明了主键时，设置all-replace为true，全部更新覆盖，
           -- 或者是忽略，即来的新数据不插入？
           'sink.all-replace' = 'true', -- 解释如下(其他rdb数据库类似)：默认：false。定义了PRIMARY KEY才有效，否则是追加语句
                                       -- sink.all-replace = 'true' 生成如：INSERT INTO `result3`(`mid`, `mbb`, `sid`, `sbb`) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE `mid`=VALUES(`mid`), `mbb`=VALUES(`mbb`), `sid`=VALUES(`sid`), `sbb`=VALUES(`sbb`) 。会将所有的数据都替换。
                                       -- sink.all-replace = 'false' 生成如：INSERT INTO `result3`(`mid`, `mbb`, `sid`, `sbb`) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE `mid`=IFNULL(VALUES(`mid`),`mid`), `mbb`=IFNULL(VALUES(`mbb`),`mbb`), `sid`=IFNULL(VALUES(`sid`),`sid`), `sbb`=IFNULL(VALUES(`sbb`),`sbb`) 。如果新值为null，数据库中的旧值不为null，则不会覆盖。
           -- 新增写入选项：默认会判断,当声明了key则是update
           'sink.parallelism' = '1'    -- 写入结果的并行度，默认：null
      );
insert into sink select id,username as name,age as age  from source;

