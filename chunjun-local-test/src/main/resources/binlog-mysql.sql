CREATE TABLE tjy_test1_ss
(
  `id` int,
  `name` string,
  age string
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
      'url' = 'jdbc:mysql://10.202.254.219:3306/middle_test?useunicode=true&characterEncoding=utf8&useSSL=false&useCursorFetch=true',
      'username' = 'middle_test'
      );
CREATE TABLE zzzzz_star01
(
  `qq` int,
  `ww` string
) WITH (
      'connector' = 'print'
      );

CREATE TABLE tjy_fortest1_sink
(
  `id` int,
  `name` string
) WITH (
      'connector' = 'print'
      );
--insert into tjy_fortest1_sink select  `id` as `id`,`name` as `name`  from tjy_test1_ss  ;
-- insert into test0608 select  CAST(`id` AS string) as `id`,`name` as `name`,`face` as `face`  from tjy_test1_ss  ;
insert into zzzzz_star01 select  t1.`id` as `qq` ,t2.`name` as `ww`   from tjy_test1_ss  t1  join   tjy_fortest1 t2  on t1.name=t2.name;


