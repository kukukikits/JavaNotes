## 2.1 What is Thread safe?
- Definition:
A class is thread-safe when it continues to behave correctly when accessed from multiple thread, regardless of scheduling or interleaving of the execution of those threads by the runtime environment, and with no additional synchronization or other coordination on the part of the calling code.

- Stateless objects are always thread safe.

## 2.2 Atomicity
### count++
It's a shorthand for a sequence of three discrete operations:
1. fetch the current value;
2. add one to it;
3. write the new value back.

count++ is an example of a read-modify-write operations.

### race conditions
A race condition occurs when the correctness of a computation depends on the relative timing or interleaving of multiple threads by the runtime. In other words, when getting right answer relies on lucky timing.[4] 

Using a potentially stale observation to make a decision or perform a computation. This type of race condition is called **check-then-act**: you observe something to be true(file X doesn't exist) and then take action based on that observation (create X). But in fact the observation could have become invalid between the time you observe it and the time you acted on it (someone else created X in the meantime), causing a problem (unexpected exception, overwritten data, file corruption).

> [4] The term race condition is often confused with the related term data race, which arises when synchronization is not used to coordinate all  access to a shared non‐final field. You risk a data race whenever a thread writes a variable that might next be read by another thread or reads a  variable that might have last been written by another thread if both threads do not use synchronization; code with data races has no useful  defined semantics under the Java Memory Model. Not all race conditions are data races, and not all data races are race conditions, but they both  can cause concurrent programs to fail in unpredictable ways. UnsafeCountingFactorizer has both race conditions and data races. See Chapter  16 for more on data races. 

> :warning:
> The most common type of race condition is check-then-action, where a potentially stale observation is used to make a decision on what to do next.

**Read-modify-write** operations, like incrementing a counter, define a transformation of an object's state in terms of previous state

### compound actions
To avoid race conditions, there must be a way to prevent other threads from using a variable while we're in the middle of modifying it, so we can ensure that other threads can observe or modify the state only before we start or after we finish, but not in the middle.

## 2.3 Locking
:warning:
To preserve state consistency, update related state variables in a single atomic operation.

## 2.4 Guarding State with Locks
:warning:
For each mutable state variable that may be accessed by more than one thread, **all accesses** to that variable must be performed with the same lock held. In this case, we can say that the variable is guarded by that lock.

:warning:
Every shared, mutable variable should be guarded by exactly one lock. Make it clear to maintainers which lock that is.

## 2.5 Liveness and Performance
:warning:There is frequently a tension between simplicity and performance. When implementing a synchronization policy, resist the temptation to prematurely sacrifice simplicity (potentially compromising safety) for the sake of performance.

## 3.1 Visibility
### 3.1.2 Non-atomic 64-bit Operations
When a thread reads a variable without synchronization, it may see a stale value, but at least it sees a value that was actually placed there by some thread rather than some random value. This safety guarantee is called **out-of-thin-air safety**.

Out-of-thin-air safety applies to all variables, with on exception: 64-bit numeric variables(double and long) that are not declared volatile. The Java Memory Model requires fetch and store operations to be at atomic, but for nonvolatile long and double variables, the JVM is permitted to treat a 64-bit read or write as two separate 32-bit operations. If the reads and writes occur in different threads, it is therefore possible to read a nonvolatile long and get back the high 32 bits of one value and the low 32 bits of of another. Thus, even if you don't care about stale values, it is not safe to use shared mutable long and double variables in multi-threaded programs unless they are declared volatile or guarded by a lock. 

### 3.1.3 Locking and Visibility
:zap: Locking is not just about mutual exclusion; it is also about memory visibility. To ensure that all threads see the most up-to-date values of shared mutable variables, the reading and writing threads must synchronize on a common lock.

### 3.1.4 Volatile Variables
#### Two effects of using volatile
1. Ensure visibility: ensure that updates to a variable are propagated predictably to other threads.
2. Same memory semantic as synchronized block.(Not recommend):
     When thread A writes to a volatile variable and subsequently thread B reads that same variable, that values of all variables that were visible to A prior to writing to the volatile variable become visible to B after reading the volatile variable. So from a memory visibility perspective, writhing a volatile variable is like exiting a synchronized block and reading a volatile variable is like entering a synchronized block. 

     :warning: However, we do not recommend relying too heavily on volatile variables for visibility; code that relies on volatile variables for visibility of arbitrary state is more fragile and harder to understand than code that uses locking.

#####:key:Debugging tip
> For server applications, be sure to always specify the -server JVM command line switch when invoking the JVM, even for  development and testing. The server JVM performs more optimization than the client JVM, such as hoisting variables out of a loop that are not  modified in the loop; code that might appear to work in the development environment (client JVM) can break in the deployment environment  (server JVM). For example, had we "forgotten" to declare the variable asleep as volatile in Listing 3.4, the server JVM could hoist the test out  of the loop (turning it into an infinite loop), but the client JVM would not. An infinite loop that shows up in development is far less costly than one  that only shows up in production.  

####:key: Locking can guarantee both visibility and atomicity; volatile variables can only guarantee visibility.

## 3.3 Thread Confinement线程封闭
Thread confinement is an element of your program's 
design that must be enforced by its implementation.
Java language and core libraries provide mechanisms
that help in maintaining thread confinement —— local
variables and the ThreadLocal class —— but even with
these, it is still the programmer's responsibility
to ensure that thread-confined objects do not escape from their intended thread.
### 3.3.1 Ad-hoc Thread Confinement
Ad Hoc源自于拉丁语，意思是“for this”引申为“for this purpose only”，即“为某种目的设置的，特别的”意思，即Ad hoc网络是一种有特殊用途的网络。

Ad-hoc thread confinement describes when the responsibility for maintaining thread confinement falls entirely on the implementation.(也就是变量的线程封闭性的维护完全由代码实现来保障)

A special case of thread confinement applies to volatile variables. It is safe to perform read-modify-write operations on shared volatile variables as long as you ensure that the volatile variable is only written from a single thread. In this case, you are confining the modification to a single thread to prevent race conditions, and the visibility guarantees for volatile variables ensure that other threads see the most up-to-date value.

### 3.3.2 Stack Confinement栈封闭
也就是使用本地变量来实现对变量访问权限的封闭，但是要防止本地变量溢出（被公开）

### 3.3.3 ThreadLocal
ThreadLocal allows you to associate a per-thread value with a value-holding object.

**Using ThreadLocal to ensure thread confinement**
```java
private static ThreadLocal<Connection> connectionHolder = new ThreadLocal<>() {
     public Connection initialValue() {
          return DriverManager.getConnection(DB_URL);
     }
};

public static Connection getConnection() {
     return connectionHolder.get();
}
```

## 3.4 Immutability

:warning: An object is immutable if:
- Its state cannot be modified after construction;
- All its fields are final; and
- It is properly constructed (the this reference does not escape during construction)

### Example: Using Volatile to Publish Immutable Objects
:warning:
Whenever a group of related data items must be acted on atomically, consider creating an immutable **holder class** for them, such as OneValueCache below.

```java
@Immutable
public class OneValueCache {
     private final BigInteger lastNumber;
     private final BigInteger[] lastFactors;

     public OneValueCache(BigInteger i, BigInteger[] factors) {
          lastNumber = i;
          lastFactors = Arrays.copyOf(factors, factors.length);
     }

     public BigInteger[] getFactors(BigInteger i) {
          if (lastNumber == null || !lastNumber.equals(i)) {
               return null;
          }
          return Arrays.copyOf(lastFactors, lastFactors.length);
     }
}
```

Racing conditions in accessing or updating multiple related variables(eg. lastFactors) can be eliminated by using an immutable object(OneValueCache) to hold all the variables. With a mutable holder object, you would have to use locking to ensure atomicity; with an immutable one, once a thread acquires a reference to it, it need never worries about another thread modifying its state. If the variables are to be update, a new holder object(an OneValueCache instance) is created, but any threads working with the previous holder still see it in a consistent state. 

The cache-related operations cannot interfere with each other because OneValueCache is immutable and the cache field is accessed only once in each of the relevant code paths. This combination of an immutable holder object(**OneValueCache**) for multiple state variables(**lastFactors**) related by an invariant(**lastNumber**), and a volatile reference used to ensure its timely visibility, allows VolatileCachedFactorizer to be thread-safe even though it does no explicit locking.

```java 
@ThreadSafe
public class VolatileCachedFactorizer implements Servlet{
     private volatile OneValueCache cache = new OneValueCache(null, null);

     public void service(ServletRequest req, ServletResponse res) {
          BigInteger i = extractFrom(req);
          BigInteger[] factors = cache.getFactors(i);
          if(factors == null) {
               factors = factor(i);
               cache = new OneValueCache(i, factors);
          }
          encodeIntoResponse(res, factors);
     }
}

```

## 3.5 Safe Publication

### 3.5.3 Safe Publication Idioms
Objects that are not immutable must be safely published, which usually entails synchronization by both the publishing and the consuming thread. For the moment, let's focus on ensuring that the consuming thread can see the object in its as published state; we'll deal with visibility of modifications made after publication soon.

:warning: To publish an object safely, both the reference to the object and the object's state must be made visible to other threads at the same time. A properly constructed object can be safely published by:
- Initializing an object reference from a static initializer;
- Storing a reference to it into a volatile field or AtomicReference;
- Storing a reference to it into a final field of a properly constructed object; or
- Storing a reference to it into a field that is properly guarded by a lock.

### 3.5.5 Mutable objects
:warning: The publication requirements of an object depend on its mutability:
- Immutable objects can be published through any mechanism;
- Effectively immutable objects must be safely published;
- Mutable objects mush be safely published, and must be either thread-safe or guarded by a lock.

### 3.5.6 Sharing Objects Safely
:warning:The most useful policies for using and sharing objects in a concurrent program are:  
- Thread‐confined. A thread‐confined object is owned exclusively by and confined to one thread, and can be modified by  its owning thread.  
- Shared read‐only. A shared read‐only object can be accessed concurrently by multiple threads without additional  synchronization, but cannot be modified by any thread. Shared read‐only objects include immutable and effectively  immutable objects.  
- Shared thread‐safe. A thread‐safe object performs synchronization internally, so multiple threads can freely access it  through its public interface without further synchronization.  
- Guarded. A guarded object can be accessed only with a specific lock held. Guarded objects include those that are  encapsulated within other thread‐safe objects and published objects that are known to be guarded by a specific lock.  


# Chapter 4 Composing Objects
## 4.1 Designing a Thread-safe Class
:warning:The design process for a thread-safe class should include these three basic elements:
- Identify the variables that form the object's state;
- Identify the invariants that constrain the state variables;
- Establish a policy for managing concurrent access to the object's state.
### 4.1.1 Gathering Synchronization Requirements
### State-dependent Operations
### State Ownership

## 4.2 Instance Confinement
:warning:
- Encapsulating data within an object confines access to the data to the object's methods, making it easier to ensure that the data is always accessed with the appropriate lock held.
- Confinement makes it easier to build thread-safe classed because a class that confines its state can be analyzed for thread safety without having to examine the whole program.

### 4.2.1 The Java Monitor Pattern
- Java Monitor Pattern: encapsulating all mutable state and guarding it with the object's own intrinsic lock.

```java
public class PrivateLock{
     private final Object myLock = new Object();
     @GuardedBy("myLock")
     Widget widget;

     void someMethod() {
          synchronized(myLock) {
               //Access or modify the state of widget
          }
     }
}
```
Making the lock object private encapsulates the lock so that client code cannot acquire it, whereas a publicly accessible lock allows client code participate in its synchronization policy - correctly or incorrectly.

## 4.3 Delegating Thread Safety

###:cry: Number Range Class that does Not Sufficiently Protect Its Invariants. Don't Do this

```java
public class NumberRange{
     //INVARIANT: lower <= upper
     private final AtomicInteger lower = new AtomicInteger(0);
     private final AtomicInteger upper = new AtomicInteger(0);

     public void setLower(int i) {
          //warning -- unsafe check-then-act
          if(i > upper.get()){
               throw new IllegalArgumentException("can't set lower to " +i+" > upper");
          }
          lower.set(i);
     }

     public void setUpper(int i) {
          //warning -- unsafe check-then-act
          if(i < lower.get()){
               throw new IllegalArgumentException("can't set upper to " +i+" < lower");
          }
          upper.set(i);
     }

     public boolean isInRange(int i) {
          return (i >= lower.get() && i<=upper.get());
     }
}
```

:warning:
If a class is composed of multiple independent thread-safe state variables and has no operations that have any invalid state transitions, then it can delegate thread safety to the underlying state variables.

### 4.3.4 Publishing Underlying State Variables
:warning: If a state variable is thread-safe, does not participate in any invariants that constrain its value, and has no prohibited state transitions for any of its operations, then it can safely be published.

# 5 Chapter 5 Building Blocks
## 5.1 Synchronized Collections

### Problems with Synchronized Collections
Compound actions need client-side locking to guarantee
safety. 
:warning: The synchronized collection classes guard each method with the lock on the synchronized collection object itself. So, by acquiring the collection lock we can make a compound action atomic, like below:
```java
public static Object getLast(Vector list) {
     synchronized(list) {
          int lastIndex = list.size() - 1;
          return list.get(lastIndex);
     }
}

public static void deleteLast(Vector list) {
     synchronized(list) {
          int lastIndex = list.size() - 1;
          list.remove(lastIndex);
     }
}
```

### 5.1.2 Iterators and Concurrent modification exception
:warning: The iterators returned by the synchronized collections are not designed to deal with concurrent modification, and they are fail-fast - meaning that if they detect that the collection has changed since iteration began, they throw the unchecked *ConcurrentModificationException*.

:warning: *ConcurrentModificationException* can arise in single-threaded code as well; this happens when objects are removed from the collection directly rather than through iterator.remove.

An alternative to locking the collection during iteration is to clone the collection and iterate the copy instead. Since the clone is thread-confined, no other thread can modify it during iteration, eliminating the possibility of ConcurrentModificationException. (The collection still must be locked during the clone operation itself.) Cloning the collection has an obvious performance cost.

### 5.1.3 Hidden Iterators
:cry::b::a:d: Iteration Hidden within String Concatenation. *Don't Do this*
```java
public class HiddenIterator {
     @GuardedBy("this")
     private final Set<Integer> set = new HashSet<Integer>();

     public synchronized void add(Integer i) {
          set.add(i);
     }

     public synchronized void remove(Integer i) {
          set.remove(i);
     }

     public void addTenThings() {
          Random r = new Random();
          for (int i = 0; i < 10; i++) {
               add(r.nextInt());
          }
           //implicitly call set collection's 
           //toString method. Could throw 
           //ConcurrentModificationException here.
          System.out.println("Debug: added then elements to " + set)
     }
}
```

## 5.2 Concurrent Collections
###🤓:
> Replacing synchronized collection with concurrent collections can offer dramatic scalability improvements with little risk.

### 5.2.1 ConcurrentHashMap
The one feature offered by the synchronized map implementations but not by ConcurrentHashMap is the ability to lock the map for exclusive access. With HashTable and synchronizedMap, acquiring the Map lock prevents any other thread from accessing it. On the whole, though, this is a reasonable tradeoff: **concurrent collections should be expected to change their contents continuously**. 

### 5.2.3 CopyOnWriteArrayList
CopyOnWriteArrayList implement mutability by creating and republishing a new copy of the collection every time it is modified. The iterators returned by the copy-on-write collections do not throw *ConcurrentModificationException* and return the elements exactly as they were at the time the iterator was created, regardless of subsequent modifications.

The copy-on-write collections are reasonable to use only when iteration is far more common than modification. This collection is a very good candidate for event-notification systems as in most cases registering or unregistering an event listener is far less common than receiving an event notification.

## 5.3 Blocking Queues and the Producer-consumer Pattern
:warning: Blocking queues provide an *offer* method, which returns a failure status if the item cannot be enqueued. This enables you to create more flexible polices for dealing with overload, such as **shedding load, serializing excess work items and writing them to disk, reducing the number of producer threads, or throttling producers in some other manner.**

:warning: Bounded queues are a powerful resource management tool for building reliable applications: they make your program more robust to overload by throttling activities that threaten to produce more work than can be handled.

### 5.3.3 Deques and Work Stealing
Just as blocking queues lend themselves to the producer-consumer pattern, deques lend themselves to a related pattern called work stealing. In a work stealing design, every consumer has its own deque. If a consumer exhausts the work in its own deque, it cam steal work from the tail of someone else's deque.

Work stealing is well suited to problems in which consumers are also producers - when performing a unit of work is likely to result in the identification of more work. For example, processing a page in a web crawler usually results in the identification of new pages to be crawled.

## 5.4 Blocking an Interruptible Methods
Threads may block, ore pause for several reasons:
> waiting for I/O completion
> waiting to acquire a lock
> waiting to wake up form *Thread.sleep*
> waiting for the result of a computation in another thread

When a thread blocks, it is usually suspended and placed in one of the blocked thread states: **BLOCKED, WAITING, or TIMED_WAITING**

:warning: The *put* and *take* methods of *BlockingQueue* throw the checked *InterruptedException*, as do a number of other library methods such *Thread.sleep*.
**When a method can throw *InterruptedException*, it is telling you that it is a blocking method, and further that if it is interrupted, it will make an effort to stop blocking early.**

Interruption is a cooperative mechanism. The most sensible use for interruption is to cancel an activity. **Blocking methods that are responsive to interruption make it easier to cancel long-running activities on a timely basis**.

When your code calls a method that throws *InterruptedException*, then your method is blocking method too, and must have a plan for responding to interruption. For library code, there are basically two choices:

1. Propagate the *InterruptedException*. This could involve not catching *InterruptedException*, or catching it and throwing it again after performing some brief activity-specific cleanup.
2. Restore the interrupt. Sometimes you cannot throw *InterruptedException*, for instance when your code is part of a *Runnable*. In these situations, you must catch *InterruptedException* and restore the interrupted status by calling *interrupt* on the current thread, so that code higher up the call stack can see that interrupt was issued, as demonstrated below:

**Restoring the Interrupted Status so as Not to Swallow the Interrupt**
```java
public class TaskRunnable implements Runnable {
     BlockingQueue<Task> queue;
     public void run() {
          try {
               processTask(queue.take());
          } catch(InterruptedException e) {
               //restore interrupted status
               Thread.currentThread().interrupt();
          }
     }
}
```

You can get much more sophisticated with interruption, but these two approaches should work in the vast majority of situations. But there is one thing you should not do with *InterruptionException* - catch it and do nothing in response. This deprives code higher up on the call stack of the opportunity to act on the interruption, because the evidence that the thread was interrupted is lost. The only situation in which it is acceptable to swallow an interrupt is when you are extending Thread and therefore control all the code higher up on the call stack.

### 5.5.2 Future Task
**Using *FutureTask* to Preload Data that is needed later.**
```java
public class Preloader {
     private final FutureTask<ProductInfo> future = new FutureTask<>(new Callable<>(){
          public ProductInfo call() throws DataLoadException {
               return loadProductInfo();
          }
     });

     private final Thread thread = new Thread(future);

     public void start() {
          thread.start();
     }

     public ProductInfo get() throws DataLoadException, InterruptedException{
          try {
               return future.get();
          } catch(ExecutionException e) {
               Throwable cause = e.getCause();
               if(cause instance of DataLoadException) {
                    throw (DataLoadException) cause;
               } else {
                    throw launderThrowable(cause);
               }
          }
     }
}
```

*Preloader* creates a FutureTask that describes the task of loading product information from a database and a thread in which the computation will be performed. It provides a start method to start the thread, since it is inadvisable to start a thread from a constructor or static initializer. When the program later needs the *ProductInfo*, it can call get, which returns the loaded data if it is ready, or waits for the load to complete if not.

Tasks described by Callable can throw checked and unchecked exceptions, and any code can throw an *Error*. Whatever the task code my throw, it is wrapped in an *ExecutionException* and rethrown from *Future.get*. This complicates code that calls get, not only because it must deal with the possibility of ExecutionException (and the unchecked CancellationException), but also because the cause of the ExecutionException is returned as a Throwable, which is inconvenient to deal with.

When get throws an *ExecutionException* in *Preloader*, the cause will fall into on of three categories: 
- a checked exception thrown by the *Callable*,
- a *RuntimeException*,
- or an *Error*.

We must handle each of these cases separately, but we will us the *landerThrowable* utility method in below to encapsulate some of the **messier** exception-handling logic. Before calling *launderThrowable*, *Preloader* tests for the known checked exceptions and rethrows them. That leaves only unchecked exceptions, which *Preloader* handles by calling *launderThrowable* and throwing the result. If the *Throwable* passed to *launderThrowable* is an *Error*, *launderThrowable* rethrows it directly; if it is not a *RuntimeException*, it throws and *IllegalStateException* to indicate a logic error. That leaves only *RuntimeException*, which *launderThrowable* returns to its caller, and which the caller generally rethrows.

***Coercing and Unchecked *Throwable* to a *RuntimeException***
```java
/**
 * If the Throwable is an Error, throw it;
 * If it is a RuntimeException return it,
 * otherwise throw IllegalStateException.
 */
 public static RuntimeException launderThrowable(Throwable t) {
      if(t instanceof RuntimeException) {
           return (RuntimeException) t;
      } else if (t instanceof Error) {
           throw (Error)  t;
      } else {
           throw new IllegalStateException("Not unchecked", t);
      }
 }
```

### 5.5.3 Semaphores
Counting semaphores are used to control the number of activities that can access a certain resource or perform a given action at the same time. Counting semaphores can be used to implement resource pools or to impose a bound on a collection.


A degenerate case of a counting semaphore is a binary semaphore, a Semaphore with an initial count of one. A binary semaphore can be used as a mutex with non-reentrant locking semantics; whoever holds the sole permit holds the mutex.

Uses of semaphores:
1. implementation of resource pools such as database connection pools. See the example used in the bounded buffer class in **Chapter 12**(An easier way to construct a blocking objet pool would be to use a *BlockingQueue* to hold the pooled resources.).
2. Turn any collection into a blocking bounded collection, as illustrated by *BoundedHashSet* in Listing **5.14**.


**Listing 5.14 Using Semaphore to Bound a Collection.**
```java
public class BoundedHashSet<T> {
     private final Set<T> set;
     private final Semaphore sem;

     public BoundHashSet(int bound) {
          this.set = Collections.synchronizedSet(new HashSet<T>());
          sem = new Semaphore(bound);
     }

     public boolean add(T o) throws InterruptedException {
          sem.acquire();
          boolean wasAdded = false;
          try {
               wasAdded = set.add(o);
               return wasAdded;
          } finally {
               if(!wasAdded) {
                    sem.release();
               }
          }
     }

     public boolean remove(Object o) {
          boolean wasRemoved = set.remove(o);
          if (wasRemoved) {
               sem.release();
          }
          return wasRemoved;
     }
}
```

### 5.5.4 Barriers

*CyclicBarrier* allows a fixed number of parties to rendezvous repeatedly at a barrier point and is useful in parallel iterative algorithms that break down a problem into a fixed number of independent subproblems. If a call to await times times out or a thread blocked in await is interrupted, then the barrier is considered broken and all outstanding calls to await terminate with *BrokenBarrierException*.

*CellularAutomata* in Listing 5.15 demonstrates using a barrier to compute a cellular automata simulation, such as Conway's Life game(Gardner, 1970).


**Coordinating Computation in a Cellular Automaton with CyclicBarrier**
```java
public class CellularAutomata {
     private final Board mainBoard;
     private final CyclicBarrier barrier;
     private final Worker[] workers;

     public CellularAutomata(Board board) {
          this.mainBoard = board;
          int count = Runtime.getRuntime().availableProcessors();
          this.barrier = new CyclicBarrier(count, new Runnable() {
               public void run() {
                    mainBoard.commitNewValues();
               }
          });
          this.workers = new Worker[count];
          for (int i = 0; i < count; i++) {
               workers[i] = new Work(mainBoard.getSubBoard(count, i));
          }
     }

     private class Worker implements Runnable {
          private final Board board;

          public Worker(Board board) {
               this.board = board;
          }

          public void run() {
               while(!board.hasConverged()) {
                    for (int x = 0; x <board.getMaxX(); x++) {
                         for (int y =0; y< board.getMaxY(); y++) {
                              board.setNewValue(x, y, computeValue(x, y));
                         }
                    }
                    try {
                         barrier.await();
                    } catch (InterruptedException ex) {
                         return ;
                    } catch (BrokenBarrierException ex) {
                         return ;
                    }
               }
          }
     }

     public void start() {
          for(int i =0; i< workers.length; i++) {
               new Thread(workers[i]).start();
          }
          mainBoard.waitForConvergence();
     }
}
```

## 5.6 Building an Efficient, Scalable Result Cache

Caching a Future instead of a value creates the possibility of cache pollution: if a computation is cancelled ore fails, future attempts to compute the result will also indicate cancellation or failure. To avoid this, *Memorizer* removes the Future from the cache if it detects that the computation was cancelled; it might also be desirable to remove the Future upon detecting a *RuntimeException* if the computation might succeed on a future attempt. *Memorizer* also does not address cache expiration, but this could be accomplished by suing a subclass of *FutureTask* that associates an expiration time with each result and periodically scanning the cache for expired entires.(Similarly, it does not address cache eviction, where old entires are removed to make room for new ones so that the cache does not consume too much memory.)

**Final Implementation of Memorizer**
```java
public class Memorizer<A, V> implements Computable<A,V> {
     private final ConcurrentMap<A, Future<V> cache> = new ConcurrentHashMap<>();
     private final Computable<A, V> c;

     public V compute(final A arg) throws InterruptedException {
          while(true) {
               Future<V> f = cache.get(arg);
               if (f == null) {
                    Callable<V> eval = new Callable<V>() {
                         public V call() throws InterruptedException {
                              return c.compute(arg);
                         }
                    };
                    FutureTask<V> ft = new FutureTask<>(eval);
                    f = cache.putIfAbsent(arg, ft);
                    if(f == null) {
                         f == ft;
                         ft.run();
                    }
               }
               try {
                    return f.get();
               } catch (CancellationException e) {
                    cache.remove(arg, f);
               } catch (ExecutionException e) {
                    throw launderThrowable(e.getCause());
               }
          }
     }
}
```

**Factorizing Servlet that Caches Results Using Memorizer**
```java 
@ThreadSace
public class Factorizer implements Servlet {
     private final Computable<BigInteger, BigInteger[]> c = new Computable<>(){
          public BigInteger[] compute(BigInteger arg) {
               return factor(arg);
          }
     };

     private final Computable<BigInteger, BigInteger[]> cache = new Memorizer<>(c);

     public void service(ServletRequest req, ServletResponse resp) {
          try {
               BigInteger i = extractFromRequest(req);
               encodeIntoResponse(resp, cache.compute(i));
          } catch(InterruptedException e) {
               encodeError(resp, "Factorization Interrupted");
          }
     }
}
```

# Summary of Part I
- It's the mutable state, stupid.
All concurrency issues boil down to coordinating access to mutable state. The less mutable state, the easier it is to ensure thread safety.
- Make fields final unless they need to be mutable.
- Immutable objects are automatically thread-safe.
Immutable objects simplify concurrent programming tremendously. They are simpler and safer, and can be shared freely without locking or defensive copying.
- Encapsulation makes it practical to manage the complexity.
You could write a thread-safe program with all data stored in global variables, but why would you want to? Encapsulating date within objects makes it easier to preserve their invariants; encapsulating synchronization within objects makes it easier to comply with their synchronization policy.
- Guard each mutable variable with a lock.
- Guard all variables in an invariant with the same lock.
- Hold locks for the duration of compound actions.
- A program that accesses a mutable variable from multiple threads without synchronization is a broken program.
- Don't rely on clever reasoning about why you don't need to synchronize.
- Include thread safety in the design processor explicitly document that your class is not thread-safe.
- Document your synchronization policy.

#
### 6.2.4 Executor Life cycle
JVM can't exit until all the (non-daemon) threads have terminated, so failing to shut down an Executor could prevent the JVM from exiting.

In shutting down an application, there is a **spectrum** from graceful shutdown (finish what you've started but don't accept any new work) to **abrupt** shutdown (turn off the power to the machine room), and various points in between. Since Executors provide a service to applications, they should be able to be shut down as well, both gracefully and abruptly, and feedback information to the application about the status of tasks that were affected by the shutdown.

To address the issue of execution service lifecycle, the *ExecutorService* interface extends *Executor*, adding a number of methods for lifecycle management (as wll as some convenience methods for task submission). The lifecycle management methods of *ExecutorService*  are shown in Listing 6.7

**Listing 6.7 Lifecycle Methods in ExecutorService**
```java
public interface ExecutorService extends Executor {
     void shutdown();
     List<Runnable> shutdownNow();
     boolean isShutdown();
     boolean isTerminated();
     boolean awaitTermination(long timeout, TimeUnit unit) throw InterruptedException;
     // ... additional convenience methods for task submission.
}
```

The lifecycle implied by *ExecutorService* has three states:
- running (ExecutorServices are initially created in the running state),
- shutting down (the graceful *shutdown* method, or the abrupt *shutdownNow* method),
- and terminated (Once all tasks have completed, the *ExecutorService* transitions to the terminated state).

:warning: You can wait for an ExecutorService to reach the terminated state with *awaitTermination*, or poll for whether it has ye terminated with *isTerminated*. It is common to follow *shutdown* immediately by *awaitTermination*, creating the effect of synchronously shutting down the *ExecutorService*.

### 6.2.5 Delayed and Periodic Tasks
The *Timer* facility manages the execution of deferred("run this task in 100ms") and periodic("run this task every 10ms") tasks. However, Timer has some drawbacks, and *ScheduledThreadPoolExecutor* should be thought of as its replacement.
> *Timer* does have support for scheduling based on absolute, not relative time, so that tasks can be sensitive to changes in the system clock; *ScheduledThreadPoolExecutor* supports only relative time.

The *Timer*'s drawbacks are:
1.  A *Timer* creates only a single thread for executing timer tasks. If a timer task takes too long to run, the timing accuracy of other *TimerTasks* can suffer.
     > Scheduled thread pools address this limitation by letting you provide multiple threads for executing deferred and periodic tasks.
2. A *Timer* behaves poorly if a *TimerTask* throws an unchecked exception. An unchecked exception thrown from a *TimerTask* terminates the timer thread. *Timer* also doesn't resurrect the thread in this situation; instead, it erroneously assumes the entire *Timer* was cancelled. In this case, *TimerTasks* that are already scheduled but not yet executed are never run, and new tasks cannot be scheduled.(This problem is called "thread leakage")

OutOfTime in Listing 6.9 illustrates how a *Timer* can become confused in this manner. You might expect the program to run for six seconds and exit, but what actually happens is that it terminates after on second with an *IllegalStateException* whose message text is "Timer already cancelled".

:cry:**Listing 6.9 Class Illustrating Confusing Timer Behavior**
```java
public class OutOfTime{
     public static void main(String[] args) throw Exception {
          Timer timer = new Timer();
          timer.schedule(new ThrowTask(), 1);
          SECONDS.sleep(1);
          timer.schedule(new ThrowTask(), 1);
          SECONDS.sleep(5);
     }

     static class ThrowTask extends TimerTask{
          public void run() {
               throw new RuntimeException();
          }
     }
}
```

### 6.3.4 Limitations of Parallelizing Heterogeneous Tasks

:cry: **Listing 6.13 Waiting for Image Download with Future**
```java
public class FutureRenderer {
     private final ExecutorService executor = ...;

     void renderPage(CharSequence source) {
          final List<ImageInfo> imageInfos = scanForImageInfo(source);
          Callable<List<ImageData>> task = new Callable<>(){
               public List<ImageData> call() {
                    List<ImageData> result = new ArrayList<>();
                    for (ImageInfo imageInfo : imageInfos) {
                         result.add(imageInfo.downloadImage());
                    }
                    return result;
               }
          };

          Future<List<ImageData>> future = executor.submit(task);
          renderText(source);

          try {
               List<ImageData> imageData = future.get();
               for(ImageData data : imageData) {
                    renderImage(data);
               }
          } catch (InterruptedException e) {
               // Re-assert the thread's interrupted status
               Thread.currentThread().interrupt();
               // We don't need the result, so cancel the task too
               future.cancel(true);
          } catch( ExecutionException e) {
               throw launderThrowable(e.getCause());
          }
     }
}
```
:imp: In the last example, we tried to execute two different types of tasks in parallel - downloading the images and rendering the page. But obtaining significant performance improvements by trying to parallelize sequential heterogeneous tasks can be tricky.  Try to increase concurrency by parallelizing heterogenous activities can be a lot of work, and there is a limit to how much additional concurrency you can get out of it.

🍖:The real performance **payoff** of dividing a program's workload into tasks comes when there are a large number of independent, **homogeneous**  tasks that can be processed concurrently. 

### 6.3.5 *CompletionService* : Executor Meets *BlockingQueue*
*CompletionService* combines the functionality of an *Executor* and a *BlockingQueue*. You can submit *Callable* tasks to it for execution and use the queue-like methods *take* and *poll* to retrieve completed results, packaged as Futures, as they become available.

The implementation of *ExecutorCompletionService* is quite straightforward. The constructor creates a *BlockingQueue* to hold the completed results. Future-Task has a done method that is called when the computation completes. When a task is submitted, it is wrapped with a *QueueingFuture*, a subclass of *FutureTask* that overrides done to place the result on the BlockingQueue, as shown in Listing 6.14. The take and poll methods delegate to the BlockingQueue, blocking if results are not yet available.
**Listing 6.14 QueueingFuture Class Used By ExecutorCompletionService**
```java
private class QueueingFuture<V> extends FutureTask<V> {
     QueueingFuture(Callable<V> c) {
          super(c)；
     }
     QueueingFuture(Runnable t, V r) {
          super(t, v);
     }

     protected void done() {
          //BlockingQueue hold the result.
          completionQueue.add(this);
     }
}
```

### 6.3.6 Example: Page Renderer with CompletionService
:smile:We can use a *CompletionService* to improve the performance of the page renderer in two ways:
- shorter total runtime:
     > Create a separate task for downloading each image and execute them in a thread pool, turing the sequential download into a parallel one: this reduces the amount of time to download all the images.
- improved responsiveness:
     > By fetching results from the CompletionService and rendering each image as soon as it is available, we can give the user a more dynamic and responsive user interface.


**Listing 6.15 Using CompletionService to Render Page Elements as they become available**
```java
public class Renderer {
     private final ExecutorService executor;

     Renderer(ExecutorService executor) {
          this.executor = executor;
     }

     void renderPage(CharSequence source) {
          final List<ImageInfo> info = scanForImageInfo(source);
          CompletionService<ImageData> completionService = new ExecutorCompletionService<>(executor);
          for (ImageInfo imageInfo : info) {
               completionService.submit(new Callable<>{
                    public ImageData call() {
                         return imageInfo.downloadImage();
                    }
               });
          }
          renderText(source);

          try {
               for (int t = 0, n = info.size(); t < n; t++) {
                    Future<ImageData> f = completionService.take();
                    ImageData imageData = f.get();
                    renderImage(imageData);
               }
          } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
          } catch (ExecutionException e) {
               throw launderThrowable(e.getCause());
          }
     }
}
```

---

# Chapter 7. Cancellation and shutdown
Getting tasks and threads to stop safely, quickly, and reliably is not always easy. Java does not provide any mechanism for safely forcing a thread to stop what it is doing. Instead, it provides interruption, a cooperative mechanism that lets one thread ask another to stop what it is doing.

The cooperative approach is required because we rarely want a task, thread, or service to stop immediately, since that could leave shared data structures in an inconsistent state. Instead, tasks and services can be coded so that, when requested, they clean up any work currently in progress and then terminate. This provides greater flexibility, since the task code itself is usually better able to assess the cleanup required than is the code requesting cancellation.

## 7.1 Task Cancellation
### 7.1.1 Interruption
:warning: There is nothing in the API or language specification that ties interruption to any specific cancellation semantics, but in practice, using interruption for anything but cancellation is fragile and difficult to sustain in larger applications.

Each thread has a boolean interrupted status; interrupting a thread sets its interrupted status to true. Thread contains methods for interrupting a thread and querying the interrupted status of a thread.

**Interruption Methods in Thread**
```java
public class Thread {
     public void interrupt(){...}
     public boolean isInterrupted() {...}
     
     //返回线程状态，然后重置线程的中断状态为false. 这是唯一的可以清除线程中断状态的方法
     public static boolean interrupted() {...}
}
```

Blocking library methods like Thread.sleep and Object.wait try to detect when a thread has been interrupted and return early. They respond to interruption by clearing the interrupted status and throwing InterruptedException, indicating that the blocking operation completed early due to interruption. The JVM makes no guarantees on how quickly a blocking method will detect interruption, but in practice this happens reasonably quickly.

If a thread is interrupted when it is not blocked, its interrupted status is set, and it is up to the activity being cancelled to poll the interrupted status to detect interruption. **:warning: In this way interruption is "sticky" if it doesn't trigger an *InterruptedException*, evidence of interruption persists until someone  deliberately clears the interrupted status.**

🐵Calling interrupted does not necessarily stop the target thread from doing what it is doing; it merely delivers the message that interruption has been requested.

A good way to think about interruption is that it does not actually interrupt a running thread; it just requests that the thread interrupt itself at the next convenient opportunity. (These opportunities are called cancellation points.) Some methods, such as *wait*, *sleep*, and *join*, take such requests seriously, throwing an exception when they receive an interrupt request or encounter an already set interrupt interrupt status upon entry. 
- Well behaved methods my totally ignore such requests so long as they leave the interruption request in place so that calling code can do something with it. 
- Poorly behaved methods **swallow**  the interrupt request, thus denying code further up the call stack the opportunity to act on it.

:pencil: The static interrupted method should be used with caution, because it clears the current thread's interrupted status. If you call interrupted and it returns True, unless you are planning to swallow the interruption, you should do something with it -- either throw *InterruptedException* or restore the interrupted status by calling *interrupt* again.

:pill: Interruption is usually the most sensible way to implement cancellation.

### 7.1.2 Interruption Policies
It is important to distinguish between how tasks and threads should react to interruption. A single interrupt request my have more than one desired recipient interrupting a worker thread in a thread pool can mean both "cancel the current task" and "shut down the worker thread".

Tasks do not execute in threads they own; they borrow threads owned by a service such as a thread pool. Code that doesn't own the thread (for a thread pool, and code outside of the thread pool implementation) should be careful to preserve the interrupted status so that the owning code can eventually act on it, even if the "guest" code acts on the interruption as well. (If you are house-sitting for someone, you don't throw out the mail that comes while they're away. You save it and let them deal with it when they get back, even if you do read their magazines.)

This is why most blocking library method simply throw *InterruptedException* in response to an interrupt. They will never execute in a thread they own, so they implement the most reasonable cancellation policy for task or library code: get out of the way as quickly as possible and communicate the interruption back to caller so that code higher up on the call stack can take further action.

Whether a task interprets interruption as cancellation or takes some other action on interruption, it should take care to preserve the executing thread's interruption status. If it is not simply going to propagate *InterruptedException* to its caller,  it should restore the interruption status after catching *InterruptedException*：
```java
//restore the interruption status after catching InterruptedException
Thread.currentThread().interrupt(); 
```

:pill: Because each thread has its own interruption policy, you should not interrupt a thread unless you know what interruption means to that thread.

### 7.1.3 Responding to Interruption
Two practical strategies for handling *InterruptedException*:
- Propagate the exception (possibly after some task-specific cleanup), making your method an interruptible blocking method, too; or
- Restore the interruption status so that code higher up on the call stack can deal with it.

:key: Only code that implements a thread's interruption policy may swallow an interruption request. General-purpose task and library code should never swallow interruption requests.

**Activities that do not support cancellation but still can interruptible blocking methods will have to call them in a loop, retrying when interruption is detected.** In this case, they should save the interruption status locally and restore it just before returning, as shown in Listing 7.7, rather than immediately upon catching *InterruptedException*. Setting the interrupted status too early could result in an infinite loop, because most interruptible blocking methods check the interrupted status on entry and throw *InterruptedException* immediately if it is set. (Interruptible methods usually pool for interruption before blocking or doing any significant work, so as to be responsive to interruption as possible.)

**List 7.7 Non-cancelable Task that Restores Interruption before exit**
```java
public Task getNextTask(BlockingQueue<Task> queue) {
     boolean interrupted = false;
     try {
          while (true) {
               return queue.take();
          } catch(InterruptedException e) {
               interrupted = true;
               // fall through and retry
          }
     } finally {
          if (interrupted) {
               Thread.currentThread().interrupt();
          }
     }
}
```

If your code does not call interruptible blocking methods, it can still be made responsive to interruption by polling the current thread's interrupted status throughout the task code. Choosing a polling frequency is a tradeoff between efficiency and responsiveness.

### 7.1.4 Example: Timed Run
:cry: **Listing 7.8 Scheduling an Interrupt on a Borrowed Thread.** Don't do this.
```java
private static final ScheduledExecutorService cancelExec =  ...;

public static void timedRun(Runnable r, long timeout, TimeUnit unit) {
     final Thread taskThread = Thread.currentThread();
     cancelExec.schedule(new Runnable() {
          public void run() {
               taskThread.interrupt();
          }
     }, timeout, unit);
     r.run();
}
```
We can't do this because:
- it violates the rules: you should know a thread's interruption policy before interrupting it.
- we don't know when would the task complete. If the task completes before the timeout, the cancellation task that interrupts the thread in which timedRun was called could go off after timedRun has returned to its caller.
- We don't know if the task is responsive to interruption. If the task is not responsive to interruption, timedRun will not return until the task finishes.

### 7.1.5 Cancellation Via Future
:smiley: Listing 7.10 Cancelling a Task Using Future
```java
public static void timeRun(Runnable r, long timeout, TimeUnit unit) throws InterruptedException {
     Future<?> task = taskExec.submit(r);
     try {
          task.get(timeout, unit);
     } catch (TimeoutException e) {
          // task will be cancelled below
     } catch (ExecutionException e) {
          // exception thrown in task; rethrow
          throw launderThrowable(e.getCause());
     } finally {
          // Harmless if task already completed
          task.cancel(true); // interrupt if running
     }
}
```

### 7.1.6 Dealing with Non-interruptible Blocking
Many blocking library methods respond to interruption by returning early and throwing *InterruptedException*, which makes it easier to build tasks that are responsive to cancellation. However, not all blocking methods or blocking mechanisms are responsive to interruption; if a thread is blocked performing synchronous socket I/O or waiting to acquire an intrinsic lock, interruption has no effect other than setting the thread's interrupted status. We can sometimes convince threads blocked in non-interruptible activities to stop by means similar to interruption, but this requires greater awareness of why the thread is blocked.

Synchronous socket I/O in java.io. The common form of blocking I/O in server applications is reading or writing to a socket. Unfortunately, the read and write methods in **InputStream** and **OutputStream** are not responsive to interruption, but closing the underlying socket makes any threads blocked in read or write throw a **SocketException**.

Synchronous I/O in java.nio. Interrupting a thread waiting on an **InterruptibleChannel** causes it to throw **ClosedByInterruptException** and close the channel (and also causes all other threads blocked on the channel to throw **ClosedByInterruptException**). Closing an **InterruptibleChannel** causes threads blocked on channel operations to throw **AsynchronousCloseException**. Most standard Channels implement **InterruptibleChannel**.

Asynchronous I/O with Selector. If a thread is blocked in Selector.select (in java.nio.channels), wakeup causes it to return prematurely by throwing a **ClosedSelectorException**.

Lock acquisition. If a thread is blocked waiting for an intrinsic lock, there is nothing you can do to stop it, **short of** ensuring that it eventually acquires the lock and makes enough progress that you can get its attention some other way. However, the explicit Lock classes offer the **lockInterruptibly** method, which allows you to wait for a lock and still be responsive to interrupts.

ReaderThead in Listing 7.11 shows a technique for encapsulating nonstandard cancellation. ReaderThread manages a single socket connection, reading synchronously from the socket and passing any data received to processBuffer. To facilitate terminating a user connection or shutting down the server, ReaderThread overrides interrupt to both deliver a standard interrupt and close the underlying socket; thus interrupting a ReaderThread makes it stop what it is doing whether it is blocked in read or in an interruptible blocking method.

**List 7.11 Encapsulating Nonstandard Cancellation in a Thread by Overriding Interrupt**
```java
public class ReaderThread extends Thread {
     private final Socket socket;
     private final InputStream in;

     public ReaderThread(Socket socket) throws IOException {
          this.socket = socket;
          this.in = socket.getInputStream();
     }

     public void interrupt() {
          try {
               socket.close();
          } catch (IOException ignored) {}
          finally {
               super.interrupt();
          }
     }

     public void run() {
          try {
               byte[] buf = new byte[BUFSZ];
               while (true) {
                    int count = in.read(buf);
                    if(count < 0) {
                         break;
                    } else if (count > 0) {
                         processBuffer(buf, count);
                    }
               } catch (IOException e) {
                    // Allow thread to exit
               }
          }
     }
}
```
### 7.1.7 Encapsulating Nonstandard Cancellation with Newtaskfor
CancellableTask in Listing 7.12 defines a CancellableTask interface that extends Callable and adds a cancel method and a newTask factory method for constructing a RunnableFuture. CancellingExecutor extends ThreadPoolExecutor, and overrides newTaskFor to let a CancellableTask create its own Future.

**Listing 7.12 Encapsulating Nonstandard Cancellation in a Task with Newtaskfor**
```java
public interface CancellableTask<T> extends Callable<T> {
     void cancel();
     RunnableFuture<T> newTask();
}

@ThreadSafe
public class CancellingExecutor extends ThreadPoolExecutor {
     ...
     protected<T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
          if (callable instanceof CancellableTask){
               return ((CancellableTask<T>) callable).newTask();
          } else {
               return super.newTaskFor(callable);
          }
     }
}

public abstract class SocketUsingTask<T> implements CancellableTask<T> {
     @GuardedBy("this")
     private Socket socket;
     protected synchronized void setSocket(Socket s) {
          socket = s;
     }

     public synchronized void cancel() {
          try {
               if (socket != null) {
                    socket.close();
               }
          } catch (IOException ignored){}
     }

     public RunnableFuture<T> newTask() {
          return new FutureTask<T>(this) {
               public boolean cancel(boolean mayInterruptIfRunning) {
                    try {
                         SocketUsingTask.this.cancel();
                    } finally {
                         return super.cancel(mayInterruptIfRunning);
                    }
               }
          }
     }
}
```

## 7.2 Stopping a Thread-based Service
Sensible encapsulation practices dictate that you should not manipulate a thread - interrupt it, modify its priority, etc. - unless you own it. The thread API has no formal concept of thread ownership: a thread is represented with a Thread object that can be freely shared like an other object. However, it makes sense to think of a thread as having an owner, and this is usually the class that created the thread. So a thread pool owns its worker threads, and if those threads need to be interrupted, the thread pool should take care of it.

:pill: Provide lifecycle methods whenever a thread-owning service has a lifetime longer than that of the method that created it.

### 7.2.2 ExecutorService Shutdown
More sophisticated programs are likely to encapsulate an ExecutorService behind a higher-level service that provides its own lifecycle methods, such as the variant of LogService in Listing 7.16 that delegates to an ExecutorService instead of managing its own threads.
**Listing 7.16 Logging Service that Uses an ExecutorService**
```java
public class LogService {
     private final ExecutorService exec = newSingleThreadExecutor();
     ...
     public void start() {}

     public void stop() throws InterruptedException {
          try {
               exec.shutdown();
               exec.awaitTermination(TIMEOUT, UNIT);
          } finally {
               writer.close();
          }
     }

     public void log(String msg) {
          try {
               exec.execute(new WriteTask(msg));
          } catch (RejectedExecutionException ignored) {

          }
     }
}
```

### 7.2.3 Poison Pills
Another way to convince a producer-consumer service to shut down is with a poison pill: a recognizable object placed on the queue that means "when you get this, stop." With a FIFO queue, poison pills ensure that consumers finish the work on their queue before shutting down, since any work submitted prior to submitting the poison pill will be retrieved before the pill; producers should not submit any work after putting a poison pill on the queue.

### 7.2.4 Example: A One-shot Execution Service
If a method needs to process a batch of tasks and does not return until all the tasks are finished, it can simplify service lifecycle management by using a private Executor whose lifetime is bounded by that method.(The invokeAll and invokeAny methods can often be useful in such situations.)

**Listing 7.20 Using a Private Executor Whose Lifetime is Bounded by a Method Call**
```java
boolean checkMail(Set<String> hosts, long timeout, TimeUnit unit) throws InterruptedException {
     ExecutorService exec = Executors.newCachedThreadPool();
     final AtomicBoolean hasNewMail = new AtomicBoolean(false);
     try {
          for(String host : hosts) {
               exec.execute(() -> {
                    if (checkMail(host)) {
                         hasNewMail.set(true);
                    }
               });
          }
     } finally {
          exec.shutdown();
          exec.awaitTermination(timeout, unit);
     }
     return hasNewMail.get();
}
```
### 7.2.5 Limitations of shutdownnow
There is no general way of knowing the state of the tasks in progress at shutdown time unless that tasks themselves perform some sort of checkpointing.

TrackingExecutor in Listing 7.21 shows a technique for determining which tasks were in progress at shutdown time.

TrackingExecutor has an unavoidable race condition that could make it yield **false positives**: tasks that are identified as cancelled but actually completed. This arises because the thread pool could be shut down between when the last instruction of the task executes and when the pool records the task as complete. This is not a problem if tasks are idempotent (if performing them twice has the same effect as performing them once), as they typically are in a web crawler. Otherwise, the application retrieving the cancelled tasks must be aware of this risk and be prepared to deal with false positives.

**List 7.21 ExecutorService that Keeps Track of cancelled tasks after shutdown**
```java
public class TrackingExecutor extends AbstractExecutorService {
     private final ExecutorService exec;
     private final Set<Runnable> taskCancelledAtShutdown = Collections.synchronizedSet(new HashSet<>());
     ...

     public List<Runnable> getCancelledTasks() {
          if(!exec.isTerminated()) {
               throw new IllegalStateException(...);
          }
          return new ArrayList<>(taskCancelledAtShutdown);
     }

     public void execute(Runnable runnable) {
          exec.execute(() -> {
               try {
                    runnable.run();
               } finally {
                    if (isShutdown()
                    && Thread.currentThread().isInterrupted()) {
                         taskCancelledAtShutdown.add(runnable);
                    }
               }
          });
     }

     //delegate other ExecutorService methods to exec
}
```

**List 7.22 Using TrackingExecutorService to Save unfinished tasks for later execution**
```java
public abstract class WebCrawler {
     private volatile TrackingExecutor exec;
     @GuardedBy("this")
     private final Set<URL> urlsToCrawl = new HashSet<>();
     ...

     public synchronized void start() {
          exec = new TrackingExecutor(Executors.newCachedThreadPool());
          for (URL url : urlsToCrawl) {
               submitCrawTask(url);
          }
          urlsToCraw.clear();
     }

     public synchronized void stop() throws InterruptedException {
          try {
               saveUncrawled(exec.shutdownNow());
               if(exec.awaitTermination(TIMEOUT, UNIT)) {
                    saveUncrawled(exec.getCancelledTasks());
               }
          } finally {
               exec = null;
          }
     } 

     protected abstract List<URL> processPage(URL url);

     private void saveUncrawled(List<Runnable> uncrawled) {
          for (Runnable task : uncrawled) {
               urlsToCraw.add(((CrawTask) task).getPage());
          }
     }
     private void submitCrawTask(URL u) {
          exec.execute(new CrawTask(u));
     }

     private class CrawTask implements Runnable {
          private final URL url;
          ...
          public void run() {
               for (URL link : processPage(url)) {
                    if (Thread.currentThread().isInterrupted()) {
                         return;
                    }
                    submitCrawTask(link);
               }
          }

          public URL getPage() {
               return url;
          }
     }
}
```

## 7.3 Handling Abnormal Thread Termination
:pill: Just about any code can throw a _RuntimeException_. Whenever you call another method, you are taking a leap of faith that it will return normally or throw one of the checked exceptions its signature declares. **The less familiar you are with the code being called, the more skeptical you should be about its behavior.** Always call tasks within a try-catch block that catches unchecked exceptions, or within a try-finally block to take corrective action. 

### 7.3.1 Uncaught Exception Handlers
The Thread API also provides the _UncaughtExceptionHandler_ facility, which lets you detect when a thread dies due to an uncaught exception.

When a thread exits due to an uncaught exception, the JVM reports this event to an application-provided _UncaughtExceptionHandler_; if no handler exists, the default behavior is print the stack trace to System.err.
```java
public interface UncaughtExceptionHandler {
     void uncaughtException(Thread t, Throwable e);
}
```

To set an _UncaughtExceptionHandler_ for pool threads, provide a _ThreadFactory_ to the _ThreadPoolExecutor_ constructor.

Somewhat confusingly, exceptions thrown from tasks make it to the uncaught exception handler only for tasks submitted with execute; for tasks submitted with submit, any thrown exception, checked or not, is considered to be part of the task's return status. If a task submitted with submit terminates with an exception, it is rethrown by Future.get, wrapped in an _ExecutionException_.

## 7.4 JVM Shutdown
Two ways that can shut down JVM:
1. **An orderly shutdown**
An orderly shutdown is initiated when **the last "normal" (non-daemon) thread terminates**, someone calls **System.exit**, or by other platform-specific means (such as sending a **SIGINT** or hitting **Ctrl-C**). This is the standard and preferred way for the JVM to shut down.
2. **Abrupt shutdown**
JVM can also be shut down abruptly by calling **Runtime.halt** or by **killing the JVM process through the operation** system(such as sending a **SIGKILL**).

### 7.4.1 Shutdown Hooks
#### Orderly shutdown
In an orderly shutdown:
1. The JVM first starts all registered shutdown hooks.
Shutdown hooks are unstarted threads that are registered with **Runtime.addShutdownHook**. The JVM makes no guarantees on the order in which shutdown hooks are started.
2. If any **application threads** (daemon or nondaemon) are still running at shutdown time, they continue to **run concurrently with the shutdown process**.
3. When all shutdown hooks have completed, the JVM may choose to run finalizers if **runFinalizerOnExit** is ture, and then halts.
The JVM makes no attempt to stop or interrupt any application threads that are still running at shutdown time; they are abruptly terminated when the JVM eventually halts.
4. If the shutdown hooks or finalizers don't complete within a certain time (_if you ask me how long does this process takes, I don't know..._ 我自己加的), then the orderly shutdown process "hangs" and the JVM must be shutdown abruptly.

