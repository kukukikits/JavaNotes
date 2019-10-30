# 2. 设置属性值
## 2.1 给任意属性赋值
使用th:attr可以给任意标签进行属性赋值，举例如下：<!--?prettify linenums=true?-->
```xml
<form action="subscribe.html" th:attr="action=@{/subscribe}">
  <fieldset>
    <input type="text" name="email" />
    <input type="submit" value="Subscribe!" th:attr="value=#{subscribe.submit}"/>
  </fieldset>
</form>
```

&lt;form&gt;元素的action属性会设置为@{/subscribe}变量的值

&lt;input type="submit"&gt;元素的value属性会被设置为#{subscribe.submit}变量的值

多个赋值操作用逗号隔开：
```xml
<img src="../../images/gtvglogo.png" 
     th:attr="src=@{/images/gtvglogo.png},title=#{logo},alt=#{logo}" />
结果举例：
<img src="/gtgv/images/gtvglogo.png" title="Logo de Good Thymes" alt="Logo de Good Thymes" />
```

## 2.2 给指定的属性赋值
th:\*属性可以给特定的属性赋值，通过把*替换为指定的属性名来使用：
```xml
<input type="submit" value="Subscribe!" th:value="#{subscribe.submit}"/>
<form action="subscribe.html" th:action="@{/subscribe}">
```
Thymeleaf支持很多HTML5的属性：
|          --           |         --          |        --         |         --         |         --          |       --        |
| :-------------------: | :-----------------: | :---------------: | :----------------: | :-----------------: | :-------------: |
|        th:abbr        |      th:accept      | th:accept-charset |    th:accesskey    |      th:action      |    th:align     |
|        th:alt         |     th:archive      |     th:audio      |  th:autocomplete   |       th:axis       |  th:background  |
|      th:bgcolor       |      th:border      |  th:cellpadding   |   th:cellspacing   |    th:challenge     |   th:charset    |
|        th:cite        |      th:class       |    th:classid     |    th:codebase     |     th:codetype     |     th:cols     |
|      th:colspan       |     th:compact      |    th:content     | th:contenteditable |   th:contextmenu    |     th:data     |
|      th:datetime      |       th:dir        |   th:draggable    |    th:dropzone     |     th:enctype      |     th:for      |
|        th:form        |    th:formaction    |  th:formenctype   |   th:formmethod    |    th:formtarget    |   th:fragment   |
|       th:frame        |   th:frameborder    |    th:headers     |     th:height      |       th:high       |     th:href     |
|      th:hreflang      |      th:hspace      |   th:http-equiv   |      th:icon       |        th:id        |    th:inline    |
|      th:keytype       |       th:kind       |     th:label      |      th:lang       |       th:list       |   th:longdesc   |
|        th:low         |     th:manifest     |  th:marginheight  |   th:marginwidth   |       th:max        |  th:maxlength   |
|       th:media        |      th:method      |      th:min       |      th:name       |     th:onabort      | th:onafterprint |
|   th:onbeforeprint    |  th:onbeforeunload  |     th:onblur     |    th:oncanplay    | th:oncanplaythrough |   th:onchange   |
|      th:onclick       |  th:oncontextmenu   |   th:ondblclick   |     th:ondrag      |    th:ondragend     | th:ondragenter  |
|    th:ondragleave     |    th:ondragover    |  th:ondragstart   |     th:ondrop      | th:ondurationchange |  th:onemptied   |
|      th:onended       |     th:onerror      |    th:onfocus     |  th:onformchange   |   th:onforminput    | th:onhashchange |
|      th:oninput       |    th:oninvalid     |   th:onkeydown    |   th:onkeypress    |     th:onkeyup      |    th:onload    |
|    th:onloadeddata    | th:onloadedmetadata |  th:onloadstart   |    th:onmessage    |   th:onmousedown    | th:onmousemove  |
|     th:onmouseout     |   th:onmouseover    |   th:onmouseup    |  th:onmousewheel   |    th:onoffline     |   th:ononline   |
|      th:onpause       |      th:onplay      |   th:onplaying    |   th:onpopstate    |    th:onprogress    | th:onratechange |
| th:onreadystatechange |      th:onredo      |    th:onreset     |    th:onresize     |     th:onscroll     |   th:onseeked   |
|     th:onseeking      |     th:onselect     |     th:onshow     |    th:onstalled    |    th:onstorage     |   th:onsubmit   |
|     th:onsuspend      |   th:ontimeupdate   |     th:onundo     |    th:onunload     |  th:onvolumechange  |  th:onwaiting   |
|      th:optimum       |     th:pattern      |  th:placeholder   |     th:poster      |     th:preload      |  th:radiogroup  |
|        th:rel         |       th:rev        |      th:rows      |     th:rowspan     |      th:rules       |   th:sandbox    |
|       th:scheme       |      th:scope       |   th:scrolling    |      th:size       |      th:sizes       |     th:span     |
|     th:spellcheck     |       th:src        |    th:srclang     |     th:standby     |      th:start       |     th:step     |
|       th:style        |     th:summary      |    th:tabindex    |     th:target      |      th:title       |     th:type     |
|       th:usemap       |      th:value       |   th:valuetype    |     th:vspace      |      th:width       |     th:wrap     |
|      th:xmlbase       |     th:xmllang      |    th:xmlspace    |


## 2.3 同时给多个属性赋值
- **th:alt-title** 会把 **alt** 和 **title**属性设置为相同的值
- **th:lang-xmllang** 会把 **lang** 和 **xml:lang**属性设置为相同的值

下面的两种写法是等效的
```xml
<img src="../../images/gtvglogo.png" 
     th:src="@{/images/gtvglogo.png}" th:title="#{logo}" th:alt="#{logo}" />
<img src="../../images/gtvglogo.png" 
     th:src="@{/images/gtvglogo.png}" th:alt-title="#{logo}" />
```

