<h3>对象的共享，目的是构造线程安全的对象，从而支持多个线程安全地同时访问</h3>
<h3>1.可见性</h3>
所谓可见性，我觉得就是某个变量的值发生变化后能否被其他读操作察觉。在多线程环境中每个线程都维持了共享变量的一个副本，假如sharedVariable=1，当线程A对共享变量进行写操作sharedVariable=2改变其值后，B线程再读取sharedVariable的值，这个时候假如sharedVariable不具有可见性那么B读取的值还是1（从自己的维持的副本中读），假如共享变量具备可见性，那么线程A对共享变量的操作就能被线程B发觉，B线程知道他这里的这个共享变量的副本已经失效，所以B线程知道从哪里去获取最新的值，最后B线程就能获取正确的获取到值——2。

由于线程A和线程B在不同步的情况下，B线程可能读取到旧的sharedVariable的值，导致B线程读取了错误的数据，我们把这种错误的数据叫做：失效数据。

使用加锁的方式可以保证可见性，也可以使用volatile申明保证某一个变量的可见性
<pre class="prettyprint">//加锁保证同步，既保证了可见性，也保证了原子性
@ThreadSafe
public class SynchronizedInteger(){
    private int value;
    public synchronized int get(){ return value; }
    public synchronized void set(int value){ this.value = value; }
}

//volatile申明保证可见性，但不能保证原子性，所以是一种弱的同步机制
//线程1
volitle boolean stop = false;
while(!stop){
    doSomething();
}
 
//线程2
stop = true;</pre>
<h3>2.发布与逸出（Publish and Escape）</h3>
发布：使对象能够在当前作用域之外的代码中使用，即将对象共享出去

逸出：当某个不应该被发布的对象被发布时，这种情况称为逸出

下面是两个使对象逸出的两个例子：
<pre class="prettyprint">//内部可变状态逸出<img src="http://47.93.1.79/wordpress/wp-content/uploads/2017/12/失败-1-150x150.png" alt="" width="150" height="150" class="img wp-image-226 size-thumbnail alignright" title="don&#96;t" style="color: #555555; font-family: Roboto, Helvetica, Arial, sans-serif; font-size: 17px; white-space: normal;" />
public class UnsafeStates {
    private String[] states = new String[] { 'A', 'B' };
    public String[] getStates() {
        return states;                  //私有变量逸出
    }
}

//隐式地使this逸出
public class ThisEscape {
    private int count = 0;
    public ThisEscape(EventSource source) {
        //构造ThisEscape时就把this隐式的传给匿名内部类EventListener了，造成this逸出
        source.registerListener(new EventListener() {
            public void onEvent(Event e) {
                doSomething(e);
            }
        });
        // more initialization
        // ...
        count = 100;
    }

    void doSomething(Event e) {
        /**
         * 假如onEvent事件在ThisEscape的实例还没有实例化完成就已经触发调用，那么此时将得到0，而不是100
         * 因为ThisEscape.this在ThisEscape实例化完成前就已经隐式地发布出去了，所以在ThisEscape实例化
         * 完成前就可以获取到count的值，然而这个值并不符合预期
         */
        System.out.print(count)        
    }
    interface EventSource {
        void registerListener(EventListener e);
    }
    interface EventListener {
        void onEvent(Event e);
    }
    interface Event {
    }
}</pre>
以上两个是错误的用法，不要发布不应该发布的对象，不要在构造过程中使this逸出。

下面来看看使用工厂方法防止this引用在构造过程中逸出的写法：
<pre class="prettyprint">public class SafeListener {
    private final EventListener listener;<img src="http://47.93.1.79/wordpress/wp-content/uploads/2017/12/成功-转换.png" alt="" width="128" height="128" class="size-full wp-image-229 alignright" />
    //使用私有构造函数
    private SafeListener() {
        listener = new EventListener() {
            public void onEvent(Event e) {
                doSomething(e);
            }
        };
    }
    public static SafeListener newInstance(EventSource source) {
        SafeListener safe = new SafeListener();  //构造完成后再注册监听事件
        source.registerListener(safe.listener);
        return safe;
    }
}
</pre>
<h3>3.构造不可变对象（Immutable Object）</h3>
不可变对象一定是线程安全的。不可变对象只有一种状态，并且该状态由<span style="color: #ff0000;">构造函数</span>控制。