#### Abrupt shutdown
In an abrupt shutdown, the JVM is not required to do anything other halt the JVM; **shutdown hooks will not run**.

#### Summary
:pill: Tips:
1. Shutdown hooks should be thread-safel;
2. They should not make assumptions about the state of the application (such as whether other services have shut down already or all normal threads have completed) or about why the JVM is shutting down, and must therefore be coded extremely defensively;
3. They should exit as quickly as possible.

:book: Usage scenarios:
1. Service or application cleanup, such as **deleting temporary files** or **cleaning up resources** that are not automatically cleaned up by the OS.
2. 停止日志服务
3. 内存数据写到磁盘
4. **use a single shutdown hook for all services**, rather than one for each service, and have it call a series of shutdown actions. This ensures that shutdown actions execute sequentially in a single thread, thus avoiding the possibility of race conditions or deadlock between shutdown actions.  

### 7.4.2 Daemon Threads
Two types of threads:
1. normal threads
2. daemon threads

:book: When the JVM starts up, **all the threads it creates (such as garbage collector and other housekeeping threads) are daemon thread, exception the main thread**. When a new thread is created, it **inherits the daemon status of the thread that created it**, so by default any threads created by the main thread are also normal threads.

:key: Normal threads and daemon threads differ only in what happens when they exit. **When a thread exits**, the JVM performs an inventory of running threads, and if the only threads that are left are daemon threads, it initiates an orderly shutdown. **When the JVM halts**, any remaining daemon threads are abandoned - finally blocks are not executed, stacks are not unwound - the JVM just exits.

