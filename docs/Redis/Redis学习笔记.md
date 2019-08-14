<ol>
 	<li style="list-style-type: none">
<ol>
 	<li>存key-value: SET key "value"</li>
 	<li>取：GET key</li>
 	<li>当不存在时才存  SETNX key "value". <span>SETNX actually means "SET if Not eXists".</span></li>
 	<li>INCR key/INCRBY key integer. <span>Increment</span><span> the number stored at key by one</span></li>
 	<li>DECR key/DECRBY key integer.</li>
 	<li>EXPIRE resource 120 设置resource在120s后失效</li>
 	<li>TTL resource 100 resource剩余的时间为100s。当使用SET重置resource的值后，resource的过期状态会被清除</li>
 	<li>操作链表<!--?prettify linenums=true?-->
<pre class="prettyprint">RPUSH puts the new value at the end of the list：

    RPUSH friends "Alice"
    RPUSH friends "Bob"

LPUSH puts the new value at the start of the list：

    LPUSH friends "Sam"

LRANGE key startIndex endIndex. 截取链表数据。-1表示截取数据到链表末位

     LRANGE friends 0 -1 =&gt; 1) "Sam", 2) "Alice", 3) "Bob"
     LRANGE friends 0 1 =&gt; 1) "Sam", 2) "Alice"

LLEN returns the current length of the list.

    LLEN friends =&gt; 3

LPOP removes the first element from the list and returns it.

    LPOP friends =&gt; "Sam"

RPOP removes the last element from the list and returns it.

    RPOP friends =&gt; "Bob"

Note that the list now only has one element:

    LLEN friends =&gt; 1
    LRANGE friends 0 -1 =&gt; 1) "Alice"</pre>
</li>
 	<li>操作无序的set集合<!--?prettify linenums=true?-->
<pre class="prettyprint">SADD 向set集合插入数据：

    SADD superpowers "flight"
    SADD superpowers "x-ray vision"
    SADD superpowers "reflexes"

SREM 从set集合移除数据：

    SREM superpowers "reflexes"

SISMEMBER setName value. 测试value值存不存在：
    
    SISMEMBER superpowers "flight" =&gt; 1  表示存在
    SISMEMBER superpowers "reflexes" =&gt; 0  表示不存在

SMEMBERS setName. 返回所有setName的成员变量：

    SMEMBERS superpowers =&gt; 1) "flight", 2) "x-ray vision"

SUNION setName1 setName2. 联合两个或者更多的set集合：

    SADD birdpowers "pecking"
    SADD birdpowers "flight"
    SUNION superpowers birdpowers =&gt; 1) "pecking", 2) "x-ray vision", 3) "flight"</pre>
</li>
 	<li>操作有序set集合@since <span>Redis 1.2</span><!--?prettify linenums=true?-->
<pre class="prettyprint">ZADD setName score value. 为了给值排序需要score值

    ZADD hackers 1940 "Alan Kay"
    ZADD hackers 1906 "Grace Hopper"
    ZADD hackers 1953 "Richard Stallman"

ZRANGE setName startIndex endIndex.提取子集合

    ZRANGE hackers 2 4 =&gt; 1) "Claude Shannon", 2) "Alan Kay", 3) "Richard Stallman"</pre>
</li>
 	<li>使用Hash<!--?prettify linenums=true?-->
<pre class="prettyprint">HSET hashName field value.例如
    HSET user name "John Smith"
    HSET user email "jhon.smith@example.com"

To get back the saved data use HGETALL:
    HGETALL user

You can also set multiple fields at once:
    HMSET user name "Mary" password "123446"

If you only need a single field value that is possible as well:
    HGET user name =&gt; "Mary"

Operations to increment numerical values in an atomic way.
    HSET user visites 10
    HINCRBY user visites 1 =&gt; 11
    HINCRBY user visites 10 =&gt; 21
    HDEL user visites
    HINCRBY user visites 1 =&gt; 1</pre>
</li>
</ol>
</li>
</ol>