## 2.4 添加和插入
如果你想在某个属性值的后面添加一个值，或者在它前面插入一个值，那么可以使用<code>th:attrappend</code><span> 和<code>th:attrprepend</code> ，举个例子：</span>
```xml
<input type="button" value="Do it!" class="btn" th:attrappend="class=${' ' + cssStyle}" />
如果cssStyle变量等于warning，那么上面的模板最终结果是：
<input type="button" value="Do it!" class="btn warning" />
```

还有两个专用的语法：<code>th:classappend</code><span> 和 </span><code>th:styleappend</code>，举个例子：
```xml
<tr class="row" th:classappend="${prodStat.odd}? 'odd'">
<tr style="color:black;" th:styleappend="${customStyle}">
```

## 2.5 布尔属性
在HTML和XHTML中都有布尔属性，但是表示方式不一样。如果属性值为true，那么在HTML中该属性值就是true，而在XHTML中该属性值为属性名，举例来说：
```xml
<input type="checkbox" name="option2" checked /> <!-- HTML -->
<input type="checkbox" name="option1" checked="checked" /> <!-- XHTML -->
```
Thymeleaf支持根据条件来设置合适的值，即如果条件为true，那么属性值会根据文档类型来设置，如果条件为false，那么就不设置这个值：
```xml
<input type="checkbox" name="active" th:checked="${user.active}" />
```

下面是支持的布尔属性：
| --                | --           | --            |
| ----------------- | ------------ | ------------- |
| th:async          | th:autofocus | th:autoplay   |
| th:checked        | th:controls  | th:declare    |
| th:default        | th:defer     | th:disabled   |
| th:formnovalidate | th:hidden    | th:ismap      |
| th:loop           | th:multiple  | th:novalidate |
| th:nowrap         | th:open      | th:pubdate    |
| th:readonly       | th:required  | th:reversed   |
| th:scoped         | th:seamless  | th:selected   |

## 2.6 设置任意属性和其值
thymeleaf提供了一个默认的属性处理器，它允许我们设置任何属性的值。
```xml
<span th:whatever="${user.name}">...</span>

结果：
<span whatever="John Apricot">...</span>
```

## 2.7 HTML5友好型写法
支持使用<code>data-{prefix}-{name}</code><span> 的语法来设置HTML5元素的属性，不需要开发人员使用th:*这样的命名空间的写法。</span>

```xml
<table>
    <tr data-th-each="user : ${users}">
        <td data-th-text="${user.login}">...</td>
        <td data-th-text="${user.name}">...</td>
    </tr>
</table>
```

<span>还有一种语法用于指定自定义标记：<code class="cye-lm-tag">{prefix}-{name}</code>，它遵循W3C自定义元素规范。举个例子来说 <code class="cye-lm-tag">th:block和<code>th-block</code></code>支持block元素。</span>

# 第三部分 遍历
# 3 遍历
## 3.1 遍历基础
使用 th:each
```xml
<tr th:each="prod : ${prods}">
    <td th:text="${prod.name}">Onions</td>
    <td th:text="${prod.price}">2.41</td>
    <td th:text="${prod.inStock}? #{true} : #{false}">yes</td>
</tr>
```

可遍历的对象：
java.util.List、 java.util.Iterable、 java.util.Enumeration、java.util.Iterator、java.util.Map、任何数组、object（Object被看成是只包含自己的list）

## 3.2 遍历状态
* 当前遍历索引, 从0开始：index.
* 当前遍历索引, 从1开始：count .
* 被遍历对象包含的元素个数：size.
* 当前遍历元素：current.
* 表示当前遍历是偶数还是奇数.  even/odd 布尔值.
* 当前遍历元素是否是第一个元素：first 布尔值.
* 当前遍历元素是否是最后一个元素： last 布尔值.

使用方法：
```xml
 <tr th:each="prod,iterStat : ${prods}" th:class="${iterStat.odd}? 'odd'">
    <td th:text="${prod.name}">Onions</td>
    <td th:text="${prod.price}">2.41</td>
    <td th:text="${prod.inStock}? #{true} : #{false}">yes</td>
  </tr>
```

状态变量（在这个例子中是iterStat 变量）定义在遍历元素后面，用逗号隔开。

如果你不想显式声明状态变量，那么Thymeleaf会自动为你创建一个被遍历对象名 + Stat后缀的状态变量：
```xml
<tr th:each="prod : ${prods}" th:class="${prodStat.odd}? 'odd'">
  <td th:text="${prod.name}">Onions</td>
  <td th:text="${prod.price}">2.41</td>
  <td th:text="${prod.inStock}? #{true} : #{false}">yes</td>
</tr>
```

状态变量的作用域被限制在声明th:each的元素下。
# 4 通过惰性加载技术优化性能
某些时候我们会有这样的需求，就是当真正需要使用数据的时候，我们才从存放数据的地方（如数据库）获取数据。

Thymeleaf提供了数据惰性加载的机制，要求上下文变量需要实现<span> </span><code class="cye-lm-tag">ILazyContextVariable</code>接口。可以使用Thymeleaf实现的<code class="cye-lm-tag">LazyContextVariable</code><span> 类来实现：</span>

```java
context.setVariable(
     "users",
     new LazyContextVariable<List<User>>() {
         @Override
         protected List<User> loadValue() {
             return databaseRepository.findAllUsers();
         }
     });
```

然后在模板里我们这样使用：
```xml
<ul th:if="${condition}">
  <li th:each="u : ${users}" th:text="${u.name}">user name</li>
</ul>
```

