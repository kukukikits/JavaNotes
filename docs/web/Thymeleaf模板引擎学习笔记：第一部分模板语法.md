</content:encoded>
		<excerpt:encoded><![CDATA[]]></excerpt:encoded>
		<wp:post_id>397</wp:post_id>
		<wp:post_date><![CDATA[2018-07-25 14:42:53]]></wp:post_date>
		<wp:post_date_gmt><![CDATA[2018-07-25 14:42:53]]></wp:post_date_gmt>
		<wp:comment_status><![CDATA[open]]></wp:comment_status>
		<wp:ping_status><![CDATA[open]]></wp:ping_status>
		<wp:post_name><![CDATA[%e8%b7%a8%e7%ab%99%e8%af%b7%e6%b1%82%e4%bc%aa%e9%80%a0%e5%8f%8aspring-security%e9%92%88%e5%af%b9%e6%80%a7%e7%9a%84%e4%bf%9d%e6%8a%a4%e6%8e%aa%e6%96%bd%ef%bc%88spring-security%e5%ae%98%e6%96%b9]]></wp:post_name>
		<wp:status><![CDATA[publish]]></wp:status>
		<wp:post_parent>0</wp:post_parent>
		<wp:menu_order>0</wp:menu_order>
		<wp:post_type><![CDATA[post]]></wp:post_type>
		<wp:post_password><![CDATA[]]></wp:post_password>
		<wp:is_sticky>0</wp:is_sticky>
		<category domain="category" nicename="uncategorized"><![CDATA[Uncategorized]]></category>
		<wp:postmeta>
			<wp:meta_key><![CDATA[_edit_last]]></wp:meta_key>
			<wp:meta_value><![CDATA[1]]></wp:meta_value>
		</wp:postmeta>
	</item>
	<item>
		<title>Thymeleaf模板引擎学习笔记： 第一部分模板语法</title>
		<link>http://47.93.1.79/wordpress/?p=401</link>
		<pubDate>Fri, 27 Jul 2018 03:32:06 +0000</pubDate>
		<dc:creator><![CDATA[GeShengBin]]></dc:creator>
		<guid isPermaLink="false">http://47.93.1.79/wordpress/?p=401</guid>
		<description></description>
		<content:encoded><![CDATA[<h3>1.标准模板语法</h3>
<ul>
 	<li>下面使用xmlns的目的是防止IDE软件提示缺少th:*的命名空间，可以不用xmlns</li>
</ul>
<pre class="language-html cye-lm-tag"><code class=" language-html"><span class="token tag cye-lm-tag"><span class="token punctuation cye-lm-tag">&lt;</span>html <span class="token attr-name cye-lm-tag"><span class="token namespace cye-lm-tag">xmlns:</span>th</span><span class="token attr-value cye-lm-tag"><span class="token punctuation cye-lm-tag">=</span><span class="token punctuation cye-lm-tag">"</span>http://www.thymeleaf.org<span class="token punctuation cye-lm-tag">"</span></span><span class="token punctuation cye-lm-tag">&gt;</span></span></code></pre>
<ul>
 	<li>在HTML5文档里，下面的两种语法完全等效，即th:*与data-th-*等效。唯一的不同是data-th-*的语法只能在HTML5文档中使用</li>
</ul>
<pre class="prettyprint">&lt;p data-th-text="#{home.welcome}"&gt;Welcome to our grocery store!&lt;/p&gt;
&lt;p th:text="#{home.welcome}"&gt;Welcome to our grocery store!&lt;/p&gt;</pre>
<ul>
 	<li>特殊的模板：</li>
</ul>
<pre class="prettyprint">${x} will return a variable x stored into the Thymeleaf context or as a request attribute.
${param.x} will return a request parameter called x (which might be multivalued).
${session.x} will return a session attribute called x.
${application.x} will return a servlet context attribute called x.</pre>
<ul>
 	<li><span style="color: #ff0000;">th:text</span>=“#{home.welcome}”。th:text表示表达式#{home.welcome}的值为文本格式。#{home.welcome}的值会替换&lt;p&gt;标签的内容（Welcome to our grocery store!）。<span style="color: #555555; font-size: 17px; line-height: 1.8em;">#{home.welcome}的默认值默认保存在properties文件中，例如</span></li>
</ul>
<pre class="prettyprint">home.welcome=欢迎!</pre>
<ul>
 	<li><span><span><span style="color: #ff0000;">th:utext</span>=“#{home.welcome}”。<code class="cye-lm-tag">th:utext</code> (for “unescaped text”)，意思就是非转义文本。假如home.welcome=</span></span>&lt;p&gt;Welcome to our &lt;b&gt;fantastic&lt;/b&gt; grocery store!&lt;/p&gt;，那么使用这个属性时特殊字符不会被转义。</li>
 	<li>表达式简介：<!--?prettify linenums=true?-->
<pre class="prettyprint">Simple expressions:
    Variable Expressions: ${...}
    Selection Variable Expressions: *{...}
    Message Expressions: #{...}
    Link URL Expressions: @{...}
    Fragment Expressions: ~{...}
Literals
    Text literals: 'one text', 'Another one!',…
    Number literals: 0, 34, 3.0, 12.3,…
    Boolean literals: true, false
    Null literal: null
    Literal tokens: one, sometext, main,…
Text operations:
    String concatenation: +
    Literal substitutions: |The name is ${name}|
Arithmetic operations:
    Binary operators: +, -, *, /, %
    Minus sign (unary operator): -
Boolean operations:
    Binary operators: and, or
    Boolean negation (unary operator): !, not
Comparisons and equality:
    Comparators: &gt;, &lt;, &gt;=, &lt;= (gt, lt, ge, le)
    Equality operators: ==, != (eq, ne)
Conditional operators:
    If-then: (if) ? (then)
    If-then-else: (if) ? (then) : (else)
    Default: (value) ?: (defaultvalue)
Special tokens:
    No-Operation: _</pre>
</li>
</ul>
<h3>1.1 消息表达式<span>#{...}</span>的用法</h3>
<ul>
 	<li>使用参数</li>
</ul>
<pre class="prettyprint">//下面的{0}表示一个参数
home.welcome=欢迎, {0}!

//在HTML里可以这样使用，并传入参数替换{0}:
&lt;p th:utext="#{home.welcome(${session.user.name})}"&gt;
  Welcome to our grocery store, Sebastian Pepper!
&lt;/p&gt;
// {0}被${session.user.name}的值替换</pre>
<h3>1.2 参数表达式$<span>{...}</span>的用法</h3>
<pre class="prettyprint">/*
 * 使用点(.)访问属性。和调用属性的getter方法等效
 */
${person.father.name}

/*
 * 使用中括号([])访问属性， 并且用单引号''写入属性名
 */
${person['father']['name']}

/*
 * 如果是个map对象, 点语法和中括号语法可以结合使用，并与调用属性的get方法是等价的
 */
${countriesByCode.ES}
${personsByName['Stephen Zucchini'].age}

/*
 * 使用下标访问arrays或collections对象
 */
${personsArray[0].name}

/*
 * 调用对象方法
 */
${person.createCompleteName()}
${person.createCompleteNameWithSeparator('-')}</pre>
<ul>
 	<li><a href="https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#appendix-a-expression-basic-objects">表达式基本对象</a>：当使用的是<span>OGNL表达式时，下面的变量代表了特定的对象：</span></li>
</ul>
<pre class="prettyprint">#ctx: the context object.
#vars: the context variables.
#locale: the context locale.
#request: (only in Web Contexts) the HttpServletRequest object.
#response: (only in Web Contexts) the HttpServletResponse object.
#session: (only in Web Contexts) the HttpSession object.
#servletContext: (only in Web Contexts) the ServletContext object.

我们可以这样使用这些变量：
Established locale country: &lt;span th:text="${#locale.country}"&gt;US&lt;/span&gt;.</pre>
<ul>
 	<li><a href="https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#appendix-b-expression-utility-objects">表达式工具对象</a>。</li>
</ul>
<pre class="prettyprint">#execInfo: information about the template being processed.
#messages: methods for obtaining externalized messages inside variables expressions, in the same way as they would be obtained using #{…} syntax.
#uris: methods for escaping parts of URLs/URIs
#conversions: methods for executing the configured conversion service (if any).
#dates: methods for java.util.Date objects: formatting, component extraction, etc.
#calendars: analogous to #dates, but for java.util.Calendar objects.
#numbers: methods for formatting numeric objects.
#strings: methods for String objects: contains, startsWith, prepending/appending, etc.
#objects: methods for objects in general.
#bools: methods for boolean evaluation.
#arrays: methods for arrays.
#lists: methods for lists.
#sets: methods for sets.
#maps: methods for maps.
#aggregates: methods for creating aggregates on arrays or collections.
#ids: methods for dealing with id attributes that might be repeated (for example, as a result of an iteration).</pre>
<h3>1.3 选择表达式*<span>{...}</span>的用法</h3>
<span>${...}和</span>*{...}都是变量表达式，但是有一点不同是：*{...}表达式是对被选中的对象进行求值。如果没有被选中的对象，那么这两个表达式是等效的。

那什么是被选中的对象呢？看下面的例子。其中${session.user}就是被选中的对象，*{firstName}就是获取user对象的firstName属性
<pre class="prettyprint">&lt;div th:object="${session.user}"&gt;
    &lt;p&gt;Name: &lt;span th:text="*{firstName}"&gt;Sebastian&lt;/span&gt;.&lt;/p&gt;
    &lt;p&gt;Surname: &lt;span th:text="*{lastName}"&gt;Pepper&lt;/span&gt;.&lt;/p&gt;
    &lt;p&gt;Nationality: &lt;span th:text="*{nationality}"&gt;Saturn&lt;/span&gt;.&lt;/p&gt;
&lt;/div&gt;

<span>Of course, dollar and asterisk syntax can be mixed:</span>
&lt;div th:object="${session.user}"&gt;
  &lt;p&gt;Name: &lt;span th:text="*{firstName}"&gt;Sebastian&lt;/span&gt;.&lt;/p&gt;
  &lt;p&gt;Surname: &lt;span th:text="${session.user.lastName}"&gt;Pepper&lt;/span&gt;.&lt;/p&gt;
  &lt;p&gt;Nationality: &lt;span th:text="*{nationality}"&gt;Saturn&lt;/span&gt;.&lt;/p&gt;
&lt;/div&gt;

<span>When an object selection is in place, the selected object will also be available to dollar expressions as the </span><code>#object</code><span> expression variable:</span> &lt;div th:object="${session.user}"&gt; &lt;p&gt;Name: &lt;span th:text="${#object.firstName}"&gt;Sebastian&lt;/span&gt;.&lt;/p&gt; &lt;p&gt;Surname: &lt;span th:text="${session.user.lastName}"&gt;Pepper&lt;/span&gt;.&lt;/p&gt; &lt;p&gt;Nationality: &lt;span th:text="*{nationality}"&gt;Saturn&lt;/span&gt;.&lt;/p&gt; &lt;/div&gt;</pre>
<h3>1.4 链接表达式@<span>{...}</span></h3>
URL的类型有：
<ul>
 	<li>绝对URL：http://www.thymeleaf.org</li>
 	<li>相对URL：
<ul>
 	<li>页面相对：<span> </span><code class="cye-lm-tag">user/login.html</code></li>
 	<li>上下文相对：<span> </span><code class="cye-lm-tag">user/login.html</code>服务器的上下文会自动添加到URL上</li>
 	<li>服务器相对：<span>~/billing/processInvoice允许请求同一台服务器上不同上下文（也就是应用）</span></li>
 	<li>协议相对：<span>//code.jquery.com/jquery-2.0.3.min.js</span></li>
</ul>
</li>
</ul>
<span>th:href属性修饰符的用法：</span>
<pre class="prettyprint">&lt;!-- Will produce 'http://localhost:8080/gtvg/order/details?orderId=3' (plus rewriting) --&gt;
&lt;a href="details.html" 
   th:href="@{http://localhost:8080/gtvg/order/details(orderId=${o.id})}"&gt;view&lt;/a&gt;

&lt;!-- Will produce '/gtvg/order/details?orderId=3' (plus rewriting) --&gt;
&lt;a href="details.html" th:href="@{/order/details(orderId=${o.id})}"&gt;view&lt;/a&gt;

&lt;!-- Will produce '/gtvg/order/3/details' (plus rewriting) --&gt;
&lt;a href="details.html" th:href="@{/order/{orderId}/details(orderId=${o.id})}"&gt;view&lt;/a&gt;</pre>
<h3>1.5 字面量</h3>
<ul>
 	<li>文本字面量：使用单引号括起来的字符串。如：<!--?prettify linenums=true?-->
<pre class="prettyprint">&lt;p&gt;
  Now you are looking at a &lt;span th:text="'working web application'"&gt;template file&lt;/span&gt;.
&lt;/p&gt;</pre>
</li>
 	<li>数值字面量：单纯的数字：<!--?prettify linenums=true?-->
<pre class="prettyprint">&lt;p&gt;The year is &lt;span th:text="2013"&gt;1492&lt;/span&gt;.&lt;/p&gt;
&lt;p&gt;In two years, it will be &lt;span th:text="2013 + 2"&gt;1494&lt;/span&gt;.&lt;/p&gt;</pre>
</li>
 	<li>布尔型字面量：<code>true</code><span> 和 </span><code class="cye-lm-tag"><code class="cye-lm-tag">false<!--?prettify linenums=true?--></code></code>
<pre class="prettyprint">&lt;div th:if="${user.isAdmin()} == false"&gt; Thymeleaf takes care of == false
&lt;div th:if="${user.isAdmin() == false}"&gt; OGNL/SpringEL engines takes care of == false</pre>
</li>
 	<li>null<!--?prettify linenums=true?-->
<pre class="prettyprint">&lt;div th:if="${variable.something} == null"&gt; ...</pre>
</li>
 	<li>字面量符号：不使用''表示的字面量，可以是字母<span>(</span><code>A-Z</code><span> and </span><code>a-z</code><span><span>)、数字 (<code>0-9</code>)、括号 (<code>[</code> and <code>]</code>)、点(<code>.</code>)、连字符(<code>.</code>)、下划线 (<code>_</code>).<!--?prettify linenums=true?--></span></span>
<pre class="prettyprint">&lt;div th:class="content"&gt;注意th:class后面的值没有用单引号，这也是字面量的一种声明方式&lt;/div&gt;</pre>
</li>
</ul>
<h3>1.6 连接值</h3>
使用+连接
<pre class="prettyprint">&lt;span th:text="'The name of the user is ' + ${user.name}"&gt;</pre>
<h3>1.7 Literal substitutions 字面量替换？</h3>
使用竖线<span>(</span><code>|</code><span>)，将多个值替换为一个，免去了使用+连接值的繁琐。<!--?prettify linenums=true?--></span>
<pre class="prettyprint">&lt;span th:text="|Welcome to our application, ${user.name}!|"&gt;
等价于下面的这个：
&lt;span th:text="'Welcome to our application, ' + ${user.name} + '!'"&gt;

也可以和其他表达式混着用
&lt;span th:text="${onevar} + ' ' + |${twovar}, ${threevar}|"&gt;</pre>
<p class="cye-lm-tag"><span style="color: #ff0000;">只有变量/消息表达式 (<code>${...}</code>, <code>*{...}</code>, <code>#{...}</code>) 能在 <code>|...|</code> 符号里使用. </span></p>

<h3>1.8 算数操作符</h3>
<pre class="prettyprint">&lt;div th:with="isEven=(${prodStat.count} % 2 == 0)"&gt;
&lt;div th:with="isEven=${prodStat.count % 2 == 0}"&gt;</pre>
<h3>1.9 比较符</h3>
由于XML语法中&lt;和&gt;的特殊性，在使用大于和小于时，需要使用其转义符：<code>&amp;lt;</code><span> and </span><code>&amp;gt;</code>
<pre class="prettyprint">&lt;div th:if="${prodStat.count} &amp;gt; 1"&gt;
&lt;span th:text="'Execution mode is ' + ( (${execMode} == 'dev')? 'Development' : 'Production')"&gt;</pre>
更简洁的写法是直接使用比较符的转义符：<code>gt</code><span> (</span><code>&gt;</code><span>), </span><code>lt</code><span> (</span><code>&lt;</code><span>), </span><code>ge</code><span> (</span><code>&gt;=</code><span>), </span><code>le</code><span> (</span><code>&lt;=</code><span>), </span><code>not</code><span> (</span><code>!</code><span>). Also </span><code>eq</code><span> (</span><code>==</code><span>), </span><code>neq</code><span>/</span><code>ne</code><span> (</span><code>!=</code><span>).</span>
<h3>1.10 条件判断</h3>
<pre class="prettyprint">&lt;tr th:class="${row.even}? 'even' : 'odd'"&gt;
  ...
&lt;/tr&gt;</pre>
th:class中的条件判断语句由三个部分组成：<code>condition</code><span>, </span><code>then</code><span> 和</span><code>else</code>。这三个部分都是独立的表达式，可以是<span> variables (</span><code>${...}</code><span>, </span><code>*{...}</code><span>), messages (</span><code class="cye-lm-tag">#{...}</code><span>), URLs (</span><code>@{...}</code><span>) or literals (</span><code>'...'</code><span>).</span>

表达式可以内嵌：
<pre class="prettyprint">&lt;tr th:class="${row.even}? (${row.first}? 'first' : 'even') : 'odd'"&gt;
  ...
&lt;/tr&gt;</pre>
也可以省略else<!--?prettify linenums=true?-->
<pre class="prettyprint">&lt;tr th:class="${row.even}? 'alt'"&gt;
  <span style="color: #ff0000;">row.even是false的话条件返回null值</span>
&lt;/tr&gt;</pre>
<h3>1.11 设置默认值</h3>
<pre class="prettyprint">&lt;div th:object="${session.user}"&gt;
  ...
  &lt;p&gt;Age: &lt;span th:text="*{age}?: '(no age specified)'"&gt;27&lt;/span&gt;.&lt;/p&gt;
&lt;/div&gt;</pre>
如上所示我们可以使用<code>?:</code>来设置默认值。上面的例子中如果<span>*{age}是null，那么年龄27就会被默认值替换</span>

表达式同时支持内嵌：
<pre class="prettyprint">&lt;p&gt;
  Name: 
  &lt;span th:text="*{firstName}?: (*{admin}? 'Admin' : #{default.username})"&gt;Sebastian&lt;/span&gt;
&lt;/p&gt;</pre>
<h3>1.12 无操作符</h3>
使用下划线<span> (</span><code>_</code><span>)表示不做任何操作。举个例子：<!--?prettify linenums=true?--></span>
<pre class="prettyprint">&lt;span th:text="${user.name} ?: 'no user authenticated'"&gt;...&lt;/span&gt;</pre>
可以替换为：<!--?prettify linenums=true?-->
<pre class="prettyprint">&lt;span th:text="${user.name} ?: _"&gt;no user authenticated&lt;/span&gt;</pre>
当user.name为null时，什么都不做，那么span标签里还是no user authenticated。
<h3>1.13 数据转换/格式化</h3>
为了给数据定义不同的转化规则，Thymeleaf给<span>变量(</span><code>${...}</code><span>) 和选择selection (</span><code>*{...}</code><span>) 表达式定义了双括号语法，就像这样：<!--?prettify linenums=true?--></span>
<pre class="prettyprint">&lt;td th:text="${{user.lastAccessDate}}"&gt;...&lt;/td&gt;</pre>
${{...}}语法命令Thymeleaf把user.lastAccessDate的值传到专门的<em>conversion service</em>里，然后这个转换服务把user.lastAccessDate的值格式化为字符串。

Thymeleaf里默认的<em>conversion service </em>的实现类是<span>IStandardConversionService，默认使用对象的.toString()方法进行格式化。用户也可以自定义转换服务，详情见<a href="https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#more-on-configuration">More on Configuration</a></span>

<span style="color: #ff0000;">&#x2666; Spring实现了自己的<em>conversion service ，</em>所以Spring也支持这个语法</span>
<h3>1.14 预处理</h3>
<span>预处理可以在正常的表达式之前完成表达式的执行，它允许指定要被执行的表达式。举个例子：</span>

下面是<span>Messages_fr.properties文件里的某个属性，这个</span>article.text属性时一个<span>OGNL表达式，它调用了</span>myapp.translator.Translator类的translateToFrench静态方法，并传入一个参数。<!--?prettify linenums=true?-->
<pre class="prettyprint">article.text=@myapp.translator.Translator@translateToFrench({0})</pre>
同时<span>Messages_ch.properties文件里是这样定义的：</span>
<pre class="prettyprint">article.text=@myapp.translator.Translator@translateToChinese({0})</pre>
可以看到，两个properties文件中需要执行不同的方法，Thymeleaf可以预先执行些方法并拿到方法的返回值，对应的表达式是<span>__${expression}__。在这个例子中可以使用如下的预处理过程：</span>
<pre class="prettyprint">&lt;p th:text="${__#{article.text('textVar')}__}"&gt;Some text here...&lt;/p&gt;</pre>
最后在本地是中文的情况下，Thymeleaf会首先选择对应的article.text，然后执行方法，拿到返回值，最后替换&lt;p&gt;标签的内容。和下面的预处理表达式是等效的：
<pre class="prettyprint">&lt;p th:text="${@myapp.translator.Translator@translateToChinese(textVar)}"&gt;Some text here...&lt;/p&gt;</pre>