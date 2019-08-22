# AbstractQueuedSynchronizer 队列同步器
队列同步器大家都简称AQS，内部使用int变量表示同步状态（线程共享资源），线程通过获取同步状态来实现加锁和释放锁的语义。同步器内部使用FIFO队列来管理线程，如线程排队、等待、唤醒等操作。

作者Doug Lea期望它能够成为实现大部分同步需求的基础。所以AQS被实现为一种模板，子类通过继承并实现其同步方法来实现自定义的同步状态管理器。我们熟知的ReentrantLock、ReentrantReadWritLock和CountDownLatch等都使用AQS来管理同步状态。

## AQS接口
AQS提供了三个访问/修改同步状态的方法：
```java
private volatile int state;//共享变量，使用volatile修饰保证线程可见性

//返回同步状态的当前值
protected final int getState() {  
        return state;
}
 // 设置同步状态的值
protected final void setState(int newState) { 
        state = newState;
}
//原子地（CAS操作）将同步状态值设置为给定值update如果当前同步状态的值等于expect（期望值）
protected final boolean compareAndSetState(int expect, int update) {
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
}
```

同步器可重写的方法如下：

方法 | 描述 |
---------|----------
 protected boolean tryAcquire(int arg) | 独占式获取同步状态，实现该方法的步骤：1.查询当前状态;2.判断同步状态是否符合预期;3.CAS设置同步状态
 protected boolean tryRelease(int arg) | 独占式释放同步状态，等待获取同步状态的线程将有机会获取同步状态
 protected int tryAcquireShared(int arg) | 共享式获取同步状态，返回>=0的值，表示获取成功，反之获取失败
 protected boolean tryReleaseShared(int arg) | 共享式释放同步状态
 protected boolean isHeldExclusively() | 当前同步器是否在独占模式下被线程占用，一般该方法表示是否被当前线程占用