当${condition}为true的时候Thymeleaf才会去获取真实的数据，否则users数据将不会被加载。

---
# 第四部分 条件取值
# 5 条件取值
## 5.1 使用th:if 和 <span>th:unless</span>
我们可能希望只有在if条件为真时，才显示代码模板。这时可以使用th:if。来看一下下面的模板：
```xml
<tr th:each="prod : ${prods}" th:class="${prodStat.odd}? 'odd'">
    <td th:text="${prod.name}">Onions</td>
    <td th:text="${prod.price}">2.41</td>
    <td th:text="${prod.inStock}? #{true} : #{false}">yes</td>
    <td>
      <span th:text="${#lists.size(prod.comments)}">2</span> comment/s
      <a href="comments.html" 
         th:href="@{/product/comments(prodId=${prod.id})}" 
         th:if="${not #lists.isEmpty(prod.comments)}">view</a>
    </td>
</tr>
```

上面的HTML代码中&lt;a&gt;元素只有在th:if判断为真时，才会显示。上面模板最终输出的结果如下：

```xml
<tr class="odd">
    <td>Italian Tomato</td>
    <td>1.25</td>
    <td>no</td>
    <td>
      <span>2</span> comment/s
      <a href="/gtvg/product/comments?prodId=2">view</a>
    </td>
</tr>
<tr>
    <td>Yellow Bell Pepper</td>
    <td>2.50</td>
    <td>yes</td>
    <td>
      <span>0</span> comment/s
    </td>
</tr>
<tr class="odd">
    <td>Old Cheddar</td>
    <td>18.75</td>
    <td>yes</td>
    <td>
      <span>1</span> comment/s
      <a href="/gtvg/product/comments?prodId=4">view</a>
    </td>
</tr>
```
那么有哪些条件下th:if判断为真呢？
* 值不为空，if条件为真:
  * If value is a boolean and is true.
  * If value is a number and is non-zero
  * If value is a character and is non-zero
  * If value is a String and is not “false”, “off” or “no”
  * If value is not a boolean, a number, a character or a String.
* 值为空，if条件为假

和th:if相对，th:unless也可以用来条件判断。那么上面的代码就可以改成：
```xml
<a href="comments.html"
   th:href="@{/comments(prodId=${prod.id})}" 
   th:unless="${#lists.isEmpty(prod.comments)}">view</a>
```

也就是说，&lt;a&gt;元素会一直显示，除非th:unless条件为真。


## 5.2 使用th:switch
和其他编程语言类似，Thymeleaf提供了switch条件判断：
```xml
<div th:switch="${user.role}">
  <p th:case="'admin'">User is an administrator</p>
  <p th:case="#{roles.manager}">User is a manager</p>
  <p th:case="*">User is some other thing</p>
</div>
```
<code>th:case="*"</code>代表默认值

---
# 第六部分 模板布局
# 6. 模板布局
## 6.1 引用代码片段
### 6.1.1 定义和引用代码片段
有时候我们想从其他的模板文件引入代码，这时候可以使用th:fragment来实现。

首先创建一个<span>/WEB-INF/templates/footer.html文件，然后文件内容如下：</span>

```html
<!DOCTYPE html>

<html xmlns:th="http://www.thymeleaf.org">

  <body>
  
    <div th:fragment="copy">
      &copy; 2011 The Good Thymes Virtual Grocery
    </div>
  
  </body>
  
</html>
```

上面的代码定义了一个名为copy的代码片段。在其他HTML文件中，我们可以使用th:insert或者th:replace属性来引用这段代码（也可以使用th:include，但是在Thymeleaf3.0中不推荐使用）：
```html
<body>

  ...

  <div th:insert="~{footer :: copy}"></div>
  
</body>
```

注意到~{footer :: copy}是一个<em class="cye-lm-tag">fragment<span> 表达式</span></em><span>，代表结果在一个fragment里。<em class="cye-lm-tag">fragment 表达式</em>也可以简写：</span>

```xml
<div th:insert="footer :: copy"></div>
```

### 6.1.2 Fragment语法规范
Fragment表达式有三种书写方式：
<ul>
 	<li><code class="cye-lm-tag">"~{templatename::selector}"</code><span> ：从名为templatename的模板文件中，引用被selector选择器选中的代码片段。selector选择器的使用和CSS选择器很相似，它的低层使用了AttoParser parsing，详细内容请看这里<a href="https://www.thymeleaf.org/doc/tutorials/3.0/usingthymeleaf.html#appendix-c-markup-selector-syntax">Appendix C</a></span></li>
 	<li><code class="cye-lm-tag">"~{templatename}"</code><span> 引用模板名为template的全部内容</span></li>
 	<li><code class="cye-lm-tag">"~{::selector}"</code><span> or </span><code>"~{this::selector}"</code>引用同一个模板文件中的代码片段。当selector找不到匹配项时，模板引擎会搜索从模板根目录搜索，直到找到匹配项位置。</li>
</ul>
Fragment表达式还可以和其他的语法组合，构成功能更加丰富的语句：
```xml
<div th:insert="footer :: (${user.isAdmin}? #{footer.admin} : #{footer.normaluser})"></div>
```

代码模板里可以使用任何th:\*的属性，当代码模板插入到其他模板文件中时，所有的th:\*属性都会被解析。

### 6.1.3 不用th:fragment引用代码片段
由于强大的<span> Markup Selectors的支持，我们可以不用th:fragment属性就可以引用代码片段，甚至是从其他没有使用Thymeleaf的应用里引用代码。看下面的例子：</span>

```xml
...
<div id="copy-section">
  &copy; 2011 The Good Thymes Virtual Grocery
</div>
...
```

我们可以通过引用div的id属性来引用这片代码，选择器的写法和CSS选择器一样：