:pill: Daemon threads should be used sparingly - few processing activities can be safely abandoned at any time with on cleanup. In particular, it is dangerous to use daemon threads for tasks that might perform any sort of I/O. Daemon threads are best saved for "housekeeping" tasks, such as a background thread that periodically removes expired entries form an in-memory cache.

### 7.4.3 Finalizers
In most cases, the combination of finally blocks and explicit close methods does a better job of resource management than finalizers; the sole exception is when you need to manage objects that hold resources acquired by native methods. For these reasons and others, work hard to avoid writing or using classes with finalizers (other than the platform library classes).
:pill: Avoid finalizers


# Chapter 8. Applying Thread Pools
## 8.1 Implicit Couplings Between Tasks and Execution Policies
Types of tasks that require specific execution policies include:
1. Dependent tasks. When you submit tasks that depend on other tasks to a thread pool, you implicitly create constraints on the execution policy that must be carefully managed to avoid liveness problems.
2. Tasks that exploit thread confinement. Single-threaded executors make stronger promises about concurrency than do arbitrary thread pools. They guarantee that tasks are not executed concurrently. This forms an implicit coupling between the task and the execution policy - the tasks require their executor to be single-threaded. In this case, if you changed the Executor from a single-threaded one to a thread pool, thread safety could be lost.
3. Response-time-sensitive tasks. Submitting a long-running task to a single-threaded executor, or submitting several long-running tasks to a thread pool with a small number of threads, may **impair** the responsiveness of the service managed by that Executor. 
4. **Tasks that use ThreadLocal**. The standard Executor implementations may reap idle threads when demand is low and add new ones when demand is high, and also replace a worker thread with a fresh on if an unchecked exception is thrown from a task. **ThreadLocal makes sense to use in pool threads only if the thread-local value has a lifetime that is bounded by that of a task; Thread-Local should not be used in pool threads to communicate values between tasks**.
5. Thread pools work best when tasks are homogeneous and independent. Mixing long-running and short-running tasks risks "clogging" the pool unless it is very large; submitting tasks that depend on other tasks risks deadlock unless the pool is unbounded.

