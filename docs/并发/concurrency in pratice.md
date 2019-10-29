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