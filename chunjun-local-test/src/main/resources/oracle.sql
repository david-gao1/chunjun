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
      -- ,'journal-name' = 'binlog.000194'
        ,'timestamp'='1699447812000'
      ,'table' = '360test.dimension_table'
      ,'timestamp-format.standard' = 'SQL'
      );
CREATE TABLE sink
(   `id` int,
 	`username` varchar,
 	`age` int
) WITH (
       'connector' = 'print'
      );
insert into sink select * from source;