:pill: Some tasks have characteristics that require or preclude a specific execution policy. **Tasks that depend on other tasks require that the thread pool be large enough** that tasks are never queued or rejected; tasks that exploit thread confinement require sequential execution. Document these requirements so that future maintainers do not undermine safety or liveness by substituting an incompatible execution policy.

### 8.1.1 Thread Starvation Deadlock(线程饥饿死锁：就是没有可用的线程了，导致死锁)
If tasks that depend on other tasks execute in a thread pool, they can deadlock. 
1. **In a single-threaded executor, a task that submits another task to the same executor and waits for its results will always deadlock**. 
The second task sits on the work queue until the first task completes, but the first will not complete because it is waiting for the result of the second task.
2. In a larger thread pool, if all threads are executing tasks that are blocked waiting for other tasks still on the work queue. This is called **thread starvation deadlock**, and can occur whenever a pool task initiates an unbounded blocking wait for some resource or condition that can succeed only through the action of another pool task, such waiting for the return value or side effect of another task, unless you can guarantee that the pool is large enough.

:book: Whenever you submit to an Executor tasks that are not independent, be aware of the possibility of thread starvation deadlock, and document any pool sizing or configuration constraints in the code or configuration file where the Executor is configured.

ThreadDeadLock in Listing 8.1 illustrates thread starvation deadlock.

