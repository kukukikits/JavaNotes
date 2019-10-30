# 1.标准模板语法
* 下面使用xmlns的目的是防止IDE软件提示缺少th:*的命名空间，可以不用xmlns
```xml
<html xmlns:th="http://www.thymeleaf.org">
```
* 在HTML5文档里，下面的两种语法完全等效，即th:\*与data-th-\*等效。唯一的不同是data-th-*的语法只能在HTML5文档中使用
```xml
<p data-th-text="#{home.welcome}">Welcome to our grocery store!</p>
<p th:text="#{home.welcome}">Welcome to our grocery store!</p>
```
* 特殊的模板：
```
${x} will return a variable x stored into the Thymeleaf context or as a request attribute.
${param.x} will return a request parameter called x (which might be multivalued).
${session.x} will return a session attribute called x.
${application.x} will return a servlet context attribute called x.
```
* th:text=“#{home.welcome}”。th:text表示表达式#{home.welcome}的值为文本格式。#{home.welcome}的值会替换\<p\>标签的内容（Welcome to our grocery store!）。#{home.welcome}的默认值默认保存在properties文件中，例如
```
home.welcome=欢迎!
```
* th:utext=“#{home.welcome}”。th:utext (for “unescaped text”)，意思就是非转义文本。假如home.welcome=\<p\>Welcome to our \<b\>fantastic\</b\> grocery store!\</p\>，那么使用这个属性时特殊字符不会被转义。
* 表达式简介：

```
Simple expressions:
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
    Comparators: >, <, >=, <= (gt, lt, ge, le)
    Equality operators: ==, != (eq, ne)
Conditional operators:
    If-then: (if) ? (then)
    If-then-else: (if) ? (then) : (else)
    Default: (value) ?: (defaultvalue)
Special tokens:
    No-Operation: _
```

## 1.1 消息表达式<span>#{...}</span>的用法
* 使用参数

```xml
//下面的{0}表示一个参数
home.welcome=欢迎, {0}!

//在HTML里可以这样使用，并传入参数替换{0}:
<p th:utext="#{home.welcome(${session.user.name})}">
  Welcome to our grocery store, Sebastian Pepper!
</p>
// {0}被${session.user.name}的值替换
```

