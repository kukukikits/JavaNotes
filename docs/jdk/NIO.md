NIO：New IO，也就是新IO的意思

Java中新IO相关的包如下：
<ul>
 	<li>java.io：主要包含各种与Buffer相关的类</li>
 	<li>java.nio.channels：主要包含与Channel和Selector相关的类</li>
 	<li>java.nio.charset：主要包含与字符集相关的类</li>
 	<li>java.nio.channels.spi：主要包含与Channel相关的服务提供者编程接口</li>
 	<li>java.nio.charset.spi：主要包含字符集相关的服务提供者编程接口</li>
</ul>
NIO中的两个核心概念和对象：
<ul>
 	<li>Channel：通道，是对传统的输入/输出系统的模拟</li>
 	<li>Buffer：缓冲，数据的读取必须经过Buffer</li>
</ul>
<h3>一、使用Buffer</h3>
Buffer是一个抽象类，可供我们直接使用的Buffer有：CharBuffer、ShortBuffer、IntBuffer、LongBuffer、FloatBuffer、DoubleBuffer。注意StringBuffer和这里的这些不是一个东西。