<article>
<div id="article_content" class="article_content csdn-tracking-statistics" data-mod="popu_519" data-dsm="post">

转载<a href="http://blog.csdn.net/walkerjong/article/details/50913754">http://blog.csdn.net/walkerjong/article/details/50913754</a>

1. Private Constructor capture idiom<span> </span>
<div class="content">

java puzzler 53

The following code fails to compile, tips: Cannot refer to an instance field arg while explicitly invoking a constructor
<pre>package arkblue.lang.javapuzzler.n53;

class Thing {
        public Thing(int i) {

        }
}

public class MyThing extends Thing {
        private final int arg;

        public MyThing() {
                super(arg = Math.round(12L)); // Compilation failed
        }

}
</pre>
Suppose Thing is a library class, only a constructor argument, did not provide any access device, you do not have permission to access internal and therefore can not modify it.

At this time, want to write a subclass, the constructor through the bar with SomtOtherClass.func () method to calculate the super class constructor parameter. Return value of this method call can return again and again different values, you want to pass this value is stored in the parent class of a final sub-class instance. So with the above code, but can not compile.

Modify
<pre>class SomeOtherClass {
        static int func() {
                return Math.round(12L);
        }
}

public class MyThing extends Thing {
        private final int arg;

        public MyThing() {
                this(SomeOtherClass.func());
        }

        private MyThing(int i) {
                super(i);
                arg = i;
        }
}
</pre>
The program uses the alternate constructor invocation mechanism (alternate constructor invocation)

In the private constructor, the expression SomeOtherClass.func () the value has been captured in the variable i, and it can be returned after the superclass constructor to store the final type of domain arg.

</div>
source:<span> </span><a href="http://www.quweiji.com/capture-the-private-constructor-private-constructor-capture/">http://www.quweiji.com/capture-the-private-constructor-private-constructor-capture/</a>

2.<a href="http://stackoverflow.com/questions/12028925/private-constructor-to-avoid-race-condition" class="question-hyperlink"><span> </span>Private constructor to avoid race condition</a>
<table>
<tbody>
<tr>
<td class="votecell">
<div class="vote"><span class="vote-count-post">27</span><span> </span><a class="vote-down-off" title="This question does not show any research effort; it is unclear or not useful">down vote</a><a class="star-off" href="http://stackoverflow.com/questions/12028925/private-constructor-to-avoid-race-condition#" title="This is a favorite question (click again to undo)">favorite</a>
<div class="favoritecount"><strong>13</strong></div>
</div></td>
<td class="postcell">
<div>
<div class="post-text">

I am reading the book<span> </span><code>Java Concurrency in Practice</code><span> </span>session 4.3.5
<pre><code>  @ThreadSafe
  public class SafePoint{

       @GuardedBy("this") private int x,y;

       private SafePoint (int [] a) { this (a[0], a[1]); }

       public SafePoint(SafePoint p) { this (p.get()); }

       public SafePoint(int x, int y){
            this.x = x;
            this.y = y;
       }

       public synchronized int[] get(){
            return new int[] {x,y};
       }

       public synchronized void set(int x, int y){
            this.x = x;
            this.y = y;
       }

  }
</code></pre>
I am not clear where It says
<blockquote>The private constructor exists to avoid the race condition that would occur if the copy constructor were implemented as this (p.x, p.y); this is an example of the private constructor capture idiom (Bloch and Gafter, 2005).</blockquote>
I understand that it provides a getter to retrieve both x and y at once in a array instead of a separate getter for each, so the caller will see consistent value, but why private constructor ? what's the trick here

