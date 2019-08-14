今天学习启动一个Spring Cloud Server的Maven项目，期间遇到很多问题，查看官方开发手册又看不到解决的办法，所以记录下问题。
<h3>问题一：官方文档描述不清</h3>
我要启动一个Spring Cloud Config Server，但是翻看官方文档，只发现如下的描述：
<pre class="prettyprint">/*
Spring Cloud Config Server provides an HTTP resource-based API for external 
configuration (name-value pairs or equivalent YAML content). The server is embeddable
in a Spring Boot application, by using the @EnableConfigServer annotation. Consequently, 
the following application is a config server:
*/

@SpringBootApplication
@EnableConfigServer
public class ConfigServer {
  public static void main(String[] args) {
    SpringApplication.run(ConfigServer.class, args);
  }
}</pre>
问题在哪里呢？ 根本就没有说pom.xml文件里的dependency怎么配置，原来@EnableConfigServer需要这个包
<pre>import org.springframework.cloud.config.server.<span>EnableConfigServer</span>;</pre>
什么鬼？官网文档根本就没有描述嘛，最后还是在官网的“Guides”页面里面发现了猫腻，原来需要
<pre class="prettyprint">&lt;dependency&gt;
            &lt;groupId&gt;org.springframework.cloud&lt;/groupId&gt;
            &lt;artifactId&gt;spring-cloud-config-server&lt;/artifactId&gt;
&lt;/dependency&gt;</pre>
偷个懒直接把地址贴出来吧：

<a href="https://spring.io/guides/gs/centralized-configuration/">https://spring.io/guides/gs/centralized-configuration/</a>

好了，该解决的依赖都解决了，运行！

What the ****! 报下面的错误：

Caused by: java.lang.ClassNotFoundException: org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType

好继续百度。。。

好吧还是google吧。这里还得感谢 <span style="color: #33cccc;">吃饭不喝汤 <span style="color: #000000;">的<a href="http://www.th7.cn/Program/java/201712/1292886.shtml">文章</a>，下面也就引出了Spring boot的第二个问题</span></span>
<h3>二、SpringBootApplication.java文件不能直接放到main/java文件夹下</h3>
来看一下我的Spring Cloud Config Server的项目结构：

<img src="http://47.93.1.79/wordpress/wp-content/uploads/2018/03/springConfigServer.png" alt="" width="389" height="188" class="aligncenter size-full wp-image-360" />

&nbsp;

注意到有两个SpringConfigServer的java文件，放在包下面的SpringConfigServer可以正常运行，而另一个却报上面说的ClassNotFound的错误。

好了，问题解决。学海无涯，共勉！