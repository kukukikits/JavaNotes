<div>

格式定义：
<pre class="hljs xml"><code class="xml"><span class="hljs-tag">&lt;<span class="hljs-name">context-param</span>&gt;</span>  
    <span class="hljs-tag">&lt;<span class="hljs-name">param-name</span>&gt;</span>contextConfigLocation<span class="hljs-tag">&lt;/<span class="hljs-name">param-name</span>&gt;</span>  
    <span class="hljs-tag">&lt;<span class="hljs-name">param-value</span>&gt;</span>contextConfigLocationValue&gt;<span class="hljs-tag">&lt;/<span class="hljs-name">param-value</span>&gt;</span>  
<span class="hljs-tag">&lt;/<span class="hljs-name">context-param</span>&gt;</span>  
</code></pre>
作用：该元素用来声明应用范围(整个WEB项目)内的上下文初始化参数。
<code>param-name</code>设定上下文的参数名称。必须是唯一名称
<code>param-value</code> 设定的参数名称的值
<h2>初始化过程：</h2>
在启动Web项目时，容器(比如Tomcat)会读<code>web.xml</code>配置文件中的两个节点<code>&lt;listener&gt;</code>和<code>&lt;contex-param&gt;</code>。
接着容器会创建一个<code>ServletContext</code>(上下文),应用范围内即整个WEB项目都能使用这个上下文。
接着容器会将读取到<code>&lt;context-param&gt;</code>转化为键值对,并交给<code>ServletContext</code>。
容器创建<code>&lt;listener&gt;&lt;/listener&gt;</code>中的类实例,即创建监听（备注：listener定义的类可以是自定义的类但必须需要继承<code>ServletContextListener</code>）。
在监听的类中会有一个<code>contextInitialized(ServletContextEvent event)</code>初始化方法，在这个方法中可以通过<code>event.getServletContext().getInitParameter("contextConfigLocation")</code>来得到<code>context-param</code>设定的值。在这个类中还必须有一个<code>contextDestroyed(ServletContextEvent event)</code>销毁方法.用于关闭应用前释放资源，比如说数据库连接的关闭。
得到这个<code>context-param</code>的值之后,你就可以做一些操作了.注意,这个时候你的WEB项目还没有完全启动完成.这个动作会比所有的Servlet都要早。
<strong>由上面的初始化过程可知容器对于web.xml的加载过程是<code>context-param &gt;&gt; listener &gt;&gt; fileter &gt;&gt; servlet**</code>
如何使用
页面中<code>${initParam.contextConfigLocation}</code>
Servlet中</strong><code>String paramValue=getServletContext().getInitParameter("contextConfigLocation")</code>**

</div>
<pre>转载自：

作者：lovePython
链接：https://www.jianshu.com/p/437ef95e8e1b
來源：简书</pre>