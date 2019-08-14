对于一个初学者来说，使用eclipse进行开发是一件非常苦恼的事情，因为需要配置各种库，使用各种插件，而且一般工程给的例子也不是基于eclipse的，反正有各种各样的坑等着你踩。

今天使用gradle构建工具搭建struts2项目，遇到了各种坑，就此做个笔记。

<!--more-->
<h6>1. 在eclipse中安装gradle插件</h6>
点击菜单栏Help&gt;Eclipse Marketplace，在弹出的应用市场中搜“Gradle”，安装如下的插件：

<img class="aligncenter size-full wp-image-117" src="http://47.93.1.79/wordpress/wp-content/uploads/2017/09/QQ截图20170908170316.png" alt="" width="478" height="186" />

安装完成后可以调出Gradle的两个工具“Gradle Tasks”和“Gradle Executions”。打开方式为：菜单栏Window&gt;show view&gt;other，在弹出的对话框中找到这两个工具，选择并打开，最终效果如下：

<img class="aligncenter size-full wp-image-118" src="http://47.93.1.79/wordpress/wp-content/uploads/2017/09/QQ截图20170908170656.png" alt="" width="627" height="187" />
<h6>2. 新建gradle工程</h6>
File&gt;New&gt;Project，在弹出的对话框中找到Gradle Project选择并新建工程。新建的gradle工程目录结构如下：

[caption id="attachment_119" align="aligncenter" width="289"]<img class="size-full wp-image-119" src="http://47.93.1.79/wordpress/wp-content/uploads/2017/09/QQ截图20170908171008.png" alt="gradle工程目录结构" width="289" height="219" /> gradle工程目录结构[/caption]
<h6>3. 配置Struts2之前的准备</h6>
首先将gradle工程转换为动态网页工程，右键工程&gt;Properties&gt;Project Facets&gt;Convert to faceted form。然后在弹出的界面中进行设置，即勾选Dynamic Web Module，在Further configuration avaiable...中进行设置，勾选复选框以生成web.xml文件。最终设置内容如下：

<img class="aligncenter size-full wp-image-120" src="http://47.93.1.79/wordpress/wp-content/uploads/2017/09/QQ截图20170908171827.png" alt="" width="1081" height="566" />

设置完成后，应用新的设置。但是有时候会弹出“Failed whild installing Dynamic Web Module 3.0”的错误，如下：

<img class="aligncenter size-full wp-image-121" src="http://47.93.1.79/wordpress/wp-content/uploads/2017/09/QQ截图20170908172120.png" alt="" width="548" height="202" />

解决该问题的办法是重新配置JRE Library。项目右键&gt;Build Path&gt;Configure Build Path。然后在Libraries中重新设置JRE System Library：

点击Add Library，在弹出的对话框中选择“JRE System Library”，Next，然后配置合适的java环境。配置好后重新将项目转换为Dynamic Web Module项目。转换成功后项目目录，发现目录结构变了：

<img class="aligncenter wp-image-122" src="http://47.93.1.79/wordpress/wp-content/uploads/2017/09/QQ截图20170908172854.png" alt="" width="292" height="225" />
<h6>4. 配置Struts2</h6>
在build.gradle文件中加入项目依赖。可以先在网上的Maven库中找到Struts2的引用方法，在这个网站中找https://mvnrepository.com/

找到引用方法后，复制到项目依赖中：
<pre><code>dependencies {
//....别的依赖
compile group: 'org.apache.struts', name: 'struts2-core', version: '2.5.8'</code> <code>//https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core
compile group: 'org.apache.logging.log4j', name: 'log4j-core', version: '2.8.2'
</code> <code>//https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-api
compile group: 'org.apache.logging.log4j', name: 'log4j-api', version: '2.8.2'
}
</code></pre>
在依赖中还需要加入两个log4j的包，用来在控制台输出调试内容。

最后右键项目&gt;Gradle&gt;Refresh Gradle Project，更新项目库，Gradle会把项目依赖自动下载到项目中。
<h6>5. 构建Struts2</h6>
好了，现在可以按照Struts2官网上的例子进行设置了。

<strong>Step1:</strong> 在之前的设置中，我们把web.xml文件创建在了src/main/webapp/WEB-INF目录下，那么我们网站的静态文件存放的目录就是src/main/webapp，就是我们在之前设置Dynamic Web Module时设置的“Content directory”，我们在src/main/webapp下新建index.jsp文件，文件内容如下：
<pre><code class="language-jsp">&lt;!DOCTYPE html&gt;
&lt;%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %&gt;
&lt;html&gt;
  &lt;head&gt;
    &lt;meta charset="UTF-8"&gt;
    &lt;title&gt;Basic Struts 2 Application - Welcome&lt;/title&gt;
  &lt;/head&gt;
  &lt;body&gt;
    &lt;h1&gt;Welcome To Struts 2!&lt;/h1&gt;
  &lt;/body&gt;
