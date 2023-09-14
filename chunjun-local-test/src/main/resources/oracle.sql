CREATE TABLE source
(   `T1` DATE,
 	`T2` TIMESTAMP (6),
 	`T3` TIMESTAMP (9),
 	`T4` TIMESTAMP (6)
) WITH (
      'schema' = 'TEST',
      'scan.fetch-size' = '1024',
      'password' = 'oracle',
      'connector' = 'oracle-x',
      'scan.query-timeout' = '600',
      'table-name' = 'ALL_DATA_TYPES_DATE',
      'url' = 'jdbc:oracle:thin:@p70197v.hulk.bjzdt.qihoo.net:1521:ORCL',
      'username' = 'system'
      );
CREATE TABLE sink
(
  `T1` DATE,
 	`T2` TIMESTAMP (6),
 	`T3` TIMESTAMP (9),
 	`T4` TIMESTAMP (6)
) WITH (
       'connector' = 'print'
      );
insert into sink select * from source;
