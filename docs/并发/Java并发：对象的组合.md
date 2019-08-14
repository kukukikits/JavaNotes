这一章的目的是介绍一些组合模式，使用这些模式能很容易地构建线程安全类。

通过封装技术，不需要对整个程序进行分析就可以判断一个类是否是线程安全的。
<h3>1. 设计线程安全的类</h3>
在设计线程安全类的过程中，需要包含以下三个基本条件：
<ul>
 	<li>找出构成对象状态的所有变量</li>
 	<li>找出约束状态变量的不变性条件</li>
 	<li>建立对象状态的并发访问管理策略</li>
</ul>
分析对象的状态首先从对象的域开始。如果对象中所有的域都是基本类型的变量（如int），那么这些域将构成对象的全部状态。如果在对象的域中引用了其他对象，那么该对象的状态将包含被引用对象的域。例如，LinkedList的状态就包括该链表中所有节点对象的状态。
<h4>1.1Java监视器模式</h4>
使用封闭机制确保线程安全。遵循Java监视器模式的对象需要把对象的所有可变状态都封装起来，并由对象自己的内置锁来保护。

当从头开始构建一个类，或者组合多个非线程安全的类为一个类时，Java监视器模式是非常有用的。

<span style="color: #ff0000;">注：将一个本该被封闭的对象发布出去，会破坏封闭性</span>

使用对象的内置锁：
<pre class="prettyprint">@ThreadSafe
public class PersonSet {
    //将mySet这个非线程安全的类封闭在PersonSet类的实例中，所有与该状态有关的操作使用synchronized加锁
    @GuardedBy("this") private final Set&lt;Person&gt; mySet = new HashSet&lt;Person&gt;();

    public synchronized void addPerson(Person p) {
        mySet.add(p);
    }

    public synchronized boolean containsPerson(Person p) {
        return mySet.contains(p);
    }

    interface Person {
    }
}</pre>
使用私有的锁对象：
<pre class="prettyprint">public class PrivateLock {
    private final Object myLock = new Object();
    @GuardedBy("myLock") Widget widget;

    void someMethod() {
        synchronized (myLock) {
            // Access or modify the state of widget
        }
    }
}</pre>
使用私有的锁对象的优点：可以将锁封装起来，使客户代码无法得到锁，但客户代码可以通过公有方法来访问锁。
<h4>1.2 线程安全性的委托</h4>
当从头开始构建一个类，或者组合多个非线程安全的类为一个类时，Java监视器模式是非常有用的。但是如果类中的各个组件已经是线程安全的，我们是否还需要增加额外的同步机制？答案是需要视情况而定。
<h5>1.2.1 示例：基于委托的不需要额外同步机制的实现</h5>
在下面的类中线程安全性委托给了一个线程安全的类ConcurrentHashMap。由于下面这个类的不变性条件只需要一个locations就能满足，而且DelegatingVehicleTracker是一个不可变类，所以是线程安全的。如果Point类不是一个不可变类，那么DelegatingVehicleTracker的封装性就被破坏了，因为getLocations这个方法会发布一个可变的Point实例。
<pre class="prettyprint">@ThreadSafe
public class DelegatingVehicleTracker {
    private <strong>final</strong> ConcurrentMap&lt;String, Point&gt; locations;
    private <strong>final</strong> Map&lt;String, Point&gt; unmodifiableMap;

    public DelegatingVehicleTracker(Map&lt;String, Point&gt; points) {
        //将线程安全委托给线程安全类ConcurrentHashMap
        locations = new<strong> ConcurrentHashMap</strong>&lt;String, Point&gt;(points);
        unmodifiableMap = <strong>Collections.unmodifiableMap</strong>(locations);
    }
    // return real-time locations 返回实时地理位置
    public Map&lt;String, Point&gt; getLocations() {
        return unmodifiableMap;
    }

    public Point getLocation(String id) {
        return locations.get(id);
    }

    public void setLocation(String id, int x, int y) {
        if (locations.replace(id, new Point(x, y)) == null)
            throw new IllegalArgumentException("invalid vehicle name: " + id);
    }

    // return a snapshot 返回快照
    public Map&lt;String, Point&gt; getLocationsAsStatic() {
        return Collections.unmodifiableMap(
                new HashMap&lt;String, Point&gt;(locations));
    }
}

@Immutable
public class Point(){
    public final int x, y;
    public Point(int x, int y){
        this.x = x;
        this.y = y;
    }
}</pre>

<h5>1.2.2 示例：委托失效</h5>

set方法都是“先检查后执行”，不是原子性操作

<pre class="prettyprint">@ThreadUnSafe
public class NumberRange {
    // INVARIANT: lower &lt;= upper
    private final AtomicInteger lower = new AtomicInteger(0);
    private final AtomicInteger upper = new AtomicInteger(0);

    public void setLower(int i) {
        // Warning -- unsafe check-then-act
        if (i &gt; upper.get())
            throw new IllegalArgumentException("can't set lower to " + i + " &gt; upper");
        lower.set(i);
    }

    public void setUpper(int i) {
        // Warning -- unsafe check-then-act
        if (i &lt; lower.get())
            throw new IllegalArgumentException("can't set upper to " + i + " &lt; lower");
        upper.set(i);
    }

    public boolean isInRange(int i) {
        return (i &gt;= lower.get() &amp;&amp; i &lt;= upper.get());
    }
}</pre>

<h5>1.2.3 示例：发布底层可变状态变量，且是线程安全的</h5>
在下面的例子中SafePoint是一个可变类，不同于1.2.1当中的不可变Point。

<pre class="prettyprint">@ThreadSafe
public class SafePoint {
    @GuardedBy("this") private int x, y;

    private SafePoint(int[] a) {
        this(a[0], a[1]);
    }

    public SafePoint(SafePoint p) {
        this(p.get());
    }

    public SafePoint(int x, int y) {
        this.set(x, y);
    }

    public synchronized int[] get() {
        return new int[]{x, y};
    }

    public synchronized void set(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
@ThreadSafe
public class PublishingVehicleTracker {
    private final Map&lt;String, SafePoint&gt; locations;
    private final Map&lt;String, SafePoint&gt; unmodifiableMap;

    public PublishingVehicleTracker(Map&lt;String, SafePoint&gt; locations) {
        this.locations = new ConcurrentHashMap&lt;String, SafePoint&gt;(locations);
        this.unmodifiableMap = Collections.unmodifiableMap(this.locations);
    }
    //客户程序通过geLocations拿到不可修改的Map，因此调用者不能通过这个引用增加或删除车辆
    //但是调用者却可以通过修改Map中的SafePoint的值来改变车辆的位置，并且这种改变是实时的
    //一旦改变了SafePoint的x,y值那么这个改变会实时反映到PublishingVehicleTracker的实例中
    public Map&lt;String, SafePoint&gt; getLocations() {
        return unmodifiableMap;
    }

    //返回底层Map对象的一个不可变的副本，因为locations是不可变的
    public SafePoint getLocation(String id) {
        return locations.get(id);
    }

    public void setLocation(String id, int x, int y) {
        if (!locations.containsKey(id))
            throw new IllegalArgumentException("invalid vehicle name: " + id);
        locations.get(id).set(x, y);
    }
}</pre>