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

 