## 事务的四大特性ACID
- 原子性（Atomic）: 事务是最小执行单元，事务内的所有操作要么一次性全部成功，要么全部失败
- 一致性（Consistency）：执行事务前后，数据保持一致，多个事务对同一个数据的读取结果是相同的
- 隔离性（Isolation）: 不同的事务之间互不影响
- 持久性（Durability）：事务提交后，对数据库中数据的改变是持久的

## 并发事务带来的问题
- 脏读（dirty read）: 一个事务读取了另一个事务还未提交的数据，发送脏读
- 丢失修改（Lost to modify）: 两个事务同时读取一个数据，然后两个事务同时修改这个数据，那么其中一个事务的修改结果将丢失。
- 不可重复读（Unrepeatable read）: 一个事务A多次读取一个数据，多次读取操作中间，这个数据被其他的事务B修改了，那么事务A多次读取的数据不一样，这就是不可重复读
- 幻读（Phantom read）: 事务A读取了几行数据，随后事务B插入了一些数据，再然后事务A又使用同样的条件读取多行数据，结果发现读取结果中多了一些原本不存在的数据。

## 事务的隔离级别有哪些？
- Read uncommitted: 最低的隔离级别
- Read committed: 
- Repeatable read: 对同一字段的多次读取结果都是一致的，除非数据是被本事务自己所修改.
- Serializable: 最高隔离级别，完全辐辏ACID


隔离级别 | 脏读 | 不可重复读 | 幻影读
---------|----------|---------|--------
 Read uncommitted | √ | √ | √
 Read committed | × | √ | √
 Repeatable read | × | × | √
 Serializable | × | × | ×

 MySQL InnoDB存储引擎的默认隔离级别是Repeatable read。MySQL与SQL标准不同的地方在于InnoDB存储引擎在Repeatable read事务隔离级别下使用的是Next-Key Lock锁算法，因此可以避免幻读的产生。

 ## 锁机制与InnoDB锁算法
 InnoDB存储引擎的锁算法有三种：
 - Record lock: 单行上锁
 - Gap lock: 间隙锁，锁定一个范围，不包括记录本身
 - Next-key lock: record + gap 锁定一个范围，包含记录本身

 
 ## 使用脚本初始化数据库
 若有两个sql文件，分别是schema-mysql.sql和data-mysql.sql，那么可以通过执行如下脚本来初始化数据库

 set_db.sh
 ```sh
#!/bin/bash
#set input like :  set_db.sh root rootpassword user userpassword database
#"please input like :  set_db.sh root rootpassword user userpassword database
username="'$3'"
userpassword="'$4'"
database="$5"
drop_user="drop user ${username}@'localhost' ;"
create_user="CREATE USER ${username}@'localhost' IDENTIFIED BY ${userpassword};"
create_user2="CREATE USER ${username}@'%' IDENTIFIED BY ${userpassword};"
drop_database="DROP DATABASE IF EXISTS ${database};"
create_database="CREATE DATABASE ${database} DEFAULT CHARSET=utf8 COLLATE utf8_general_ci;"
grant="GRANT ALL ON *.* TO "${username}"@'localhost' IDENTIFIED BY "${userpassword}" WITH GRANT OPTION;"
grant2="GRANT ALL ON *.* TO "${username}"@'%' IDENTIFIED BY "${userpassword}" WITH GRANT OPTION;"
flush="FLUSH PRIVILEGES;"
set_log_bin_trust_function_creators="SET GLOBAL log_bin_trust_function_creators=1;"
use_database="USE ${database}"
mysql -u$1 -p$2 -e "${grant}${grant2}${drop_database}${create_database}${flush}${set_log_bin_trust_function_creators}${use_database}"
echo "successfully create user and database"
mysql -u$3 -p$4 alter database $database character set utf8
mysql -u$3 -p$4 --default-character-set=utf8 $database< schema-mysql.sql
echo "successfully create table"
mysql -u$3 -p$4 --default-character-set=utf8 $database< data-mysql.sql
echo "successfully create data"

 ```

具体使用方式如下：

1. 在控制台输入命令：`set_db.sh root rootpassword user userpassword database`
    
    set_db.sh为要执行的数据库脚本文件，root为mysql的root用户，rootpassword为mysql的root用户密码，user为要创建的用户名，userpassword为创建的用户的密码，database为需要创建的数据库名称

    控制台输出如下：
    ```sh
        [root@localhost src]# sh ./set_db.sh root 123456 user 123456 mydatabase
        successfully create user and database
        successfully create table
        successfully create ProcedureAndFunction
        successfully create data
        successfully create data(test)
        successfully execute sql
    ```

2. 如出现以下错误：
   ```sh
        [root@localhost src]# sh ./set_db.sh root 123456 user 123456 mydatabase
        : 没有那个文件或目录e 4: schema-mysql.sql
   ``` 

    是因为在Windows下，每一行的结尾是\n\r，但是在Linux下文件的结尾是\n。因此在Windows环境下编辑过的文件在Linux下打开看的时候每一行的结尾就会多出来一个字符\r。
    解决办法：
    vim filename 
    然后用命令 
    :set ff? #可以看到dos或unix的字样. 如果的确是dos格式的。 
    然后用 
    :set ff=unix #把它强制为unix格式的, 然后存盘退出。 
    再次运行脚本。 

    还需注意.sh脚本文件最好保存成ANSI格式，否则文件头的#符号可能会是乱码，对运行有影响

    如果生成表数据后在客户端里中文显示乱码时：
    修改MySql服务器的字符集为utf8
    1.找到MySql目录下的my.cnf的配置文件（在Window系统下是my.ini文件）
    2.打开my.cnf文件，找到[mysql]和[mysqld]的配置段，在这两个配置段下，加上default-character-set=utf8，character_set_server=utf8,如果配置节点存在，就替换
    3.保存my.cnf文件
    4.Linux下使用service mysql restart命令重启MySql服务
    新建的数据库都会使用到这个字符集做为默认字符集