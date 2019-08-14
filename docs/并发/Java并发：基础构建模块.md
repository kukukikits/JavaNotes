<h3>1.同步容器类</h3>
包括Vector和Hashtable.

同步容器类是线程安全的，但是当多线程并发的修改容器时，复合操作可能会出现一些问题。

比如：
<pre class="prettyprint">for(int i = 0; i&lt; vector.size(); i++){
    doSomething(vector.get(i));
}</pre>
假如在调用size和get之间，有其他线程在并发地修改vector，那么可能会抛出ArrayIndexOutOfBoundsException的错误。可以对vector进行加锁来解决这个问题，但是性能也会因此降低。