</div>
<div class="post-taglist"><a href="http://stackoverflow.com/questions/tagged/java" class="post-tag" title="show questions tagged 'java'">java</a><span> </span><a href="http://stackoverflow.com/questions/tagged/multithreading" class="post-tag" title="show questions tagged 'multithreading'">multithreading</a><span> </span><a href="http://stackoverflow.com/questions/tagged/race-condition" class="post-tag" title="show questions tagged 'race-condition'">race-condition</a></div>
<table class="fw">
<tbody>
<tr>
<td class="vt">
<div class="post-menu"><a href="http://stackoverflow.com/q/12028925" title="short permalink to this question" class="short-link" id="link-post-12028925">share</a><span class="lsep"></span><a href="http://stackoverflow.com/posts/12028925/edit" class="suggest-edit-post" title="">improve this question</a></div></td>
<td class="post-signature" align="right">
<div class="user-info">
<div class="user-action-time"><a href="http://stackoverflow.com/posts/12028925/revisions" title="show all edits to this post">edited<span> </span><span title="2015-06-01 16:16:27Z" class="relativetime">Jun 1 '15 at 16:16</span></a></div>
<div class="user-gravatar32"></div>
<div class="user-details">
<div class="-flair"></div>
</div>
</div></td>
<td class="post-signature owner">
<div class="user-info">
<div class="user-action-time">asked<span> </span><span title="2012-08-19 18:31:06Z" class="relativetime">Aug 19 '12 at 18:31</span></div>
<div class="user-gravatar32"><a href="http://stackoverflow.com/users/1389813/peter"></a>
<div class="gravatar-wrapper-32"><img src="https://www.gravatar.com/avatar/8b7cace6620ed08e8f2f723671537ec4?s=32&amp;d=identicon&amp;r=PG" alt="" height="32" width="32" /></div>
</div>
<div class="user-details"><a href="http://stackoverflow.com/users/1389813/peter">peter</a>
<div class="-flair"><span class="reputation-score" title="reputation score" dir="ltr">2,084</span><span title="4 gold badges"><span class="badge1"></span><span class="badgecount">4</span></span><span title="27 silver badges"><span class="badge2"></span><span class="badgecount">27</span></span><span title="60 bronze badges"><span class="badge3"></span><span class="badgecount">60</span></span></div>
</div>
</div></td>
</tr>
</tbody>
</table>
</div></td>
</tr>
<tr>
<td class="votecell"></td>
<td>
<div id="comments-12028925" class="comments">
<table>
<tbody>
<tr id="comment-16054348" class="comment">
<td class="comment-actions">
<table>
<tbody>
<tr>
<td class="comment-score"><span title="number of 'useful comment' votes received" class="cool">4</span></td>
<td></td>
</tr>
</tbody>
</table>
</td>
<td class="comment-text">
<div class="comment-body"><span class="comment-copy">It's only<span> </span><code>private</code><span> </span>because they don't want other people to use it ;-)</span><span> </span>– <a href="http://stackoverflow.com/users/591495/oldrinb" title="15367 reputation" class="comment-user">oldrinb</a><span> </span><span class="comment-date" dir="ltr"><span title="2012-08-19 18:38:25Z" class="relativetime-clean">Aug 19 '12 at 18:38</span></span></div></td>
</tr>
<tr id="comment-16055209" class="comment">
<td>
<table>
<tbody>
<tr>
<td class="comment-score"></td>
<td></td>
</tr>
</tbody>
</table>
</td>
<td class="comment-text">
<div class="comment-body"><span class="comment-copy">I have updated the answer )</span><span> </span>– <a href="http://stackoverflow.com/users/241986/boris-treukhov" title="8083 reputation" class="comment-user">Boris Treukhov</a><span> </span><span class="comment-date" dir="ltr"><span title="2012-08-19 19:52:06Z" class="relativetime-clean">Aug 19 '12 at 19:52</span></span></div></td>
</tr>
<tr id="comment-16069044" class="comment">
<td>
<table>
<tbody>
<tr>
<td class="comment-score"></td>
<td></td>
</tr>
</tbody>
</table>
</td>
<td class="comment-text">
<div class="comment-body"><span class="comment-copy">@user1389813 you should notice that this pattern could be easily avoided if the constructor would be refactored to a method for example. See my answer also ;)</span><span> </span>– <a href="http://stackoverflow.com/users/1059372/eugene" title="8092 reputation" class="comment-user">Eugene</a><span> </span><span class="comment-date" dir="ltr"><span title="2012-08-20 12:42:07Z" class="relativetime-clean">Aug 20 '12 at 12:42</span></span></div></td>
</tr>
</tbody>
</table>
</div>
<div id="comments-link-12028925"><a class="js-add-link comments-link disabled-link" title="Use comments to ask for more information or suggest improvements. Avoid answering questions in comments.">add a comment</a></div></td>
</tr>
</tbody>
</table>
<div id="answers"><a name="tab-top"></a>
<div id="answers-header">
<div class="subheader answers-subheader">
<h2>7 Answers</h2>
<div>
<div id="tabs"><a href="http://stackoverflow.com/questions/12028925/private-constructor-to-avoid-race-condition?answertab=active#tab-top" title="Answers with the latest activity first">active</a><span> </span><a href="http://stackoverflow.com/questions/12028925/private-constructor-to-avoid-race-condition?answertab=oldest#tab-top" title="Answers in the order they were provided">oldest</a><span> </span><a class="youarehere" href="http://stackoverflow.com/questions/12028925/private-constructor-to-avoid-race-condition?answertab=votes#tab-top" title="Answers with the highest score first">votes</a></div>
</div>
</div>
</div>
<a></a>
<div id="answer-12037506" class="answer accepted-answer">
<table>
<tbody>
<tr>
<td class="votecell">
<div class="vote"><a class="vote-up-off" title="This answer is useful">up vote</a><span class="vote-count-post">19</span><span> </span><a class="vote-down-off" title="This answer is not useful">down vote</a><span class="vote-accepted-on load-accepted-answer-date" title="loading when this answer was accepted...">accepted</span></div></td>
<td class="answercell">
<div class="post-text">