```xml
<body>

  ...

  <div th:insert="~{footer :: #copy-section}"></div>
  
</body>
```

### 6.1.4 th:insert和th:replace的不同
* th:insert 插入标有th:insert的元素，作为其子元素.
* th:replace 替换标有th:replace的元素
* th:include和th:insert类似, 但是它仅仅是把fragment代码片段的文本内容插入进来.

看下面的例子，比较不同：

首先有一个名为copy的fragment
```xml
<footer th:fragment="copy">
  &copy; 2011 The Good Thymes Virtual Grocery
</footer>
```
然后引用：

```xml
<body>
  ...

  <div th:insert="footer :: copy"></div>

  <div th:replace="footer :: copy"></div>

  <div th:include="footer :: copy"></div>
  
</body>
```

最终结果：
```xml
<body>
  ...

  <div>
    <footer>
      &copy; 2011 The Good Thymes Virtual Grocery
    </footer>
  </div>

  <footer>
    &copy; 2011 The Good Thymes Virtual Grocery
  </footer>

  <div>
    &copy; 2011 The Good Thymes Virtual Grocery
  </div>
  
</body>
```

## 6.2 参数化fragment签名
为了创建函数化的fragment，可以给th:fragment添加一系列参数：
```xml
<div th:fragment="frag (onevar,twovar)">
    <p th:text="${onevar} + ' - ' + ${twovar}">...</p>
</div>
```

使用的时候可以这样：
```xml
<div th:replace="::frag (${value1},${value2})">...</div>
<div th:replace="::frag (onevar=${value1},twovar=${value2})">...</div>
```

注意，按照下面的写法，参数顺序不重要，同样和上面的写法等效：
```xml
<div th:replace="::frag (twovar=${value2},onevar=${value1})">...</div>
```

### 6.2.1 fragment本地变量写法
即使是没有定义参数，也可以网frag里传入变量。如：
```xml
<div th:fragment="frag">
    ...
</div>
```

可以用第二种语法（只能用第二种语法）来传参：
```xml
<div th:replace="::frag (onevar=${value1},twovar=${value2})">
```

这样即使frag后面没有参数签名，frag内部同样可以使用onevar和twovar这两个变量。这种写法和下面的写法是等效的：
```xml
<div th:replace="::frag" th:with="onevar=${value1},twovar=${value2}">
```

### 6.2.2 th:assert断言
使用th:assert对表达式集合进行断言，表达式集合用逗号隔开。每个表达式的结果必须为真，否则会抛出异常：
```xml
<div th:assert="${onevar},(${twovar} != 43)">...</div>
```
这对于在片段签名中验证参数非常方便：
```xml
<header th:fragment="contentheader(title)" th:assert="${!#strings.isEmpty(title)}">...</header>
```

## 6.3 灵活布局：不仅仅是fragment插入
由于强大的fragment表达式的支持，我们可以把数字、bean对象等等设置为fragment的参数，即使不写参数签名都可以。

这大大增强了模板的灵活性。举个例子，有这样一个fragment:
```xml
<head th:fragment="common_header(title,links)">

  <title th:replace="${title}">The awesome application</title>

  <!-- Common styles and scripts -->
  <link rel="stylesheet" type="text/css" media="all" th:href="@{/css/awesomeapp.css}">
  <link rel="shortcut icon" th:href="@{/images/favicon.ico}">
  <script type="text/javascript" th:src="@{/sh/scripts/codebase.js}"></script>

  <!--/* Per-page placeholder for additional links */-->
  <th:block th:replace="${links}" />

</head>
```

我们在另一个模板里通过th:replace来使用common_header模板：

```xml
...
<head th:replace="base :: common_header(~{::title},~{::link})">

  <title>Awesome - Main</title>

  <link rel="stylesheet" th:href="@{/css/bootstrap.min.css}">
  <link rel="stylesheet" th:href="@{/themes/smoothness/jquery-ui.css}">

</head>
...

```

上面common_header(\~{::title},\~{::link})中，\~{::title}表达式选择了当前模板中的&lt;title&gt;元素，~{::link}表达式选中了当前模板中的所有&lt;link&gt;元素，最终结果是：
```xml
...
<head>

  <title>Awesome - Main</title>

  <!-- Common styles and scripts -->
  <link rel="stylesheet" type="text/css" media="all" href="/awe/css/awesomeapp.css">
  <link rel="shortcut icon" href="/awe/images/favicon.ico">
  <script type="text/javascript" src="/awe/sh/scripts/codebase.js"></script>

  <link rel="stylesheet" href="/awe/css/bootstrap.min.css">
  <link rel="stylesheet" href="/awe/themes/smoothness/jquery-ui.css">

</head>
...
```
### 6.3.1 使用空的fragment
~{}表示空的fragment。比如使用下面的模板：
```html
<head th:replace="base :: common_header(~{::title},~{})">

  <title>Awesome - Main</title>

</head>
...
```

其最终结果如下：

```xml
...
<head>

  <title>Awesome - Main</title>

  <!-- Common styles and scripts -->
  <link rel="stylesheet" type="text/css" media="all" href="/awe/css/awesomeapp.css">
  <link rel="shortcut icon" href="/awe/images/favicon.ico">
  <script type="text/javascript" src="/awe/sh/scripts/codebase.js"></script>

</head>
...
```

### 6.3.2 使用no-operation操作符
使用{_}让模板使用默认的代码。如下所示<span style="color: #ff0000;">模板一</span>：
```xml
...
<head th:replace="base :: common_header(_,~{::link})">

  <title>Awesome - Main</title>

  <link rel="stylesheet" th:href="@{/css/bootstrap.min.css}">
  <link rel="stylesheet" th:href="@{/themes/smoothness/jquery-ui.css}">

</head>
...
```
common_header(_,~{::link})里传入了no-operation操作符。然后看一下common_header模板里的title部分：
```xml
<title th:replace="${title}">The awesome application</title>
```

