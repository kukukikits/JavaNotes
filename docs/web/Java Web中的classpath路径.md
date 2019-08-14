以下内容摘自http://blog.csdn.net/u011095110/article/details/76152952
<h3 style="box-sizing: border-box; font-family: 'PingFang SC', 'Microsoft YaHei', SimHei, Arial, SimSun; font-weight: 400; line-height: 1.1; color: #454545; margin: 0px; font-size: 24px; padding: 0px; background-color: #ffffff;"><span style="box-sizing: border-box; margin: 0px; padding: 0px; color: #ff0000;">一、classpath路径指什么</span></h3>
<span style="color: #454545; font-family: 'PingFang SC', 'Microsoft YaHei', SimHei, Arial, SimSun; font-size: 16px; line-height: 24px; background-color: #ffffff;">    只知道把配置文件如：mybatis.xml、spring-web.xml、applicationContext.xml等放到src目录（就是存放代码.java文件的目录），然后使用“classpath：xxx.xml”来读取，都放到src目录准没错，那么到底classpath到底指的什么位置呢？</span>
<ol start="1" class="dp-j">
 	<li class="alt"><span>src路径下的文件在编译后会放到WEB-INF/clases路径下吧。默认的classpath是在这里。直接放到WEB-INF下的话，是不在classpath下的。用ClassPathXmlApplicationContext当然获取不到。  </span></li>
 	<li class=""><span>如果单元测试的话，可以在启动或者运行的选项里指定classpath的路径的。  </span></li>
 	<li class="alt"><span><span>用maven构建项目时候resource目录就是默认的classpath  </span></span></li>
 	<li class=""><span>classPath即为java文件编译之后的<span class="keyword">class</span><span>文件的编译目录一般为web-inf/classes，src下的xml在编译时也会复制到classPath下  </span></span></li>
 	<li class="alt"><span>ApplicationContext ctx = <span class="keyword">new</span><span> ClassPathXmlApplicationContext(</span><span class="string">"xxxx.xml"</span><span>);  </span><span class="comment">//读取classPath下的spring.xml配置文件</span><span>  </span></span></li>
 	<li class=""><span>ApplicationContext ctx = <span class="keyword">new</span><span> FileSystemXmlApplicationContext(</span><span class="string">"WebRoot/WEB-INF/xxxx.xml"</span><span>);   </span><span class="comment">//读取WEB-INF 下的spring.xml文件</span><span>  </span></span></li>
</ol>
<h3><span></span></h3>
<h3><span style="color: #ff0000;">二、web.xml 配置中classpath: 与classpath*:的区别</span></h3>
<span>首先 classpath是指 WEB-INF文件夹下的classes目录 </span>

<span>解释classes含义：   </span>
<ol>
 	<li><span>存放各种资源配置文件 eg.init.properties log4j.properties struts.xml   </span></li>
 	<li><span>存放模板文件 eg.actionerror.ftl   </span></li>
 	<li><span>存放<span class="keyword">class</span>文件 对应的是项目开发时的src目录编译文件   </span></li>
 	<li><span>总结：这是一个定位资源的入口  </span><span> </span></li>
</ol>
<span>classpath 和 classpath* 区别： </span>
<span>classpath：只会到你的class路径中查找找文件; </span>
<span>classpath*：不仅包含class路径，还包括jar文件中(class路径)进行查找. </span>
<pre class="prettyprint"><span>Java代码  收藏代码</span>
&lt;param-value&gt;classpath:applicationContext-*.xml&lt;/param-value&gt;</pre>
<span></span>
<span>或者引用其子目录下的文件,如 </span>
<span><!--?prettify linenums=true?--></span>
<pre class="prettyprint">Java代码  收藏代码
&lt;param-value&gt;classpath:context/conf/controller.xml&lt;/param-value&gt;</pre>
<span> </span>
<span>classpath*的使用：当项目中有多个classpath路径，并同时加载多个classpath路径下（此种情况多数不会遇到）的文件，*就发挥了作用，如果不加*，则表示仅仅加载第一个classpath路径，代码片段： </span>
<!--?prettify linenums=true?-->
<pre class="prettyprint">Java代码  收藏代码
&lt;param-value&gt;classpath*:context/conf/controller*.xml&lt;/param-value&gt;</pre>
<span>另外： </span>
<span>"**/" 表示的是任意目录； </span>
<span>"**/applicationContext-*.xml"  表示任意目录下的以"applicationContext-"开头的XML文件。  </span>
<span>程序部署到tomcat后，src目录下的配置文件会和class文件一样，自动copy到应用的 WEB-INF/classes目录下 </span>
<span>classpath:与classpath*:的区别在于， </span>

<span>前者只会从第一个classpath中加载，而 </span>
<span>后者会从所有的classpath中加载  </span>


<span>如果要加载的资源， </span>
<span>不在当前ClassLoader的路径里，那么用classpath:前缀是找不到的， </span>
<span>这种情况下就需要使用classpath*:前缀 </span>


<span>在多个classpath中存在同名资源，都需要加载， </span>
<span>那么用classpath:只会加载第一个，这种情况下也需要用classpath*:前缀 </span>


<span>注意： </span>
<span>用classpath*:需要遍历所有的classpath，所以加载速度是很慢的，因此，在规划的时候，应该尽可能规划好资源文件所在的路径，尽量避免使用 classpath*</span>