:cry:**List 8.1 Tak that Deadlocks in a Single-threaded Executor. Don't do this**
```java
public class ThreadDeadLock {
     ExecutorService exec = Executors.newSingleThreadExecutor();

     public class RenderPageTask implements Callable<String> {
          public String call() throws Exception {
               Future<String> header, footer;
               header = exec.submit(new LoadFileTask("header.html"));
               footer = exec.submit(new LoadFileTask("footer.html"));
               String page = renderBody();
               //will deadlock - task waiting for result of subtask
               return header.get() + page + footer.get();
          }
     }
}
```

**In addition to** any explicit bounds on the size of a thread pool, there may also be implicit limits because of constraints on other resources. If your application uses a JDBC connection pool with ten connections and each task needs a database connection, it is as if your thread pool only has ten threads because tasks in excess of ten will block waiting for a connection.

### 8.1.2 Long-running Tasks
Thread pools can have responsiveness problems if tasks can block for extended periods of time, even if deadlock is not a possibility. One technique that can mitigate the ill effects of long-running tasks is for tasks to use timed resource waits instead of unbounded waits. 

## 8.2 Sizing Thread Pools
The ideal size for a thread pool depends on the types of tasks that will be submitted and the characteristics of the deployment system. Thread pool sizes should rarely be hard-coded; instead pool sizes should be **2).provided by a configuration mechanism** or **1).computed dynamically by consulting Runtime.availableProcessors**.