那么最终的结果是模板一全部被common_header模板替换，而common_header模板的title元素被保留，没有被替换为Awesome - Main。最终结果如下：
```xml
...
<head>

  <title>The awesome application</title>

  <!-- Common styles and scripts -->
  <link rel="stylesheet" type="text/css" media="all" href="/awe/css/awesomeapp.css">
  <link rel="shortcut icon" href="/awe/images/favicon.ico">
  <script type="text/javascript" src="/awe/sh/scripts/codebase.js"></script>

  <link rel="stylesheet" href="/awe/css/bootstrap.min.css">
  <link rel="stylesheet" href="/awe/themes/smoothness/jquery-ui.css">

</head>
...
```

### 6.3.3 fragment的更高级的条件断言
考虑这样的条件，当用户是管理员的时候我们插入adminhead模板，否则就插入空的内容。那么我们可以这样做：
```xml
<div th:insert="${user.isAdmin()} ? ~{common :: adminhead} : ~{}">...</div>
```

同样地，如果用户是管理员我们就插入adminhead模板，否则什么都不做：
```xml
...
<div th:insert="${user.isAdmin()} ? ~{common :: adminhead} : _">
    Welcome [[${user.name}]], click <a th:href="@{/support}">here</a> for help-desk support.
</div>
...
```

我们还可以判断fragment存不存在：
```xml
...
<!-- The body of the <div> will be used if the "common :: salutation" fragment  -->
<!-- does not exist (or is empty).                                              -->
<div th:insert="~{common :: salutation} ?: _">
    Welcome [[${user.name}]], click <a th:href="@{/support}">here</a> for help-desk support.
</div>
...
```
## 6.4 移除代码
被th:remove=“*”标记的元素会在Thymeleaf引擎渲染后移除。*的内容如下：
<ul class="cye-lm-tag">
 	<li class="cye-lm-tag"><code>all</code>: Remove both the containing tag and all its children.</li>
 	<li><code>body</code>: Do not remove the containing tag, but remove all its children.</li>
 	<li><code>tag</code>: Remove the containing tag, but do not remove its children.</li>
 	<li class="cye-lm-tag"><code class="cye-lm-tag">all-but-first</code>: Remove all children of the containing tag except the first one.</li>
 	<li class="cye-lm-tag"><code>none</code><span> </span>: Do nothing. This value is useful for dynamic evaluation.</li>
</ul>
th:remove也可以使用条件判断：
```xml
<a href="/something" th:remove="${condition}? tag : none">Link text not to be removed</a>
```

th:remove把null当做none的同义词：
```xml
<a href="/something" th:remove="${condition}? tag">Link text not to be removed</a>
```
"${condition}? tag"中，条件如果为假，返回值是null，所以上面的例子是合法的。

## 6.5 布局继承
我们可以写一个名为layoutFile的模板，声明一个名为layout的fragment，然后把这个layoutFile当做页面的原型。
```html
<!DOCTYPE html>
<html th:fragment="layout (title, content)" xmlns:th="http://www.thymeleaf.org">
<head>
    <title th:replace="${title}">Layout Title</title>
</head>
<body>
    <h1>Layout H1</h1>
    <div th:replace="${content}">
        <p>Layout content</p>
    </div>
    <footer>
        Layout footer
    </footer>
</body>
</html>
```

然后使用下面的替换来继承原型的内容（有点像编程语言继承的味道）：
```html
<!DOCTYPE html>
<html th:replace="~{layoutFile :: layout(~{::title}, ~{::section})}">
<head>
    <title>Page Title</title>
</head>
<body>
<section>
    <p>Page content</p>
    <div>Included on page</div>
</section>
</body>
</html>
```

最终的结果是，上面的模板的整个html元素都被原型替代，只不过，原型的title和content则会被上面的&lt;title&gt;Page Title&lt;/title&gt;和&lt;section&gt;元素的内容替换。

--- 
# 第七部分 本地变量和属性优先级
# 7 本地变量
## 7.1 定义本地变量
Thymeleaf允许定义本地变量，本地变量的作用域被限制在申明变量的标签内部。先看一个例子：
```xml
<tr th:each="prod : ${prods}">
    ...
</tr>
```

上面的prod就是本地变量，它的作用域如下：
<ul>
 	<li>在&lt;tr&gt;标签上，只要是优先级低于<code>th:each</code>的<code>th:*</code><span> 属性都可以使用prod变量</span><span></span></li>
 	<li>所有&lt;tr&gt;标签的子元素都可以使用prod变量。</li>
</ul>
由于涉及到th:*属性的优先级问题，所以在申明本地变量的标签上使用时要小心了。

除了使用<code>th:each</code>，最通用的申明本地变量的属性是<code>th:with</code>：
```xml
<div th:with="firstPer=${persons[0]},secondPer=${persons[1]}">
  <p>
    The name of the first person is <span th:text="${firstPer.name}">Julius Caesar</span>.
  </p>
  <p>
    But the name of the second person is 
    <span th:text="${secondPer.name}">Marcus Antonius</span>.
  </p>
</div>
```

<code>th:each</code>属性支持在申明变量时使用已经定义的变量：
```xml
<div th:with="company=${user.company + ' Co.'},account=${accounts[company]}">...</div>
注意company变量的复用
```

## 7.2 使用本地变量
```xml
<p>
  Today is: 
  <span th:with="df=#{date.format}" 
        th:text="${#calendars.format(today,df)}">13 February 2011</span>
</p>
```

