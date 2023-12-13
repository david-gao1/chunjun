CREATE TABLE tjy_sql1
(
  `id` int,
  `name` string,
  `face` string
) WITH (
      'password' = '123456',
      'lookup.async-timeout' = '10000',
      'connector' = 'mysql-x',
      'lookup.cache-type' = 'LRU',
      'lookup.fetch-size' = '1000',
      'lookup.cache.ttl' = '3600000',
      'lookup.cache.max-rows' = '1000',
      'table-name' = 'tjy_fortest1',
      'lookup.cache-period' = '3600000',
      'url' = 'jdbc:mysql://10.202.254.219:3306/middle_test?useunicode=true&characterEncoding=utf8&useSSL=false&useCursorFetch=true',
      'username' = 'middle_test'
      );



 CREATE TABLE tjy_sql1_sink
 (
   `name` string
 ) WITH (
       'connector' = 'print'
       );


insert into tjy_sql1_sink select name from tjy_sql1;