Sizing thread pools is not an exact science, but fortunately you need only avoid the extremes of "too big" and "too small". If a thread pool is too big, then threads compete for scarce CPU and memory resources, resulting in higher memory usage and possible resource exhaustion. If it is too small, throughput suffers as processors go unused despite available work.

To size a thread pool properly, 
- you need to understand your computing environment,
- your resource budget,
- and the nature of your tasks.
- How many processors does the deployment system have?
- How much memory?
- Do tasks perform mostly computation, I/O, or some combination?
- Do they require a scarce resource, such as a JDBC connection?

If you have different categories of tasks with very different behaviors, consider using multiple thread pools so each can be tuned according to its workload.

- :pill: **For compute-intensive tasks**, an $N_{cpu}$-processor system usually achieves optimum utilization with  **a thread pool of $N_{cpu} + 1$ threads**. (Even compute-intensive threads occasionally take a page fault or pause for some other reason, so an "extra" runnable thread prevents CPU cycles from going unused when this happens.)

- :pill: **For tasks that also include I/O or other blocking operations**, you want a larger pool, since not all of the threads will be schedulable at all times. In order to size the pool properly, you must **estimate the ratio of waiting time to compute time for your tasks**; this estimate need not be precise and can be obtained through pro-filing or instrumentation. **Alternatively, the size of the thread pool can be tuned by running the application using several different pool sizes under a benchmark load and observing the level of CPU utilization.**