上面的例子中定义了本地变量df，在th:text属性中使用了它。注意：由于th:with属性的优先级高于th:text属性，所以可以在同一个&lt;span&gt;里使用。
# 属性优先级
| Order | Feature                         | Attributes                           |
| ----- | ------------------------------- | ------------------------------------ |
| 1     | Fragment inclusion              | th:insert th:replace                 |
| 2     | Fragment iteration              | th:each                              |
| 3     | Conditional evaluation          | th:if th:unless th:switch th:case    |
| 4     | Local variable definition       | th:object th:with                    |
| 5     | General attribute modification  | th:attr th:attrprepend th:attrappend |
| 6     | Specific attribute modification | th:value th:href th:src ...          |
| 7     | Text (tag body modification)    | th:text th:utext                     |
| 8     | Fragment specification          | th:fragment                          |
| 9     | Fragment removal                | th:remove                            |

# 第八部分 注释和Blocks
# 8 Comments and Blocks
## 8.1 标准的HTML/XML注释
标准的HTML/XML注释是<span>&lt;!-- ... --&gt;</span>
```xml
<!-- User info follows 这是注释 -->
<div th:text="${...}">
  ...
</div>
```

## 8.2 Thymeleaf解析级别（parser-level ）的注释
parser-level级别的注释，只能被Thymeleaf在解析模板的时候移除。它是由<code>&lt;!--/*</code><span> and </span><code>*/--&gt;</code>包裹起来的，Thymeleaf会把所有内容包括<code>&lt;!--/*</code><span> and </span><code>*/--&gt;</code>移除掉：
```xml
<!--/* This code will be removed at Thymeleaf parsing time! */-->
```

既然被包裹的内容会被移除，那为什么还要用这个注释呢？

考虑这样的场景，当我们仅仅只是打开一个静态的HTML页面，不经过Thymeleaf渲染，为了保持设计内容正常显示我们可以这样使用parser-level注释：
```xml
<!--/*--> 
  <div>
     you can see me only before Thymeleaf processes me!
  </div>
<!--*/-->
```

上面的代码在没有经过Thymeleaf渲染的情况下，依然能够正常显示。

所以举个例子的话，我们可这样使用：
```xml
<table>
   <tr th:each="x : ${xs}">
     ...
   </tr>
   <!--/*-->
   <tr>
     ...
   </tr>
   <tr>
     ...
   </tr>
   <!--*/-->
</table>
```

## 8.3 原型注释prototype-only comment
原型注释：只在原型里是注释，被解析后就不是注释。

prototype-only类型的注释使用<code>&lt;!--/*/</code><span> and </span><code>/*/--&gt;</code><span> 标签包裹，</span>在不使用Thymeleaf渲染的情况下直接打开HTML，注释内容依然是注释，而使用Thymeleaf渲染后，注释的内容得到保留，注释标签被移除，就像下面的例子。

静态页面：
```xml
<span>hello!</span>
<!--/*/
  <div th:text="${...}">
    ...
  </div>
/*/-->
<span>goodbye!</span>
```

经过Thymeleaf解析后的页面：
```xml
<span>hello!</span>
 
  <div th:text="${...}">
    ...
  </div>
 
<span>goodbye!</span>
```

这种诠释的设计应该是考虑到，在页面原型设计的时候，原型使用了一些HTML不支持的属性，所以在前后端分离的时候，为了不影响原型页面的展现将这些HTML不兼容的内容用原型级注释标签注释。
## 8.4 th:block标签
th:block是Thymeleaf里唯一的元素级处理器。它只是一个属性的容器，允许程序员在th:block标签上定义任何属性。经过Thymeleaf渲染后，th:block标签会被拿掉，但它的内容不会。

看下面的例子：
```xml
<table>
  <th:block th:each="user : ${users}">
    <tr>
        <td th:text="${user.login}">...</td>
        <td th:text="${user.name}">...</td>
    </tr>
    <tr>
        <td colspan="2" th:text="${user.address}">...</td>
    </tr>
  </th:block>
</table>
```

经过渲染后th:block标签会被去掉，去掉后整个模板就是HTML合法的了。但是不经过渲染，那么模板就不是HTML合法了。所以为了使模板合法，我们可以这样使用：
```xml
<table>
    <!--/*/ <th:block th:each="user : ${users}"> /*/-->
    <tr>
        <td th:text="${user.login}">...</td>
        <td th:text="${user.name}">...</td>
    </tr>
    <tr>
        <td colspan="2" th:text="${user.address}">...</td>
    </tr>
    <!--/*/ </th:block> /*/-->
</table>
```

---
# 第八部分 代码嵌入

# 7 代码嵌入
## 7.1 内联表达式
使用内联表达式<code>[[...]]</code><span> or </span><code>[(...)]</code>往文本内容中嵌入表达式，它们的功能分别和<code>th:text</code><span> or </span><code>th:utext</code><span> 一样，可以往内联表达式内部使用任何可以在<code>th:text</code> 和 <code>th:utext</code>里使用的属性。</span><span></span>

<code>[[...]]</code>：和th:text一致，它对文本内容进行转义

<code>[(...)]</code>：和th:utext一致，不对文本内容进行转义

来比较以下它们的不同：

```xml
如果msg='This is <b>great!</b>'

<p>The message is "[(${msg})]"</p>
的结果是：
<p>The message is "This is <b>great!</b>"</p>

<p>The message is "[[${msg}]]"</p>
的结果是：
<p>The message is "This is &lt;b&gt;great!&lt;/b&gt;"</p>
```

### 7.1.1 内联表达式 VS 普通模板
内联表达式，如<code>[(...)]</code>虽然它的代码量比使用th:utext这种普通模板少，而且它们实现同样的功能，但是从网页原型设计的角度考虑，使用普通模板比使用内联表达式更能清晰地、高还原地表达原型设计。