&lt;/html&gt;
</code></pre>
<strong>Step2</strong>：在src/main/resources目录下创建<strong>log4j2.xml</strong>文件，一般是没有resources这个目录的，请自己新建一个文件夹。内容如下：
<pre class="highlight"><code><span class="cp">&lt;?xml version="1.0" encoding="UTF-8"?&gt;</span>
<span class="nt">&lt;Configuration&gt;</span>
    <span class="nt">&lt;Appenders&gt;</span>
        <span class="nt">&lt;Console</span> <span class="na">name=</span><span class="s">"STDOUT"</span> <span class="na">target=</span><span class="s">"SYSTEM_OUT"</span><span class="nt">&gt;</span>
            <span class="nt">&lt;PatternLayout</span> <span class="na">pattern=</span><span class="s">"%d %-5p [%t] %C{2} (%F:%L) - %m%n"</span><span class="nt">/&gt;</span>
        <span class="nt">&lt;/Console&gt;</span>
    <span class="nt">&lt;/Appenders&gt;</span>
    <span class="nt">&lt;Loggers&gt;</span>
        <span class="nt">&lt;Logger</span> <span class="na">name=</span><span class="s">"com.opensymphony.xwork2"</span> <span class="na">level=</span><span class="s">"debug"</span><span class="nt">/&gt;</span>
        <span class="nt">&lt;Logger</span> <span class="na">name=</span><span class="s">"org.apache.struts2"</span> <span class="na">level=</span><span class="s">"debug"</span><span class="nt">/&gt;</span>
        <span class="nt">&lt;Root</span> <span class="na">level=</span><span class="s">"warn"</span><span class="nt">&gt;</span>
            <span class="nt">&lt;AppenderRef</span> <span class="na">ref=</span><span class="s">"STDOUT"</span><span class="nt">/&gt;</span>
        <span class="nt">&lt;/Root&gt;</span>
    <span class="nt">&lt;/Loggers&gt;</span>
<span class="nt">&lt;/Configuration&gt;</span>
</code></pre>
<strong>Step3</strong>:添加Filter。在web.xml文件中加入Filter，最后效果如下：
<pre class="highlight"><code><span class="nt">&lt;?xml version="1.0" encoding="UTF-8"?&gt;
&lt;web-app id="WebApp_ID" version="3.0" xmlns="http://java.sun.com/xml/ns/javaee" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd"&gt;
</span>
<span class="nt">&lt;filter&gt;</span>
    <span class="nt">&lt;filter-name&gt;</span>struts2<span class="nt">&lt;/filter-name&gt;</span>
    <span class="nt">&lt;filter-class&gt;</span>org.apache.struts2.dispatcher.filter.StrutsPrepareAndExecuteFilter<span class="nt">&lt;/filter-class&gt;</span>
<span class="nt">&lt;/filter&gt;</span>

<span class="nt">&lt;filter-mapping&gt;</span>
    <span class="nt">&lt;filter-name&gt;</span>struts2<span class="nt">&lt;/filter-name&gt;</span>
    <span class="nt">&lt;url-pattern&gt;</span>/*<span class="nt">&lt;/url-pattern&gt;</span>
<span class="nt">&lt;/filter-mapping&gt;</span>
<span class="nt">&lt;/web-app&gt;</span>
</code></pre>
<strong>Step4</strong>:在src/main/resources目录下新建struts.xml文件，内容如下：
<pre class="highlight"><code><span class="cp">&lt;?xml version="1.0" encoding="UTF-8"?&gt;</span>
<span class="cp">&lt;!DOCTYPE struts PUBLIC
    "-//Apache Software Foundation//DTD Struts Configuration 2.5//EN"
    "http://struts.apache.org/dtds/struts-2.5.dtd"&gt;</span>

<span class="nt">&lt;struts&gt;</span>

    <span class="nt">&lt;constant</span> <span class="na">name=</span><span class="s">"struts.devMode"</span> <span class="na">value=</span><span class="s">"true"</span> <span class="nt">/&gt;</span>

    <span class="nt">&lt;package</span> <span class="na">name=</span><span class="s">"basicstruts2"</span> <span class="na">extends=</span><span class="s">"struts-default"</span><span class="nt">&gt;</span>
        <span class="nt">&lt;action</span> <span class="na">name=</span><span class="s">"index"</span><span class="nt">&gt;</span>
            <span class="nt">&lt;result&gt;</span>/index.jsp<span class="nt">&lt;/result&gt;</span>
        <span class="nt">&lt;/action&gt;</span>
    <span class="nt">&lt;/package&gt;</span>

<span class="nt">&lt;/struts&gt;</span>
</code></pre>
<strong>Step5</strong>:运行Gradle Tasks工具下的“init”和“build”两个任务

<img class="aligncenter size-full wp-image-125" src="http://47.93.1.79/wordpress/wp-content/uploads/2017/09/无标题.png" alt="" width="596" height="340" />

<strong>Step6</strong>:此时还不能直接，右击项目文件，run as&gt; Run on Server，因为tomcat默认从WEB-INF/lib目录下加载依赖库，而我们的gradle工程依赖的库全在“Project and External Dependencies”下，若直接在服务器上运行工程会出现

java.lang.ClassNotFoundException

找不到类的现象。解决方法一：将所有项目依赖的jar包复制粘贴到WEB-INF/lib目录下。解决方法二：右键项目&gt;Properties&gt;Deployment Assembly，然后单击add，选择Java Build Path Entries，next，勾选项目的依赖库，完成设置：

<img class="aligncenter size-full wp-image-127" src="http://47.93.1.79/wordpress/wp-content/uploads/2017/09/QQ截图20170908182932.png" alt="" width="750" height="644" />

最后，项目依赖库中的jar会全部自动部署到WEB-INF/lib目录下：

<img class="aligncenter size-full wp-image-128" src="http://47.93.1.79/wordpress/wp-content/uploads/2017/09/QQ截图20170908183210.png" alt="" width="749" height="565" />

<strong>Step7</strong>:至此项目构建完成，右击项目文件，run as&gt; Run on Server。完成后界面如下：

<img class="aligncenter size-full wp-image-129" src="http://47.93.1.79/wordpress/wp-content/uploads/2017/09/QQ截图20170908183508.png" alt="" width="726" height="414" />