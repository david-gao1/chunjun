CREATE TABLE tjy_test1_ss
(
  `id` int,
  `name` string,
  age string,
  `proc_time` AS `proctime`()
) WITH (
      'password' = '11111111',
      'timestamp-format.standard' = 'SQL',
      'connector' = 'binlog-x',
      'port' = '3306',
      'cat' = 'insert',
      'host' = 'localhost',
      -- 'connection-charset' = 'utf-8',
      'url' = 'jdbc:mysql://localhost:3306/360test',
      'table' = 'test003',
      'username' = 'root',
      'timestamp'='1702881040000'
      );
CREATE TABLE tjy_fortest1
(
  `id` int,
  `name` string,
  `face` string,
  PRIMARY KEY (id) NOT ENFORCED
) WITH (
      'password' = '123456',
      'connector' = 'mysql-x',
      'sink.buffer-flush.interval' = '10000',
      'sink.buffer-flush.max-rows' = '1024',
      'sink.all-replace' = 'true',
      'table-name' = 'tjy_fortest1',
      'sink.parallelism' = '1',
      'url' = 'jdbc:mysql://xxx:3306/middle_test?useunicode=true&characterEncoding=utf8&useSSL=false&useCursorFetch=true',
      'username' = 'middle_test'
      );
CREATE TABLE zzzzz_star01
(
  `qq` int,
  `ww` string
) WITH (
      'connector' = 'print'
      );

insert into zzzzz_star01 select  t1.`id` as `qq` ,t2.`name` as `ww`   from tjy_test1_ss  t1 left  join   tjy_fortest1 FOR SYSTEM_TIME AS OF `t1`.`proc_time` t2  on t1.name=t2.name;


