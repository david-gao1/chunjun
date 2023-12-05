CREATE TABLE source
(     `id` int,
      `name` string,
      `face` string
) WITH (
      'password' = '123456',
      'timestamp-format.standard' = 'SQL',
      'connector' = 'binlog-x',
      'port' = '3306',
      'cat' = 'insert,update,delete',
      'host' = '10.202.254.219',
      'connection-charset' = 'utf-8',
      'url' = 'jdbc:mysql://10.202.254.219:3306/northwind_demo?useunicode=true&characterEncoding=utf8&useSSL=false&useCursorFetch=true',
      'table' = 'tjy_test1_ss',
--      'timestamp' = '0',
      --'journal-name' = 'mysql-bin.000227',
      'username' = 'root'
      );
CREATE TABLE sink
(     `id` int,
      `name` string,
      `face` string
) WITH (
       'connector' = 'print'
      );
insert into sink select * from source;
