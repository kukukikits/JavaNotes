导入Maven工程发现没有Maven Dependencies这个依赖库。解决方案：

1.myEclipse<span>操作方式： 右击 web project --&gt;  Properties --&gt; Macven --&gt; Enable Dependency Management</span>

2.或在Maven项目目录中修改.classpath文件，添加以下代码：
<pre class="prettyprint">&lt;classpathentry kind="con" path="org.eclipse.m2e.MAVEN2_CLASSPATH_CONTAINER"&gt;  
    &lt;attributes&gt;  
        &lt;attribute name="maven.pomderived" value="true"/&gt;  
        &lt;attribute name="org.eclipse.jst.component.dependency" value="/WEB-INF/lib"/&gt;  
    &lt;/attributes&gt;  
&lt;/classpathentry&gt;</pre>
<span>Java build path，发现Maven Dependencies回来了</span>