## 1.2 参数表达式$<span>{...}</span>的用法
```
/*
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
${person.createCompleteNameWithSeparator('-')}
```
* [表达式基本对象](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#appendix-a-expression-basic-objects)：当使用的是OGNL表达式时，下面的变量代表了特定的对象：

```
#ctx: the context object.
#vars: the context variables.
#locale: the context locale.
#request: (only in Web Contexts) the HttpServletRequest object.
#response: (only in Web Contexts) the HttpServletResponse object.
#session: (only in Web Contexts) the HttpSession object.
#servletContext: (only in Web Contexts) the ServletContext object.

我们可以这样使用这些变量：
Established locale country: <span th:text="${#locale.country}">US</span>.
```
* [表达式工具对象](https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#appendix-b-expression-utility-objects)

```
#execInfo: information about the template being processed.
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
#ids: methods for dealing with id attributes that might be repeated (for example, as a result of an iteration).
```

## 1.3 选择表达式*<span>{...}</span>的用法
${...}和*{...}都是变量表达式，但是有一点不同是：*{...}表达式是对被选中的对象进行求值。如果没有被选中的对象，那么这两个表达式是等效的。

那什么是被选中的对象呢？看下面的例子。其中${session.user}就是被选中的对象，*{firstName}就是获取user对象的firstName属性
```xml
<div th:object="${session.user}">
    <p>Name: <span th:text="*{firstName}">Sebastian</span>.</p>
    <p>Surname: <span th:text="*{lastName}">Pepper</span>.</p>
    <p>Nationality: <span th:text="*{nationality}">Saturn</span>.</p>
</div>

Of course, dollar and asterisk syntax can be mixed:
<div th:object="${session.user}">
  <p>Name: <span th:text="*{firstName}">Sebastian</span>.</p>
  <p>Surname: <span th:text="${session.user.lastName}">Pepper</span>.</p>
  <p>Nationality: <span th:text="*{nationality}">Saturn</span>.</p>
</div>

When an object selection is in place, the selected object will also be available to dollar expressions as the #object expression variable: <div th:object="${session.user}"> <p>Name: <span th:text="${#object.firstName}">Sebastian</span>.</p> <p>Surname: <span th:text="${session.user.lastName}">Pepper</span>.</p> <p>Nationality: <span th:text="*{nationality}">Saturn</span>.</p> </div>
```

## 1.4 链接表达式@{...}
URL的类型有：
* 绝对URL：http://www.thymeleaf.org
* 相对URL：
  * 页面相对： user/login.html
  * 上下文相对： user/login.html服务器的上下文会自动添加到URL上
  * 服务器相对：~/billing/processInvoice允许请求同一台服务器上不同上下文（也就是应用）
  * 协议相对：//code.jquery.com/jquery-2.0.3.min.js

th:href属性修饰符的用法：
```xml
<!-- Will produce 'http://localhost:8080/gtvg/order/details?orderId=3' (plus rewriting) -->
<a href="details.html" 
   th:href="@{http://localhost:8080/gtvg/order/details(orderId=${o.id})}">view</a>

<!-- Will produce '/gtvg/order/details?orderId=3' (plus rewriting) -->
<a href="details.html" th:href="@{/order/details(orderId=${o.id})}">view</a>

<!-- Will produce '/gtvg/order/3/details' (plus rewriting) -->
<a href="details.html" th:href="@{/order/{orderId}/details(orderId=${o.id})}">view</a>
```

## 1.5 字面量
* 文本字面量：使用单引号括起来的字符串。如：
	```xml
	<p>
	Now you are looking at a <span th:text="'working web application'">template file</span>.
	</p>
	```
* 数值字面量：单纯的数字：
	```xml
	<p>The year is <span th:text="2013">1492</span>.</p>
	<p>In two years, it will be <span th:text="2013 + 2">1494</span>.</p>
	···
* 布尔型字面量：true 和 false
	```xml
	<div th:if="${user.isAdmin()} == false"> Thymeleaf takes care of == false
	<div th:if="${user.isAdmin() == false}"> OGNL/SpringEL engines takes care of == false
	```
* null
	```xml
	<div th:if="${variable.something} == null"> ...
	```
* 字面量符号：不使用''表示的字面量，可以是字母(A-Z and a-z)、数字 (0-9)、括号 ([ and ])、点(.)、连字符(.)、下划线 (_).
	```xml
	<div th:class="content">注意th:class后面的值没有用单引号，这也是字面量的一种声明方式</div>
	```

## 1.6 连接值
使用+连接
```xml
<span th:text="'The name of the user is ' + ${user.name}">
```

## 1.7 Literal substitutions 字面量替换？
使用竖线(|)，将多个值替换为一个，免去了使用+连接值的繁琐。
```xml
<span th:text="|Welcome to our application, ${user.name}!|">
等价于下面的这个：
<span th:text="'Welcome to our application, ' + ${user.name} + '!'">

也可以和其他表达式混着用
<span th:text="${onevar} + ' ' + |${twovar}, ${threevar}|">
```
只有变量/消息表达式: ${...}, *{...}, #{...} 能在 |...| 符号里使用. 


## 1.8 算数操作符
```xml
<div th:with="isEven=(${prodStat.count} % 2 == 0)">
<div th:with="isEven=${prodStat.count % 2 == 0}">
```

## 1.9 比较符
由于XML语法中&lt;和&gt;的特殊性，在使用大于和小于时，需要使用其转义符：<code>&amp;lt;</code><span> and </span><code>&amp;gt;</code>
```xml
<div th:if="${prodStat.count} &gt; 1">
<span th:text="'Execution mode is ' + ( (${execMode} == 'dev')? 'Development' : 'Production')">
```

更简洁的写法是直接使用比较符的转义符：<code>gt</code><span> (</span><code>&gt;</code><span>), </span><code>lt</code><span> (</span><code>&lt;</code><span>), </span><code>ge</code><span> (</span><code>&gt;=</code><span>), </span><code>le</code><span> (</span><code>&lt;=</code><span>), </span><code>not</code><span> (</span><code>!</code><span>). Also </span><code>eq</code><span> (</span><code>==</code><span>), </span><code>neq</code><span>/</span><code>ne</code><span> (</span><code>!=</code><span>).</span>

## 1.10 条件判断
```xml
<tr th:class="${row.even}? 'even' : 'odd'">
  ...
</tr>
```

th:class中的条件判断语句由三个部分组成：<code>condition</code><span>, </span><code>then</code><span> 和</span><code>else</code>。这三个部分都是独立的表达式，可以是<span> variables (</span><code>${...}</code><span>, </span><code>*{...}</code><span>), messages (</span><code class="cye-lm-tag">#{...}</code><span>), URLs (</span><code>@{...}</code><span>) or literals (</span><code>'...'</code><span>).</span>

表达式可以内嵌：
```xml
<tr th:class="${row.even}? (${row.first}? 'first' : 'even') : 'odd'">
  ...
</tr>
```

也可以省略else
```xml
<tr th:class="${row.even}? 'alt'">
  row.even是false的话条件返回null值
</tr>
```

## 1.11 设置默认值
```xml
<div th:object="${session.user}">
  ...
  <p>Age: <span th:text="*{age}?: '(no age specified)'">27</span>.</p>
</div>
```

如上所示我们可以使用<code>?:</code>来设置默认值。上面的例子中如果<span>*{age}是null，那么年龄27就会被默认值替换</span>

表达式同时支持内嵌：
```xml
<p>
  Name: 
  <span th:text="*{firstName}?: (*{admin}? 'Admin' : #{default.username})">Sebastian</span>
</p>
```

## 1.12 无操作符
使用下划线(</span><code>_</code><span>)表示不做任何操作。举个例子：
```xml
<span th:text="${user.name} ?: 'no user authenticated'">...</span>
```

可以替换为：
```xml
<span th:text="${user.name} ?: _">no user authenticated</span>
```

当user.name为null时，什么都不做，那么span标签里还是no user authenticated。

## 1.13 数据转换/格式化
为了给数据定义不同的转化规则，Thymeleaf给<span>变量(</span><code>${...}</code><span>) 和选择selection (</span><code>*{...}</code>) 表达式定义了双括号语法，就像这样：
```xml
<td th:text="${{user.lastAccessDate}}">...</td>
```

${{...}}语法命令Thymeleaf把user.lastAccessDate的值传到专门的<em>conversion service</em>里，然后这个转换服务把user.lastAccessDate的值格式化为字符串。

Thymeleaf里默认的<em>conversion service </em>的实现类是<span>IStandardConversionService，默认使用对象的.toString()方法进行格式化。用户也可以自定义转换服务，详情见<a href="https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#more-on-configuration">More on Configuration</a></span>

<span style="color: #ff0000;">&#x2666; Spring实现了自己的<em>conversion service ，</em>所以Spring也支持这个语法</span>


## 1.14 预处理
预处理可以在正常的表达式之前完成表达式的执行，它允许指定要被执行的表达式。举个例子：

下面是Messages_fr.properties文件里的某个属性，这个article.text属性时一个OGNL表达式，它调用了myapp.translator.Translator类的translateToFrench静态方法，并传入一个参数。
```properties
article.text=@myapp.translator.Translator@translateToFrench({0})
```

同时Messages_ch.properties文件里是这样定义的：
```properties
article.text=@myapp.translator.Translator@translateToChinese({0})
```

可以看到，两个properties文件中需要执行不同的方法，Thymeleaf可以预先执行些方法并拿到方法的返回值，对应的表达式是\__${expression}__。在这个例子中可以使用如下的预处理过程：
```xml
<p th:text="${__#{article.text('textVar')}__}">Some text here...</p>
```

最后在本地是中文的情况下，Thymeleaf会首先选择对应的article.text，然后执行方法，拿到返回值，最后替换&lt;p&gt;标签的内容。和下面的预处理表达式是等效的：
```xml
<p th:text="${@myapp.translator.Translator@translateToChinese(textVar)}">Some text here...</p>
```