使用普通模板属性，即使是不经过Thymeleaf渲染，它依然能准确地显示原型内容。但是使用内联表达式，你看到的将会是这样的：
```
Hello, [[${session.user.name}]]!
```

所以，你可以看到这两者的区别了吧。
### 7.1.3 禁用内联表达式
使用th:inline="none"禁止Thymeleaf解析内联表达式：
```xml
<p th:inline="none">A double array looks like this: [[1, 2, 3], [4, 5]]!</p>
```

结果如下：
```xml
<p>A double array looks like this: [[1, 2, 3], [4, 5]]!</p>
```

## 7.2 Text inlining
Text inlining模式需要显式申明th:inline="text"。其内容下一节讨论
## 7.3 JavaScript inlining
JavaScript内联<span>允许在HTML模板模式中集成JavaScript脚本块。</span>

和text内联一样，JavaScript内联其实就是在JavaScript模板模式下对脚本内容进行处理（针对JavaScript语法进行表达式解析），因此下一节即将讲到的<em>textual template modes</em>的功能对于JavaScript inlining也是可用的。然而这一节我们先来看看怎么在JavaScript代码块中使用表达式。

首先显式申明<code>th:inline="javascript"</code><span>:</span>
```xml
<script th:inline="javascript">
    ...
    var username = [[${session.user.name}]];
    ...
</script>
```

经过解析后的代码：
```xml
<script th:inline="javascript">
    ...
    var username = "Sebastian \"Fruity\" Applejuice";
    ...
</script>
```

从上面的代码可以发现：

<ul>
 	<li>第一，JavaScript内联不仅解析了需要的文本内容，还使用分号对代码进行了正确的转义。</li>
 	<li>第二，之所以会进行转义，是因为我们使用了<code>[[...]]</code>表达式，而如果使用<code>[(...)]</code>表达式，那么结果将如下：</li>
</ul>
```xml
<script th:inline="javascript">
    ...
    var username = Sebastian "Fruity" Applejuice;
    ...
</script>
```
可以看到，表达式的内容虽然解析了，但是解析后的JavaScript代码是不合法的，没有进行正确的转义。
### 7.3.1 JavaScript natural templates
我们可以把内联表达式注释起来使用，如下：
```xml
<script th:inline="javascript">
    ...
    var username = /*[[${session.user.name}]]*/ "Gertrud Kiwifruit";
    ...
</script>
```

Thymeleaf会把这个注释后面的，分号前面的内容删除，也就是把"Gertrud Kiwifruit"删除，最后解析结果如下：
```xml
<script th:inline="javascript">
    ...
    var username = "Sebastian \"Fruity\" Applejuice";
    ...
</script>
```

这样做的好处是，即使不经过Thymeleaf解析，原始的JavaScript代码模板也是合法的。

### 7.3.1 高级内联取值和JavaScript序列化
JavaScript的表达式求值并不仅仅局限在String类型上，其他类型的对象也能正确地转换，Thymeleaf支持的对象类型如下：
* Strings
* Numbers
* Booleans
* Arrays
* Collections
* Maps
* Beans (objects with getter and setter methods)

举个例子，如果使用下面的代码：
```xml
<script th:inline="javascript">
    ...
    var user = /*[[${session.user}]]*/ null;
    ...
</script>
```

<code>${session.user}</code>表达式会正确地对User对象进行求值，然后转换为JavaScript语法格式的内容：
```xml
<script th:inline="javascript">
    ...
    var user = {"age":null,"firstName":"John","lastName":"Apricot",
                "name":"John Apricot","nationality":"Antarctica"};
    ...
</script>
```

JavaScript模板模式下表达式的这种序列化能力由<code>org.thymeleaf.standard.serializer.IStandardJavaScriptSerializer</code>接口的实现类实现，可以通过模板引擎的<code>StandardDialect</code>对象的属性来进行配置。

默认的JS序列化使用了<a href="https://github.com/FasterXML/jackson">Jackson library</a>。但是如果classpath路径里没有Jackson，那么会使用内置的序列化机制来进行转换。
## 7.4 CSS inlining
在style标签里使用内联表达式：
```xml
classname = 'main elems'
align = 'center'
代码中这样使用：
<style th:inline="css">
    .[[${classname}]] {
      text-align: [[${align}]];
    }
</style>
```

最终解析后的结果：
```xml
<style th:inline="css">
    .main\ elems {
      text-align: center;
    }
</style>
```

可以看到，<code>\[\[${classname}]]</code>针对CSS进行了转义，把classname = 'main elems'转换成了main\ elems

### 7.4.1 CSS natural templates
通过注释来使用内联表达式，就像之前JavaScript中的内联表达式一样：
```xml
<style th:inline="css">
    .main\ elems {
      text-align: /*[[${align}]]*/ left;
    }
</style>
```

---
# 第九部分 文本模板的模式
# 8 文本模板的模式
## 8.1 文本型语法
在Thymeleaf中支持文本型语法的三种模型是：<code>TEXT</code><span>, </span><code>JAVASCRIPT</code>和<span> </span><code>CSS</code>。同时还有两种与它们不同的模板，那就是支持标签语法的<code>HTML</code><span> 和 </span><code>XML</code>。

文本型语法和标签型语法的不同之处是，文本型语法中没有标签的支持来表述模板的逻辑，所以文本型语法和标签型语法依赖的解析机制是不同的。

文本型语法最基础的表述方法就是内联。内联在之前的章节其实已经讲到了，而且它非常适合直接在模板中插入文本，比如一个email的模板：
```txt
Dear [(${name})],

  Please find attached the results of the report you requested
  with name "[(${report.name})]".

  Sincerely,
    The Reporter.
```

