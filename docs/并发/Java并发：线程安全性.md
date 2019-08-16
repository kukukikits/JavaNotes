共享（shared）:意味着变量可以由多个线程同时访问
可变（mutable）:意味着变量的值在其生命周期内可以发生变化
线程安全性：当多个线程访问某个类时，这个类始终能表现出正确的行为，那么就称这个类是线程安全的
竞态条件（Race Condition）
数据竞争（Data Race）
复合操作：包含了一组必须以原子方式执行的操作以确保线程安全性
<h3>1.无状态的Servlet:</h3>

```java
@ThreadSafe
public class StatelessFactorizer implements Servlet() {
    public void service(ServeletRequest req, ServletResponse res){
        BigInteger i = extractFromRequest(req);
        BigInteger[] factors = factor(i);
        encodeIntoResponse(res, factors);
    }
}
```

无状态对象一定是线程安全的。

<h3>2.原子性</h3>

```java
@ThreadSafe
public class CountingFactorizer implements Servlet() {
    private final AtomicLong count = new Atomiclong(0);  //共享的  使用线程安全对象来保证线程安全
    public long getCount() {  return count.get();  }
    public void service(ServeletRequest req, ServletResponse res){
        BigInteger i = extractFromRequest(req);
        BigInteger[] factors = factor(i);
        count.incrementAndGet();
        encodeIntoResponse(res, factors);
    }
}
```
为确保线程安全性，“先检查后执行” 和 “读取-修改-写入”等操作必须是原子的。即一组语句作为一个不可分割的单元被执行。
要保持状态的一致性，必须在单个原子操作中更新所有相关的状态变量。

<h3>3.加锁</h3>
<h4><span style="color: #ff0000;">内置锁（Intrinsic Lock）或监视锁（monitor Lock）</span></h4>
使用同步代码块（Synchronized Block）保证原子性：

```java
synchronized (lock){
//do something 操作共享的状态
}
```
被同步代码块锁定的代码，最多只能有一个线程执行，当线程A尝试获取一个由线程B持有的锁时，线程A必须等待或阻塞，直到线程B释放这个锁。

<h3>4.活跃性与性能</h3>

可能存在的问题：使用synchronized锁定一个service方法时，会造成服务响应性低的性能缺陷

解决方法：通过缩小同步代码块的作用范围，既保证并发性，同时又维护线程安全性。要确保同步代码块不要太小，也不要将本是原子的操作拆分到多个同步代码块中。同步代码块的大小应在安全性、简单性和性能之间权衡。当执行计算密集型操作时，不要持有锁。<span style="color: #ff0000;">所有访问共享可变量的位置上都应该使用同步。</span>