### Given these definitions:
> $N_{cpu} = $ number of CPUs
> $U_{cpu} = $ target CPU utilization, 0 <= $U_{cpu}$ <= 1
> $W/C = $ ratio of wait time to compute time

the optimal pool size for keeping the processors at the desired utilization is:
> $N_{threads} = N_{cpu} * U_{cpu} * (1 + W/C)$

You can determine the number of CPUs using _Runtime_:
```java
int N_cpus = Runtime.getRuntime().availableProcessors();
```

Of course, CPU cycles are not the only resource you might want to manage using thread pools. Other resources that can contribute to sizing constraints are memory, file handles, socket handles, and database connections. Calculating pool size constraints for these types of resources is easier: **just add up how much of the resource each task requires and divide that into the total quantity available. The result will be an upper bound on the pool size.**

> Here is an example:
> If we have 10GB of memory available, and each thread requires at most 1GB of memory. We can have this equation:
> $$number\ of\ tasks = \frac{Total\ Quantity\ Available}{Resources\ Per\ Task} = \frac{10GB}{1GB} = 10$$
> As a result, we can only have at most 10 threads.

When tasks require a pooled resource such as database connections, thread pool size and resource pool size affect each other. If each task requires a connection, the effective size of the thread pool is limited by the connection pool size. Similarly, when the only consumers of connections are pool tasks, the effective size of the connection pool is limited by the thread pool size.