下面是在可变对象的基础上构建的不可变类
<pre class="prettyprint">@Immutable
 public final class ThreeStooges {
    //可变对象HashSet，不可变对象stooges(构造函数中初始化完成后就不能再改变)
    private final Set&lt;String&gt; stooges = new HashSet&lt;String&gt;();

    public ThreeStooges() {
        stooges.add("Moe");
        stooges.add("Larry");
        stooges.add("Curly");
    }

    public boolean isStooge(String name) {
        return stooges.contains(name);
    }
}</pre>
那么如何正确地构造不可变对象呢？
<ul>
 	<li>对象创建以后其状态就不能修改，也就是说不能为对象提供setter方法；</li>
 	<li>对象的所有域都是private和final类型；</li>
 	<li>对象是正确创建的（在对象创建期间，this引用没有逸出）；</li>
</ul>
满足以上条件的对象才是不可变对象。

<span style="color: #ff0000;">不可变对象可以通过任意机制来发布</span>

这里也有一篇关于构造不可变对象的详细的解释<a href="http://blog.csdn.net/waeceo/article/details/54377218">http://blog.csdn.net/waeceo/article/details/54377218</a>
<h3>3.1示例：使用volatile类型来发布不可变对象</h3>
<pre class="prettyprint">@Immutable  //不可变对象
public class OneValueCache {
    private final BigInteger lastNumber;
    private final BigInteger[] lastFactors;

    public OneValueCache(BigInteger i, BigInteger[] factors) {
        lastNumber = i;
        lastFactors = Arrays.copyOf(factors, factors.length);
    }

    public BigInteger[] getFactors(BigInteger i) {
        if (lastNumber == null || !lastNumber.equals(i))
            return null;
        else
            return Arrays.copyOf(lastFactors, lastFactors.length);
    }
}
@ThreadSafe
public class VolatileCachedFactorizer extends GenericServlet implements Servlet {
    //使用volatile类型的变量发布不可变对象
    private volatile OneValueCache cache = new OneValueCache(null, null);

    public void service(ServletRequest req, ServletResponse resp) {
        BigInteger i = extractFromRequest(req);
        BigInteger[] factors = cache.getFactors(i);
        if (factors == null) {
            factors = factor(i);
            cache = new OneValueCache(i, factors);
        }
        encodeIntoResponse(resp, factors);
    }

    void encodeIntoResponse(ServletResponse resp, BigInteger[] factors) {
    }

    BigInteger extractFromRequest(ServletRequest req) {
        return new BigInteger("7");
    }

    BigInteger[] factor(BigInteger i) {
        // Doesn't really factor
        return new BigInteger[]{i};
    }
}</pre>
<h3>3.安全发布<span style="color: #000000;">对象</span></h3>
对象发布只是用于确保其他线程能够看到该对象处于已经发布的状态。

<span style="color: #ff0000;">不可变对象可以通过任意机制来发布。</span>

<span style="color: #ff0000;">可变对象必须通过安全的方式来发布</span>，也意味着在发布和使用该对象的线程必须使用同步，一个正确构造的对象可以通过以下方式来安全地发布：
<ul>
 	<li>在静态初始化函数中初始化一个对象引用；</li>
 	<li>将对象的引用保存到volatile类型的域或者AtomicReference对象中；</li>
 	<li>将对象的引用保存到某个正确构造对象的final类型域中；</li>
 	<li>将对象的引用保存到一个由锁保护的域中</li>
</ul>
<pre class="prettyprint">//静态初始化
public static Holder holder = new Holder(42);

//对象引用保存到volatile或AtomicReference中
private volatile Set&lt;String&gt; stooges = new HashSet&lt;String&gt;();
private AtomicLong count = new AtomicLong(0);

//将对象的引用保存到final类型域中，这里的前提是这个对象在某个正确构造对象的final类型域中
//假如将arr错误地发布出去，那么
private final int[] arr = new int[]{0,2,3};

//用锁保护
public class Syn(){
  private int count = 0;
  public synchronized void increase(){
     count++;
  }
}</pre>
<h3>4.安全地共享对象</h3>
安全地共享对象就是说保证线程安全，一些实用的策略如下：
<ul>
 	<li>线程封闭。线程封闭的对象只能由一个线程拥有，这样就保证了线程安全；</li>
 	<li>只读共享。在没有额外同步的情况下，共享的只读对象可以由多个线程并发访问，任何线程都不能修改它。共享的只读对象包括不可变对象和事实不可变对象；</li>
 	<li>线程安全共享。在内部实现同步，多个线程可以通过对象的公有接口进行访问，保障线程安全；</li>
 	<li>保护对象。被保护的对象只能通过特定的锁来访问。保护对象包括封装在其他线程安全对象中的对象，以及已经发布的并且由某个特定的锁保护的对象。</li>
</ul>