There are already a bunch of answers here, but I would really like to dive into some details (as much as my knowledge let's me). I will strongly advise you to run each sample that is present here in the answer to see for yourself how things are happening and why.

<em>To understand the solution, you need to understand the problem first.</em>

Suppose that the SafePoint class actually looks like this:
<pre><code>class SafePoint {
    private int x;
    private int y;

    public SafePoint(int x, int y){
        this.x = x;
        this.y = y;
    }

    public SafePoint(SafePoint safePoint){
        this(safePoint.x, safePoint.y);
    }

    public synchronized int[] getXY(){
        return new int[]{x,y};
    }

    public synchronized void setXY(int x, int y){
        this.x = x;
        //Simulate some resource intensive work that starts EXACTLY at this point, causing a small delay
        try {
            Thread.sleep(10 * 100);
        } catch (InterruptedException e) {
         e.printStackTrace();
        }
        this.y = y;
    }

    public String toString(){
      return Objects.toStringHelper(this.getClass()).add("X", x).add("Y", y).toString();
    }
}
</code></pre>
What variables create the state of this object? Just two of them : x,y. Are they protected by some synchronization mechanism? Well they are by the intrinsic lock, through the synchronized keyword - at least in the setters and getters. Are they 'touched' anywhere else? Of course here:
<pre><code>public SafePoint(SafePoint safePoint){
    this(safePoint.x, safePoint.y);
} 
</code></pre>
What you are doing here is<span> </span><em>reading</em><span> </span>from your object. For a class to be Thread safe, you have to coordinate read/write access to it, or<span> </span><em>synchronize on the same lock.</em><span> </span>But there is no such thing happening here. The<span> </span><strong>setXY</strong><span> </span>method is indeed synchronized, but the clone constructor is not, thus calling these two can be done in a non thread-safe way. Can we brake this class?

Let's try this out:
<pre><code>public class SafePointMain {
public static void main(String[] args) throws Exception {
    final SafePoint originalSafePoint = new SafePoint(1,1);

    //One Thread is trying to change this SafePoint
    new Thread(new Runnable() {
        @Override
        public void run() {
            originalSafePoint.setXY(2, 2);
            System.out.println("Original : " + originalSafePoint.toString());
        }
    }).start();

    //The other Thread is trying to create a copy. The copy, depending on the JVM, MUST be either (1,1) or (2,2)
    //depending on which Thread starts first, but it can not be (1,2) or (2,1) for example.
    new Thread(new Runnable() {
        @Override
        public void run() {
            SafePoint copySafePoint = new SafePoint(originalSafePoint);
            System.out.println("Copy : " + copySafePoint.toString());
        }
    }).start();
}
}
</code></pre>
The output is easily this one:
<pre><code> Copy : SafePoint{X=2, Y=1}
 Original : SafePoint{X=2, Y=2} 
</code></pre>
This is logic, because one Thread updates=writes to our object and the other is reading from it. They do not synchronize on some common lock, thus the output.

<em>Solution?</em>
<ul>
 	<li>synchronized constructor so that the read will synchronize on the same lock, but Constructors in Java can not use the synchronized keyword - which is logic of course.</li>
 	<li>may be use a different lock, like Reentrant lock (if the synchronized keyword can not be used). But it will also not work, because<span> </span><em>the first statement inside a constructor must be a call to this/super</em>. If we implement a different lock then the first line would have to be something like this:

lock.lock() //where lock is ReentrantLock, the compiler is not going to allow this for the reason stated above.</li>
 	<li>what if we make the constructor a method? Of course this will work!</li>
</ul>
See this code for example
<pre><code>/*
 * this is a refactored method, instead of a constructor
 */
public SafePoint cloneSafePoint(SafePoint originalSafePoint){
     int [] xy = originalSafePoint.getXY();
     return new SafePoint(xy[0], xy[1]);    
}
</code></pre>
And the call would look like this:
<pre><code> public void run() {
      SafePoint copySafePoint = originalSafePoint.cloneSafePoint(originalSafePoint);
      //SafePoint copySafePoint = new SafePoint(originalSafePoint);
      System.out.println("Copy : " + copySafePoint.toString());
 }
</code></pre>
This time the code runs as expected, because the read and the write are synchronized on the same lock, but<span> </span><strong>we have dropped the constructor</strong>. What if this were not allowed?

We need to find a way to read and write to SafePoint synchronized on the same lock.

Ideally we would want something like this:
<pre><code> public SafePoint(SafePoint safePoint){
     int [] xy = safePoint.getXY();
     this(xy[0], xy[1]);
 }
</code></pre>
But the compiler does not allow this.

We can read safely by invoking the *<em>getXY</em><span> </span>method, so we need a way to use that, but we do not have a constructor that takes such an argument thus - create one.
<pre><code>private SafePoint(int [] xy){
    this(xy[0], xy[1]);
}
</code></pre>
And then, the actual invokation:
<pre><code>public  SafePoint (SafePoint safePoint){
    this(safePoint.getXY());
}
</code></pre>
Notice that the constructor is private, this is because we do not want to expose yet another public constructor and think<span> </span><em>again</em><span> </span>about the invariants of the class, thus we make it private - and only we can invoke it.<span> </span>

</div></td>
</tr>
</tbody>
</table>
</div>
</div>
</div>
</article>