但是为了实现更多更复杂的逻辑，我们需要一个非标签语法的支持：
```
[# th:each="item : ${items}"]
  - [(${item})]
[/]
```

其实上面的模板是下面模板的简化版本：
```
[#th:block th:each="item : ${items}"]
  - [#th:block th:utext="${item}" /]
[/th:block]
```

可以注意到我们使用了<code>[#element ...]</code><span> 这样的语法来代表一个元素。这个元素和XML标签语法很像，以 <code>[#element ...]</code>表示元素的开始，以<code>[/element]</code>来表示元素的结束，并且独立元素还可以最简化为<code>[#element ... /]</code>的形式，只用一个/来表示结束，和XML中的内联元素很像。</span>

标准的唯一可以用来包裹<code>element</code>的是<code>th:block</code>，虽然我们可以通过自定义来实现自己的新<code>element</code>，但是Thymeleaf默认就支持<code>th:block</code>元素，所以不管有没有自定义的元素，<code>th:block</code>元素（<code>[#th:block ...] ... [/th:block]</code>）都可以简写为如下的形式：

```
[# th:each="item : ${items}"]
  - [# th:utext="${item}" /]
[/]
```

观察上面的代码，[# th:utext="${item}" /]元素里只包含了一个内联的非转义属性th:utext，所以我们的代码可以进一步简化为：

```
[# th:each="item : ${items}"]
  - [(${item})]
[/]
```

注意文本型语法需要让每个元素必须有引用属性，而且元素闭合。所以和HTML语法相比，这种语法和XML更像。

接下来看一下<code>JAVASCRIPT</code>模板模式。注意，下面的代码是一个.js文件里的模板，而不是&lt;script&gt;代码块中的模板：
```js
var greeter = function() {

    var username = [[${session.user.name}]];

    [# th:each="salut : ${salutations}"]    
      alert([[${salut}]] + " " + username);
    [/]

};
```

经过Thymeleaf处理，最终模板如下：

```js
var greeter = function() {

    var username = "Bertrand \"Crunchy\" Pear";

      alert("Hello" + " " + username);
      alert("Ol\u00E1" + " " + username);
      alert("Hola" + " " + username);

};
```

### 8.1.1 转义的元素属性
为了避免与处在不同模式下的模板产生交互性影响（比如：在HTML模板里使用text内联语法，如果内联的表达式里使用了&lt;号，那么它会对HTML的结构产生影响），所以Thymeleaf 3.0 支持元素中的属性使用转义后的形式。
* **TEXT** 模板模式里的属性必须是HTML转义的.
* **JAVASCRIPT** 模板模式里属性必须是 JavaScript-转义的.
* **CSS** 模板模式里的属性必须是CSS转义的.

举个例子，下面是<code>TEXT</code>模式下的代码：
```
[# th:if="${120&lt;user.age}"]
     Congratulations!
[/]
```

注意到，代码中我们使用了<code>&lt;</code>的转义符<code>&amp;lt;</code>,虽然转义后的模板看起来不太顺眼，但是如果在设计页面原型的时候使用了这样的代码，那么即使在不用Thymeleaf支持的情况下直接打开HTML文件，浏览器也不会吧user.age误认为一个标签元素。

## 8.2 可扩展性
文本型语法同标签语法一样，同样支持扩展。开发者可以自定义自己的元素和属性，使用时可以套用下面的语法模板：
```
[#myorg:dosomething myorg:importantattr="211"]some text[/myorg:dosomething]
```

## 8.3 在注释中插入代码
<code>JAVASCRIPT</code>和<code>CSS</code>模板模式支持在注释语法<code>/*[+...+]*/</code> 里插入可被解析的代码（<code>TEXT</code>模式下不支持）。Thymeleaf会在处理模板的时候解除对代码的注释，使代码可用。比如下面的代码：

```js
var x = 23;

/*[+

var msg = "Hello, " + [[${session.user.name}]];

+]*/

var f = function() {
    ...

```

经过处理后：

```js
var x = 23;

var msg = "Hello, " + [[${session.user.name}]];

var f = function() {
...
```

## 8.4 parser-level注释块：移除代码
parser-level其意思就是说，代码内容在Thymeleaf解析的时候才会被当做是注释，然后删除它。这样的注释代码需要被包裹在<code>/*[- */</code><span> 和 </span><code>/* -]*/</code>符号内：<span></span>

```js
var x = 23;

/*[- */

var msg  = "This is shown only when executed statically!";

/* -]*/

var f = function() {
...
```

上面的msg只有在不使用Thymeleaf的情况下才有用。

在TEXT模式下：
```
...
/*[- Note the user is obtained from the session, which must exist -]*/
Welcome [(${session.user.name})]!
...
```

## 8.5 Natural JavaScript and CSS templates
可被解析的注释可以用来构造JavaScript和CSS友好型的代码，比如之前讲到的内联型语法：
```js
...
var username = /*[[${session.user.name}]]*/ "Sebastian Lychee";
...
```

解析后的结果是：
```js
...
var username = "John Apricot";
...
```

同样地，我们可以把这种技巧使用到整个文本型语法当中：
```
  /*[# th:if="${user.admin}"]*/
     alert('Welcome admin');
  /*[/]*/
```

上面的代码是完全符合JavaScript语法的，当经过Thymeleaf的解析后，如果用户是admin，那么和下面的代码是等价的：
```
  [# th:if="${user.admin}"]
     alert('Welcome admin');
  [/]
```

然而你可以发现，/\*[# th:if="${user.admin}"]\*/代码后面的，分号前面的部分——alert('Welcome admin')并没有被移除，原因是这种删除功能只有内联表达式有。