### 8.3.1 Thread Creation and Teardown
:one: **core pool size**: is the target size; the implementation of thread pool attempts to maintain the pool at this size even when there are no tasks to execute[<sup>1</sup>](#8.3.1.1), and will not create more threads than this unless the work queue is full [<sup>2</sup>](#8.3.1.2).

:two: **Maximum pool size** is the upper bound on how many pool threads can be active at once.

:three: **keep-alive time**: A thread that has been idle for longer than the keep-alive time becomes a candidate for reaping and can be terminated if the current pool size exceeds the core size.

><span id="8.3.1.1" style='color:blue;'>[1]</span> When a ThreadPoolExecutor is initially created, the core threads are not started immediately but instead as tasks are submitted, unless you call prestartAllCoreThreads.
><span id="8.3.1.2" style='color:blue;'>[2]</span> Developers are sometimes tempted to set the core size to zero so that the worker threads will eventually be torn down and therefore won't prevent the JVM from exiting, but this can cause some strange-seeming behavior in thread pools that don't use a **SynchronousQueue** for their work queue (as newCachedThreadPool does). If the pool is already at the core size, **ThreadPoolExecutor** creates a new thread only if the work queue is full. So tasks submitted to a thread pool with a work queue that has any capacity and a core size of zero will not execute until the queue fills up, which is usually not what is desired. In Java 6, **allowCoreThreadTimeOut** allows you to request that all pool threads be able to time out; enable this feature with a core size of zero if you want a bounded thread pool with a bounded work queue but still have all the threads torn down when there is no work to do.

### 8.3.2 Managing Queued Tasks
:sheep: The newCachedThreadPool factory is a good default choice for an Executor, providing better queuing performance than a fixed thread pool [<sup>[1]</sup>](#8.3.2.1). A fixed size thread pool is a good choice when you need to limit the number of concurrent tasks for resource-management purposes, as in a server application that accepts requests from network clients and would otherwise be vulnerable to overload.

> <span id='8.3.2.1' style='color:blue;'>[1]</span>: This performance difference comes from the use of SynchronousQueue instead of LinkedBlocking-Queue. SynchronousQueue was replaced in Java 6 with a new non-blocking algorithm that improved throughput in Executor benchmarks by a factor of three over the Java 5.0 SynchronousQueue implementation.

Bounding either the thread pool or the work queue is suitable only when tasks are independent. With tasks that depend on other tasks, bounded thread pools or queues can cause thread starvation deadlock; instead, use an unbounded pool configuration like newCachedThreadPool.[<sup>[2]</sup>](#8.3.2.2)

> <span id='8.3.2.2' style='color:blue'>[2]</span>: An alternative configuration for tasks that submit other tasks and wait for their results is to use a bounded thread pool, a SynchronousQueue as the work queue, and the caller-runs saturation policy.


### 8.3.3 Saturation Policies 
When a bounded work queue fills up, the saturation policy comes into play. The saturation policy for a ThreadPoolExecutor can be modified by calling _setRejectedExecutionHandler_. (The saturation policy is also used when a task is submitted to an Executor that has been shut down.) Several implementations of _RejectedExecutionHandler_ are provided, each implementing a different saturation policy: **AbortPolicy**, **CallerRunsPolicy**, **DiscardPolicy**, and **DiscardOldestPolicy**.

- The default policy, abort, causes execute to throw the unchecked Rejected-ExecutionException;
- The discard policy silently discards the newly submitted task if it cannot be queued for execution;
- The discard-oldest policy discards the task that would otherwise be executed next and tries to resubmit the new task. (If the work queue is a priority queue, this discards the highest-priority element, so the combination of a discard-oldest saturation policy and a priority queue is not a good one.)
- The caller-runs policy implements a form of throttling that neither discards tasks nor throws an exception, but instead tries to slow down the flow of new tasks by pushing some of the work back to the caller. It executes the newly submitted task not in a pool thread, but in the thread that calls execute.

**There is no predefined saturation policy to make execute block when the work queue is full**. However, the same effect can be accomplished by using a Semaphore to bound the task injection rate, as shown in BoundedExecutor in Listing 8.4. In such an approach, use an unbounded queue (there's no reason to bound both the queue size ant the injection rate) and set the bound on the semaphore to be equal to the pool size plus the number of queued tasks you want to allow, since the semaphore is bounding the number of tasks both currently executing and awaiting execution.

**Listing 8.4 Using a Semaphore to Throttle Task Submission**
```java
@ThreadSafe
public class BoundedExecutor {
     private final Executor exec;
     private final Semaphore semaphore;

     public BoundedExecutor(Executor exec, int bound) {
          this.exec = exec;
          this.semaphore = new Semaphore(bound);
     }

     public void submitTask(Runnable command) {
          semaphore.acquire();
          try {
               exec.execute(() -> {
                    try {
                         command.run();
                    } finally {
                         semaphore.release();
                    }
               })
          } catch (RejectedExecutionException e) {
               semaphore.release();
          }
     }
}
```

### 8.3.4 Thread Factories
Whenever a thread pool needs to create a thread, it does so through a thread factory (see Listing 8.5). The default thread factory creates a new, nondaemon thread with no special configuration. Specifying a thread factory allows you to customize the configuration of pool threads. ThreadFactory has a single method, newThread, that is called whenever a thread pool needs to create a new thread.

**Listing 8.5 ThreadFactory Interface**
```java
public interface ThreadFactory {
     Thread newThread(Runnable r);
}
```

If your application takes advantage of security policies to grant permissions to particular codebases, you may want to use the **PrivilegedThreadFactory** factory method in Executors to construct your thread factory. It creates pool thread that have the same permissions, **AccessControlContext**, and **contextClassLoader** as the thread creating the **privilegedThreadFactory**. Otherwise, threads created by the thread pool inherit permissions from whatever client happens to be calling execute or submit ath the time a new thread is needed, which could cause confusing security-related exceptions.

### 8.3.5 Customizing ThreadPoolExecutor After Construction
- Most of the options passed to the ThreadPoolExecutor constructors can also be modified after construction via setters (such as the core thread pool size, maximum thread pool size, keep-alive time, thread factory, and rejected execution handler).
- If you will be exposing an ExecutorService to code you don't trust not to modify it, you can wrap it by using Executors' unconfigurableExecutorService method which returns an unconfigurable ExecutorService instance.

## 8.4 Extending ThreadPoolExecutor
Hooks that ThreadPoolExecutor provided for subclassed to override:
- protected void beforeExecute(Thread t, Runnable r) { }
- protected void afterExecute(Runnable r, Throwable t) { }
- protected void terminated() { }

## 8.5 Parallelizing Recursive Algorithms

If we have a loop whose iterations are independent and we don't need to wait for all of them to complete before proceeding, we can use an Executor to transform a sequential loop into a parallel one, as shown in processSequentially and processInParallel in Listing 8.10
**Listing 8.10 Transforming Sequential Execution into Parallel Execution**
```java
void processSequentially(List<Element> elements) {
     for (Element e : elements) {
          process(e);
     }
}

void processInParallel(Executor exec, List<Element> elements) {
     for (final Element t : elements) {
          exec.execute(() -> {
               process(e);
          });
     }
}
```

:pill: Sequential loop iterations are suitable for parallelization when each iteration is independent of the others and the work done in each iteration of the loop body is significant enough to offset the cost of managing a new task.

Loop parallelization can also be applied to some recursive designs; there are often sequential loops within the recursive algorithm that can be parallelized in the same manner as Listing 8.10. The easier case is when each iteration does not require the results of the recursive iterations it invokes. For example, sequentialRecursive in List 8.11 does a depth-first traversal of a tree, performing a calculation on each node and placing the result in a collection. The transformed version, parallelRecursive, also does a depth-first traversal, but instead of computing the result as each node is visited, it submits a task to compute the node result.

**Listing 8.11 Transforming Sequential Tail-recursion into Parallelized Recursion**
```java
public <T> void sequentialRecursive(List<Node<T>> nodes, Collection<T> results) {
     for (Node<T> n : nodes) {
          results.add(n.compute());
          sequentialRecursive(n.getChildren(), results);
     }
}

public <T> void parallelRecursive(final Executor exec, List<Node<T>> nodes, final Collection<T> results) {
     for (final Node<T> n : nodes) {
          exec.execute(() -> {
               results.add(n.compete());
          });
          parallelRecursive(exec, n.getChildren(), results);
     }
}
```

When parallelRecursive returns, each node in the tree has been visited (the traversal is still sequential: only the calls to compute are executed in parallel) and the computation for each node has been queued to the Executor. Callers of parallelRecursive can wait for all the results by creating an Executor specific to the traversal and using _shutdown_ and _awaitTermination_, as shown in Listing 8.12

Listing 8.12 Waiting for Results to be Calculated in Parallel.
```java
public <T> Collection<T> getParallelResults(List<Node<T>> nodes) throws InterruptedException {
     ExecutorService exec = Executors.newCachedThreadPool();
     Queue<T> resultQueue = new ConcurrentLinkedQueue<T>();
     parallelRecursive(exec, nodes, resultQueue);
     exec.shutdown();
     exec.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
     return resultQueue;
}
```

:question: Is it a good practice for waiting results by creating a dedicated Executor and using shutdown and awaitTermination? It depends.

### 8.5.1 Example: A Puzzle Framework

**Listing 8.13 Abstraction for Puzzles Like the "Sliding Blocks Puzzle".**
```java
public interface Puzzle<P, M> {
     P initialPosition();
     boolean isGoal(P position);
     Set<M> legalMoves(P position);
     P move(P position, M move);
}
```

**Listing 8.14 Link Node for the Puzzle Solver Framework**
```java
@Immutable
static class Node<P, M> {
     final P pos;
     final M move;
     final Node<P, M> prev;
     Node(P pos, M move, Node<P, M> prev){...}

     List<M> asMoveList() {
          List<M> solution = new LinkedList<M>();
          for (Node<P, M> n = this; n.move != null; n = n.prev) {
               solution.add(0, n.move);
          }
          return solution;
     }
}
```

**Listing 8.15 Sequential Puzzle Solver**
```java
public class SequentialPuzzleSolver<P, M> {
     private final Puzzle<P, M> puzzle;
     private final Set<P> seen = new HashSet<>();

     public SequentialPuzzleSolver(Puzzle<P, M> puzzle) {
          this.puzzle = puzzle;
     }

     public List<M> solve() {
          P pos = puzzle.initialPosition();
          return search(new Node<P, M>(pos, null, null));
     }

     private List<M> search(Node<P, M> node) {
          if (!seen.contains(node.pos)) {
               seen.add(node.pos);
               if(puzzle.isGoal(node.pos)) {
                    return node.asMoveList();
               }

               for (M move : puzzle.legalMoves(node.pos)) {
                    P pos = puzzle.move(node.pos, move);
                    Node<P, M> child = new Node<P, M>(pos, move, node);
                    List<M> result = search(child);
                    if(result != null) {
                         return result;
                    }
               }
          }
          return null;
     }

     static class Node<P, M> { /*Listing 8.14 */}
}
```
