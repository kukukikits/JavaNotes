<div class="titlepage">
<h2>跨站请求伪造相关内容摘自<a href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf">Spring Security</a>手册</h2>
<h2 class="title">19. Cross Site Request Forgery (CSRF)</h2>
</div>
This section discusses Spring Security’s <a class="ulink" href="https://en.wikipedia.org/wiki/Cross-site_request_forgery" target="_top">Cross Site Request Forgery (CSRF)</a> support.
<div class="section">
<div class="titlepage">
<div>
<div>
<h2 class="title"><a name="csrf-attacks" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-attacks"></a>19.1 CSRF Attacks</h2>
</div>
</div>
</div>
Before we discuss how Spring Security can protect applications from CSRF attacks, we will explain what a CSRF attack is. Let’s take a look at a concrete example to get a better understanding.
在讨论如何使用Spring Security防御CSRF攻击前，先说一说什么是CSRF攻击。让我们看看一个具体的例子来理解一下。

Assume that your bank’s website provides a form that allows transferring money from the currently logged in user to another bank account. For example, the HTTP request might look like:
假设你的银行网站提供了一个表单，让你把账户下的钱转到其他的银行账户上。举个例子：这个表单提交的HTTP请求可能和下面的类似：
<pre class="screen">POST /transfer HTTP/1.1
Host: bank.example.com
Cookie: JSESSIONID=randomid; Domain=bank.example.com; Secure; HttpOnly
Content-Type: application/x-www-form-urlencoded

amount=100.00&amp;routingNumber=1234&amp;account=9876</pre>
Now pretend you authenticate to your bank’s website and then, without logging out, visit an evil website. The evil website contains an HTML page with the following form:
现在假设你已经登陆了你的银行账户，但是没有登出，然后你访问了一个恶意的网站。这个恶意网站的HTML页面包含了下面的表单：
<pre class="programlisting"><span class="hl-tag">&lt;form</span> <span class="hl-attribute">action</span>=<span class="hl-value">"https://bank.example.com/transfer"</span> <span class="hl-attribute">method</span>=<span class="hl-value">"post"</span><span class="hl-tag">&gt;</span>
<span class="hl-tag">&lt;input</span> <span class="hl-attribute">type</span>=<span class="hl-value">"hidden"</span>
	<span class="hl-attribute">name</span>=<span class="hl-value">"amount"</span>
	<span class="hl-attribute">value</span>=<span class="hl-value">"100.00"</span><span class="hl-tag">/&gt;</span>
<span class="hl-tag">&lt;input</span> <span class="hl-attribute">type</span>=<span class="hl-value">"hidden"</span>
	<span class="hl-attribute">name</span>=<span class="hl-value">"routingNumber"</span>
	<span class="hl-attribute">value</span>=<span class="hl-value">"evilsRoutingNumber"</span><span class="hl-tag">/&gt;</span>
<span class="hl-tag">&lt;input</span> <span class="hl-attribute">type</span>=<span class="hl-value">"hidden"</span>
	<span class="hl-attribute">name</span>=<span class="hl-value">"account"</span>
	<span class="hl-attribute">value</span>=<span class="hl-value">"evilsAccountNumber"</span><span class="hl-tag">/&gt;</span>
<span class="hl-tag">&lt;input</span> <span class="hl-attribute">type</span>=<span class="hl-value">"submit"</span>
	<span class="hl-attribute">value</span>=<span class="hl-value">"Win Money!"</span><span class="hl-tag">/&gt;</span>
<span class="hl-tag">&lt;/form&gt;</span></pre>
You like to win money, so you click on the submit button. In the process, you have unintentionally transferred $100 to a malicious user. This happens because, while the evil website cannot see your cookies, the cookies associated with your bank are still sent along with the request.
你很可能想点击Win Money的按钮来赢钱，你经不住诱惑手贱点了这个按钮。接下来，你就会毫无防备地把自己的100美元转给了恶意用户。为什么会这样呢？这个恶意网站虽然看不到你的cookies，但是他仍然能把和你银行相关的cookies随着请求发出去。（没搞懂具体是怎么把cookies发出去的）

Worst yet, this whole process could have been automated using JavaScript. This means you didn’t even need to click on the button. So how do we protect ourselves from such attacks?
更蛋疼的是，这些操作可以直接使用JavaScript代码自动完成，根本就不需要你点那个赢钱的按钮。所以，我们怎么防止这样的攻击呢？？

</div>
<div class="section">
<div class="titlepage">
<div>
<div>
<h2 class="title"><a name="synchronizer-token-pattern" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#synchronizer-token-pattern"></a>19.2 Synchronizer Token Pattern令牌同步模式</h2>
</div>
</div>
</div>
The issue is that the HTTP request from the bank’s website and the request from the evil website are exactly the same. This means there is no way to reject requests coming from the evil website and allow requests coming from the bank’s website. To protect against CSRF attacks we need to ensure there is something in the request that the evil site is unable to provide.
一个重要的问题是银行网站的HTTP请求和恶意网站的HTTP请求是一模一样，就是说没有办法拒绝恶意网站的请求，同时允许银行的请求（也就是没法区分这两种请求）。所以为了阻止CSRF攻击，我们需要在请求里加一样恶意网站没办法提供的东西。

One solution is to use the <a class="ulink" href="https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)_Prevention_Cheat_Sheet#General_Recommendation:_Synchronizer_Token_Pattern" target="_top">Synchronizer Token Pattern</a>. This solution is to ensure that each request requires, in addition to our session cookie, a randomly generated token as an HTTP parameter. When a request is submitted, the server must look up the expected value for the parameter and compare it against the actual value in the request. If the values do not match, the request should fail.
一种解决方案就是令牌同步模式。这种解决方案要求每一个请求里，还有session cookie，都要包含一个随机生成的token作为HTTP参数。当请求提交时，服务器必须检查这个请求参数是否是期望的值，如果不是期望的值，那么这次请求就应该做失败处理。

We can relax the expectations to only require the token for each HTTP request that updates state. This can be safely done since the same origin policy ensures the evil site cannot read the response. Additionally, we do not want to include the random token in HTTP GET as this can cause the tokens to be leaked.
我们可以降低一下要求，仅仅给那些改变状态的HTTP请求添加token。由于同源策略的保护，恶意网站是无法读取响应的，所以我们可以安全地这样干。另外还有一点，不要使用GET请求，因为这样会把token暴露在url里。

Let’s take a look at how our example would change. Assume the randomly generated token is present in an HTTP parameter named _csrf. For example, the request to transfer money would look like this:
现在我们来看看怎么修改。假如随机生成的token设置在HTTP的_csrf参数里，那么这个HTTP请求就应该是这样的：
<pre class="screen">POST /transfer HTTP/1.1
Host: bank.example.com
Cookie: JSESSIONID=randomid; Domain=bank.example.com; Secure; HttpOnly
Content-Type: application/x-www-form-urlencoded

amount=100.00&amp;routingNumber=1234&amp;account=9876&amp;_csrf=&lt;secure-random&gt;</pre>
You will notice that we added the _csrf parameter with a random value. Now the evil website will not be able to guess the correct value for the _csrf parameter (which must be explicitly provided on the evil website) and the transfer will fail when the server compares the actual token to the expected token.
你可能已经注意到我们把随即值放在了_csrf参数里。现在恶意网站就没办法知道正确的_csrf值（恶意网站必须提供正确的_csrf值来进行伪装），那么它也就没办法成功伪装了。

</div>
<div class="section">
<div class="titlepage">
<div>
<div>
<h2 class="title"><a name="when-to-use-csrf-protection" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#when-to-use-csrf-protection"></a>19.3 When to use CSRF protection</h2>
</div>
</div>
</div>
When should you use CSRF protection? Our recommendation is to use CSRF protection for any request that could be processed by a browser by normal users. If you are only creating a service that is used by non-browser clients, you will likely want to disable CSRF protection.
我们应该什么时候使用对CSRF采取防御措施呢？我们建议只要是通过浏览器和普通用户处理的请求都应该采取CSRF防御措施。如果你用来创建服务的客户端不是浏览器的话，那你就可以关闭CSRF防御了。
<div class="section">
<div class="titlepage">
<div>
<div>
<h3 class="title"><a name="csrf-protection-and-json" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-protection-and-json"></a>19.3.1 CSRF protection and JSON</h3>
</div>
</div>
</div>
A common question is "do I need to protect JSON requests made by javascript?" The short answer is, it depends. However, you must be very careful as there are CSRF exploits that can impact JSON requests. For example, a malicious user can create a <a class="ulink" href="http://blog.opensecurityresearch.com/2012/02/json-csrf-with-parameter-padding.html" target="_top">CSRF with JSON using the following form</a>:
一个常见的问题是“我需要把javascript发起的JSON请求保护起来吗？”答案是看情况而定。然而你需要非常小心，一些操作可以利用CSRF漏洞的对JSON请求产生影响。举个例子，恶意用户可以使用下面的表单来伪造JSON数据
<pre class="programlisting"><span class="hl-tag">&lt;form</span> <span class="hl-attribute">action</span>=<span class="hl-value">"https://bank.example.com/transfer"</span> <span class="hl-attribute">method</span>=<span class="hl-value">"post"</span> <span class="hl-attribute">enctype</span>=<span class="hl-value">"text/plain"</span><span class="hl-tag">&gt;</span>
<span class="hl-tag">&lt;input</span> <span class="hl-attribute">name</span>=<span class="hl-value">'{"amount":100,"routingNumber":"evilsRoutingNumber","account":"evilsAccountNumber", "ignore_me":"'</span> <span class="hl-attribute">value</span>=<span class="hl-value">'test"}'</span> <span class="hl-attribute">type</span>=<span class="hl-value">'hidden'</span><span class="hl-tag">&gt;</span>
<span class="hl-tag">&lt;input</span> <span class="hl-attribute">type</span>=<span class="hl-value">"submit"</span>
	<span class="hl-attribute">value</span>=<span class="hl-value">"Win Money!"</span><span class="hl-tag">/&gt;</span>
<span class="hl-tag">&lt;/form&gt;</span></pre>
This will produce the following JSON structure上面的表单可以生成如下的JSON结构
<pre class="programlisting">{ <span class="hl-string">"amount"</span>: <span class="hl-number">100</span>,
<span class="hl-string">"routingNumber"</span>: <span class="hl-string">"evilsRoutingNumber"</span>,
<span class="hl-string">"account"</span>: <span class="hl-string">"evilsAccountNumber"</span>,
<span class="hl-string">"ignore_me"</span>: <span class="hl-string">"=test"</span>
}</pre>
If an application were not validating the Content-Type, then it would be exposed to this exploit. Depending on the setup, a Spring MVC application that validates the Content-Type could still be exploited by updating the URL suffix to end with ".json" as shown below:
如果应用没有检查Content-Type，那么它就存在这个漏洞。根据设置，如果URL的后缀是.json，那么Spring MVC应用即使对Content-Type进行了格式检查，同样存在这个漏洞。示例如下：
<pre class="programlisting"><span class="hl-tag">&lt;form</span> <span class="hl-attribute">action</span>=<span class="hl-value">"https://bank.example.com/transfer.json"</span> <span class="hl-attribute">method</span>=<span class="hl-value">"post"</span> <span class="hl-attribute">enctype</span>=<span class="hl-value">"text/plain"</span><span class="hl-tag">&gt;</span>
<span class="hl-tag">&lt;input</span> <span class="hl-attribute">name</span>=<span class="hl-value">'{"amount":100,"routingNumber":"evilsRoutingNumber","account":"evilsAccountNumber", "ignore_me":"'</span> <span class="hl-attribute">value</span>=<span class="hl-value">'test"}'</span> <span class="hl-attribute">type</span>=<span class="hl-value">'hidden'</span><span class="hl-tag">&gt;</span>
<span class="hl-tag">&lt;input</span> <span class="hl-attribute">type</span>=<span class="hl-value">"submit"</span>
	<span class="hl-attribute">value</span>=<span class="hl-value">"Win Money!"</span><span class="hl-tag">/&gt;</span>
<span class="hl-tag">&lt;/form&gt;</span></pre>
</div>
<div class="section">
<div class="titlepage">
<div>
<div>
<h3 class="title"><a name="csrf-and-stateless-browser-applications" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-and-stateless-browser-applications"></a>19.3.2 CSRF and Stateless Browser Applications跨站请求伪造和无状态浏览器应用</h3>
</div>
</div>
</div>
What if my application is stateless? That doesn’t necessarily mean you are protected. In fact, if a user does not need to perform any actions in the web browser for a given request, they are likely still vulnerable to CSRF attacks.
如果应用程序是无状态的，是不是意味着不需要进行CSRF防御呢？答案是同样需要。事实上，即使用户不对浏览器请求进行任何操作，同样容易受到跨站请求伪造攻击。

For example, consider an application uses a custom cookie that contains all the state within it for authentication instead of the JSESSIONID. When the CSRF attack is made the custom cookie will be sent with the request in the same manner that the JSESSIONID cookie was sent in our previous example.
举个例子，假如程序把所有身份验证相关的状态信息保存在自定义的cookie中，而不是保存在JSESSIONID里。那么当CSRF攻击发生时，这个自定义的cookie同样会随着请求发送出去（即使恶意网站看不到cookie的内容），这个先前的把JSESSIONID保存到cookie中例子一样。

Users using basic authentication are also vulnerable to CSRF attacks since the browser will automatically include the username password in any requests in the same manner that the JSESSIONID cookie was sent in our previous example.
用户使用简单的身份验证同样容易受到CSRF的攻击，因为浏览器一般会自动把用户名、密码附在所有请求上，和先前例子里的JSESSIONID cookie一样。

</div>
</div>
<div class="section">
<div class="titlepage">
<div>
<div>
<h2 class="title"><a name="csrf-using" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-using"></a>19.4 Using Spring Security CSRF Protection使用Spring Security CSRF防御</h2>
</div>
</div>
</div>
So what are the steps necessary to use Spring Security’s to protect our site against CSRF attacks? The steps to using Spring Security’s CSRF protection are outlined below:那么使用Spring Security来保护网站防御CSRF攻击的步骤有哪些呢？步骤如下：
<div class="itemizedlist">
<ul class="itemizedlist">
 	<li class="listitem"><a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-use-proper-verbs" title="19.4.1 Use proper HTTP verbs">Use proper HTTP verbs</a> 使用合适的HTTP动词</li>
 	<li class="listitem"><a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-configure" title="19.4.2 Configure CSRF Protection">Configure CSRF Protection</a> 配置CSRF保护</li>
 	<li class="listitem"><a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-include-csrf-token" title="19.4.3 Include the CSRF Token">Include the CSRF Token</a> 添加CSRF令牌</li>
</ul>
</div>
<div class="section">
<div class="titlepage">
<div>
<div>
<h3 class="title"><a name="csrf-use-proper-verbs" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-use-proper-verbs"></a>19.4.1 Use proper HTTP verbs 使用合适的HTTP动词</h3>
</div>
</div>
</div>
The first step to protecting against CSRF attacks is to ensure your website uses proper HTTP verbs. Specifically, before Spring Security’s CSRF support can be of use, you need to be certain that your application is using PATCH, POST, PUT, and/or DELETE for anything that modifies state.
防御CSRF的第一步就是使用合适的HTTP动词。尤其是在添加Spring Security CSRF支持前，你需要使用PATCH、POST、PUT和DELETE这些动词来修改应用状态。

This is not a limitation of Spring Security’s support, but instead a general requirement for proper CSRF prevention. The reason is that including private information in an HTTP GET can cause the information to be leaked. See <a class="ulink" href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec15.html#sec15.1.3" target="_top">RFC 2616 Section 15.1.3 Encoding Sensitive Information in URI’s</a> for general guidance on using POST instead of GET for sensitive information.
这不是Spring Security特有的规则，而是防御CSRF常用的措施。不能使用HTTP GET的原因是它会泄露信息。您可以在RFC这里了解详情。

</div>
<div class="section">
<div class="titlepage">
<div>
<div>
<h3 class="title"><a name="csrf-configure" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-configure"></a>19.4.2 Configure CSRF Protection配置CSRF保护</h3>
</div>
</div>
</div>
The next step is to include Spring Security’s CSRF protection within your application. Some frameworks handle invalid CSRF tokens by invaliding the user’s session, but this causes <a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-logout" title="19.5.3 Logging Out">its own problems</a>. Instead by default Spring Security’s CSRF protection will produce an HTTP 403 access denied. This can be customized by configuring the <a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#access-denied-handler" title="15.2.2 AccessDeniedHandler">AccessDeniedHandler</a> to process <code class="literal">InvalidCsrfTokenException</code> differently.
下一步就是添加Spring Security CSRF保护到程序里。<span>有些框架通过使用户会话无效来处理无效的CSRF令牌，但这样做有一些问题。Spring Security则是通过产生一个HTTP 403拒绝请求的方式来处理无效的CSRF令牌。可以通过配置<a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#access-denied-handler" title="15.2.2 AccessDeniedHandler">AccessDeniedHandler</a> 和使用<code class="literal">InvalidCsrfTokenException</code>来改变默认的配置。</span>

As of Spring Security 4.0, CSRF protection is enabled by default with XML configuration. If you would like to disable CSRF protection, the corresponding XML configuration can be seen below.
<span>在Spring Security 4.0中，使用XML配置时CSRF保护是默认开启的。如果你想关闭保护，可以使用如下的配置。</span>
<pre class="programlisting"><span class="hl-tag">&lt;http&gt;</span>
	<span class="hl-comment">&lt;!-- ... --&gt;</span>
	<span class="hl-tag">&lt;csrf</span> <span class="hl-attribute">disabled</span>=<span class="hl-value">"true"</span><span class="hl-tag">/&gt;</span>
<span class="hl-tag">&lt;/http&gt;</span></pre>
CSRF protection is enabled by default with Java Configuration. If you would like to disable CSRF, the corresponding Java configuration can be seen below. Refer to the Javadoc of csrf() for additional customizations in how CSRF protection is configured.
使用Java配置时，CSRF也是默认开启的。如果要关闭保护，参考下面的代码。更高级的配置请参考<span style="color: #ff0000;">Javadoc（官网提供的链接是失效的）</span>
<pre class="programlisting"><em><span class="hl-annotation">@EnableWebSecurity</span></em>
<span class="hl-keyword">public</span> <span class="hl-keyword">class</span> WebSecurityConfig <span class="hl-keyword">extends</span>
WebSecurityConfigurerAdapter {

	<em><span class="hl-annotation">@Override</span></em>
	<span class="hl-keyword">protected</span> <span class="hl-keyword">void</span> configure(HttpSecurity http) <span class="hl-keyword">throws</span> Exception {
		http
			.csrf().disable();
	}
}</pre>
</div>
<div class="section">
<div class="titlepage">
<div>
<div>
<h3 class="title"><a name="csrf-include-csrf-token" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-include-csrf-token"></a>19.4.3 Include the CSRF Token添加CSRF令牌</h3>
</div>
</div>
</div>
<div class="section">
<div class="titlepage">
<div>
<div>
<h4 class="title"><a name="csrf-include-csrf-token-form" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-include-csrf-token-form"></a>Form Submissions表单提交</h4>
</div>
</div>
</div>
The last step is to ensure that you include the CSRF token in all PATCH, POST, PUT, and DELETE methods. One way to approach this is to use the <code class="literal">_csrf</code> request attribute to obtain the current <code class="literal">CsrfToken</code>. An example of doing this with a JSP is shown below:
最后一步就是保证在所有PATCH、POST、PUT和DELETE方法中添加CSRF令牌。一种实现方式是使用<code class="literal">_csrf</code>请求属性来保存<code class="literal">CsrfToken</code>。在JSP页面中可以参考下面的方式：
<pre class="programlisting"><span class="hl-tag">&lt;c:url</span> <span class="hl-attribute">var</span>=<span class="hl-value">"logoutUrl"</span> <span class="hl-attribute">value</span>=<span class="hl-value">"/logout"</span><span class="hl-tag">/&gt;</span>
<span class="hl-tag">&lt;form</span> <span class="hl-attribute">action</span>=<span class="hl-value">"${logoutUrl}"</span>
	<span class="hl-attribute">method</span>=<span class="hl-value">"post"</span><span class="hl-tag">&gt;</span>
<span class="hl-tag">&lt;input</span> <span class="hl-attribute">type</span>=<span class="hl-value">"submit"</span>
	<span class="hl-attribute">value</span>=<span class="hl-value">"Log out"</span><span class="hl-tag"> /&gt;</span>
<span class="hl-tag">&lt;input</span> <span class="hl-attribute">type</span>=<span class="hl-value">"hidden"</span>
	<span class="hl-attribute">name</span>=<span class="hl-value">"${_csrf.parameterName}"</span>
	<span class="hl-attribute">value</span>=<span class="hl-value">"${_csrf.token}"</span><span class="hl-tag">/&gt;</span>
<span class="hl-tag">&lt;/form&gt;</span></pre>
An easier approach is to use <a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#the-csrfinput-tag" title="32.5 The csrfInput Tag">the csrfInput tag</a> from the Spring Security JSP tag library.
更简洁的方法是直接使用Spring Security JSP标签库里的<a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#the-csrfinput-tag" title="32.5 The csrfInput Tag">csrfInput</a>标签。
<div class="note">
<table border="0" summary="Note">
<tbody>
<tr>
<td rowspan="2" align="center" valign="top" width="25"><img alt="[Note]" src="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/images/note.png" /></td>
</tr>
<tr>
<td align="left" valign="top">If you are using Spring MVC <code class="literal">&lt;form:form&gt;</code> tag or <a class="ulink" href="http://www.thymeleaf.org/whatsnew21.html#reqdata" target="_top">Thymeleaf 2.1+</a> and are using <code class="literal">@EnableWebSecurity</code>, the <code class="literal">CsrfToken</code> is automatically included for you (using the <code class="literal">CsrfRequestDataValueProcessor</code>).
如果你使用的是Spring MVC <code class="literal">&lt;form:form&gt;</code>标签或者<a class="ulink" href="http://www.thymeleaf.org/whatsnew21.html#reqdata" target="_top">Thymeleaf 2.1+</a> ，并使用了<code class="literal">@EnableWebSecurity</code>，CSRF令牌已经自动帮你打包好了，直接使用<code class="literal">CsrfRequestDataValueProcessor</code>就行。</td>
</tr>
</tbody>
</table>
</div>
</div>
<div class="section">
<div class="titlepage">
<div>
<div>
<h4 class="title"><a name="csrf-include-csrf-token-ajax" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-include-csrf-token-ajax"></a>Ajax and JSON Requests</h4>
</div>
</div>
</div>
If you are using JSON, then it is not possible to submit the CSRF token within an HTTP parameter. Instead you can submit the token within a HTTP header. A typical pattern would be to include the CSRF token within your meta tags. An example with a JSP is shown below:
如果你使用的是JSON，那么没有办法可以把CSRF令牌放到HTTP参数里提交。但是你可以把令牌放到HTTP头部里然后提交请求。一种典型的做法是把CSRF令牌放到meta标签里。下面是一个JSP的例子
<pre class="programlisting"><span class="hl-tag">&lt;html&gt;</span>
<span class="hl-tag">&lt;head&gt;</span>
	<span class="hl-tag">&lt;meta</span> <span class="hl-attribute">name</span>=<span class="hl-value">"_csrf"</span> <span class="hl-attribute">content</span>=<span class="hl-value">"${_csrf.token}"</span><span class="hl-tag">/&gt;</span>
	<span class="hl-comment">&lt;!-- default header name is X-CSRF-TOKEN --&gt;</span>
	<span class="hl-tag">&lt;meta</span> <span class="hl-attribute">name</span>=<span class="hl-value">"_csrf_header"</span> <span class="hl-attribute">content</span>=<span class="hl-value">"${_csrf.headerName}"</span><span class="hl-tag">/&gt;</span>
	<span class="hl-comment">&lt;!-- ... --&gt;</span>
<span class="hl-tag">&lt;/head&gt;</span>
<span class="hl-comment">&lt;!-- ... --&gt;</span></pre>
Instead of manually creating the meta tags, you can use the simpler <a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#the-csrfmetatags-tag" title="32.6 The csrfMetaTags Tag">csrfMetaTags tag</a> from the Spring Security JSP tag library.
除了手动创建meta标签的方式外，你可以使用Spring Security JSP标签库里的<a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#the-csrfmetatags-tag" title="32.6 The csrfMetaTags Tag">csrfMetaTags</a>标签。

You can then include the token within all your Ajax requests. If you were using jQuery, this could be done with the following:
你可以把令牌放到所有的Ajax请求里。如果你用的是jQuery，那你可以这么干：
<pre class="programlisting">$(<span class="hl-keyword">function</span> () {
<span class="hl-keyword">var</span> token = $(<span class="hl-string">"meta[name='_csrf']"</span>).attr(<span class="hl-string">"content"</span>);
<span class="hl-keyword">var</span> header = $(<span class="hl-string">"meta[name='_csrf_header']"</span>).attr(<span class="hl-string">"content"</span>);
$(document).ajaxSend(<span class="hl-keyword">function</span>(e, xhr, options) {
	xhr.setRequestHeader(header, token);
});
});</pre>
As an alternative to jQuery, we recommend using <a class="ulink" href="http://cujojs.com/" target="_top">cujoJS’s</a> rest.js. The <a class="ulink" href="https://github.com/cujojs/rest" target="_top">rest.js</a> module provides advanced support for working with HTTP requests and responses in RESTful ways. A core capability is the ability to contextualize the HTTP client adding behavior as needed by chaining interceptors on to the client.
另一种替代jQuery的方式是，我们推荐的 <a class="ulink" href="http://cujojs.com/" target="_top">cujoJS</a>的rest.js。rest.js的模块提供了更高级一点的处理，支持对HTTP请求和响应以RESTful方式进行处理。一个核心的功能是，<span style="color: #ff0000;">通过链式拦截器来提供HTTP客户端的上下文处理能力（这句不知道怎么翻译）</span>。
<pre class="programlisting"><span class="hl-keyword">var</span> client = rest.chain(csrf, {
token: $(<span class="hl-string">"meta[name='_csrf']"</span>).attr(<span class="hl-string">"content"</span>),
name: $(<span class="hl-string">"meta[name='_csrf_header']"</span>).attr(<span class="hl-string">"content"</span>)
});</pre>
The configured client can be shared with any component of the application that needs to make a request to the CSRF protected resource. One significant difference between rest.js and jQuery is that only requests made with the configured client will contain the CSRF token, vs jQuery where <span class="emphasis"><em>all</em></span> requests will include the token. The ability to scope which requests receive the token helps guard against leaking the CSRF token to a third party. Please refer to the <a class="ulink" href="https://github.com/cujojs/rest/tree/master/docs" target="_top">rest.js reference documentation</a> for more information on rest.js.
这个client可以被前端应用的任何需要采取CSRF防御的组件共享。rest.js和jQuery的一个明显的不同是，rest.js只有经过配置的client生成的请求才会有CSRF令牌，而jQuery的所有请求都会包含令牌。这种能够管理哪个请求能拿到令牌的能力，能够防止CSRF令牌泄露给其他第三方组件。

</div>
<div class="section">
<div class="titlepage">
<div>
<div>
<h4 class="title"><a name="csrf-cookie" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-cookie"></a>CookieCsrfTokenRepository  CSRF令牌cookie仓库</h4>
</div>
</div>
</div>
There can be cases where users will want to persist the <code class="literal">CsrfToken</code> in a cookie. By default the <code class="literal">CookieCsrfTokenRepository</code> will write to a cookie named <code class="literal">XSRF-TOKEN</code> and read it from a header named <code class="literal">X-XSRF-TOKEN</code> or the HTTP parameter <code class="literal">_csrf</code>. These defaults come from<a class="ulink" href="https://docs.angularjs.org/api/ng/service/$http#cross-site-request-forgery-xsrf-protection" target="_top">AngularJS</a>
一些情况下程序员可能想把<code class="literal">CsrfToken</code> 持久化到cookie里。默认情况下<code class="literal">CookieCsrfTokenRepository</code>会在把令牌写到一个名为<code class="literal">XSRF-TOKEN</code>的cookie里，从名为<code class="literal">X-XSRF-TOKEN</code>的请求头或，名为<code class="literal">_csrf</code>的HTTP参数里读取令牌的值。这些默认操作都来自AngularJS

You can configure <code class="literal">CookieCsrfTokenRepository</code> in XML using the following:   XML配置如下：
<pre class="programlisting"><span class="hl-tag">&lt;http&gt;</span>
	<span class="hl-comment">&lt;!-- ... --&gt;</span>
	<span class="hl-tag">&lt;csrf</span> <span class="hl-attribute">token-repository-ref</span>=<span class="hl-value">"tokenRepository"</span><span class="hl-tag">/&gt;</span>
<span class="hl-tag">&lt;/http&gt;</span>
<span class="hl-tag">&lt;b:bean</span> <span class="hl-attribute">id</span>=<span class="hl-value">"tokenRepository"</span>
	<span class="hl-attribute">class</span>=<span class="hl-value">"org.springframework.security.web.csrf.CookieCsrfTokenRepository"</span>
	<span class="hl-attribute">p:cookieHttpOnly</span>=<span class="hl-value">"false"</span><span class="hl-tag">/&gt;</span></pre>
<div class="note">
<table border="0" summary="Note">
<tbody>
<tr>
<td rowspan="2" align="center" valign="top" width="25"><img alt="[Note]" src="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/images/note.png" /></td>
</tr>
<tr>
<td align="left" valign="top">The sample explicitly sets <code class="literal">cookieHttpOnly=false</code>. This is necessary to allow JavaScript (i.e. AngularJS) to read it. If you do not need the ability to read the cookie with JavaScript directly, it is recommended to omit <code class="literal">cookieHttpOnly=false</code> to improve security.
上面的例子设置了<code class="literal">cookieHttpOnly=false</code>。如果希望允许JavaScript读取cookie，那么这个设置是必须的。如果你不希望javaScript读取cookie里的令牌，那么不要设置<code class="literal">cookieHttpOnly=false</code>，这样可以提高安全性。</td>
</tr>
</tbody>
</table>
</div>
You can configure <code class="literal">CookieCsrfTokenRepository</code> in Java Configuration using: Java配置如下：
<pre class="programlisting"><em><span class="hl-annotation">@EnableWebSecurity</span></em>
<span class="hl-keyword">public</span> <span class="hl-keyword">class</span> WebSecurityConfig <span class="hl-keyword">extends</span>
		WebSecurityConfigurerAdapter {

	<em><span class="hl-annotation">@Override</span></em>
	<span class="hl-keyword">protected</span> <span class="hl-keyword">void</span> configure(HttpSecurity http) <span class="hl-keyword">throws</span> Exception {
		http
			.csrf()
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
	}
}</pre>
<div class="note">
<table border="0" summary="Note">
<tbody>
<tr>
<td rowspan="2" align="center" valign="top" width="25"><img alt="[Note]" src="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/images/note.png" /></td>
</tr>
<tr>
<td align="left" valign="top">The sample explicitly sets <code class="literal">cookieHttpOnly=false</code>. This is necessary to allow JavaScript (i.e. AngularJS) to read it. If you do not need the ability to read the cookie with JavaScript directly, it is recommended to omit <code class="literal">cookieHttpOnly=false</code> (by using<code class="literal">new CookieCsrfTokenRepository()</code> instead) to improve security.</td>
</tr>
</tbody>
</table>
</div>
</div>
</div>
</div>
<div class="section">
<div class="titlepage">
<div>
<div>
<h2 class="title"><a name="csrf-caveats" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-caveats"></a>19.5 CSRF Caveats 跨站请求伪造的陷阱</h2>
</div>
</div>
</div>
There are a few caveats when implementing CSRF.
<div class="section">
<div class="titlepage">
<div>
<div>
<h3 class="title"><a name="csrf-timeouts" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-timeouts"></a>19.5.1 Timeouts 超时</h3>
</div>
</div>
</div>
One issue is that the expected CSRF token is stored in the HttpSession, so as soon as the HttpSession expires your configured<code class="literal">AccessDeniedHandler</code> will receive a InvalidCsrfTokenException. If you are using the default <code class="literal">AccessDeniedHandler</code>, the browser will get an HTTP 403 and display a poor error message.
如果CSRF令牌保存在HTTPSession里，那么当HttpSession过期的时候，你配置的<code class="literal">AccessDeniedHandler</code>程序就会收到一个 InvalidCsrfTokenException异常。如果你使用了默认的<code class="literal">AccessDeniedHandler</code>，那么浏览器会收到一个403异常，并且显示一个错误消息。
<div class="note">
<table border="0" summary="Note">
<tbody>
<tr>
<td rowspan="2" align="center" valign="top" width="25"><img alt="[Note]" src="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/images/note.png" /></td>
</tr>
<tr>
<td align="left" valign="top">One might ask why the expected <code class="literal">CsrfToken</code> isn’t stored in a cookie by default. This is because there are known exploits in which headers (i.e. specify the cookies) can be set by another domain. This is the same reason Ruby on Rails <a class="ulink" href="http://weblog.rubyonrails.org/2011/2/8/csrf-protection-bypass-in-ruby-on-rails/" target="_top">no longer skips CSRF checks when the header X-Requested-With is present</a>. See <a class="ulink" href="http://lists.webappsec.org/pipermail/websecurity_lists.webappsec.org/2011-February/007533.html" target="_top">this webappsec.org thread</a> for details on how to perform the exploit. Another disadvantage is that by removing the state (i.e. the timeout) you lose the ability to forcibly terminate the token if it is compromised.
有人可能会问，为什么<code class="literal">CsrfToken</code>默认不是保存在cookie里的。因为有一些已知的漏洞可以在不同的域名下设置请求头部（也就是指定cookie）(这句话其实我也不懂)。这也是为什么当<a class="ulink" href="http://weblog.rubyonrails.org/2011/2/8/csrf-protection-bypass-in-ruby-on-rails/" target="_top"> X-Requested-With</a>请求头存在的情况下，X-Ruby on Rails也不跳过CSRF检查的原因。详情参考<a class="ulink" href="http://lists.webappsec.org/pipermail/websecurity_lists.webappsec.org/2011-February/007533.html" target="_top">webappsec.org</a>。移除状态（也就是超时）的缺点是，如果令牌被盗，那么你将没有办法强制销毁那个被盗的令牌。</td>
</tr>
</tbody>
</table>
</div>
A simple way to mitigate an active user experiencing a timeout is to have some JavaScript that lets the user know their session is about to expire. The user can click a button to continue and refresh the session.
一种改善的措施是通过一点JavaScript代码来告诉用户，你的会话将要过期了。用户可以通过点击按钮来刷新会话。

Alternatively, specifying a custom <code class="literal">AccessDeniedHandler</code> allows you to process the <code class="literal">InvalidCsrfTokenException</code> any way you like. For an example of how to customize the <code class="literal">AccessDeniedHandler</code> refer to the provided links for both <a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#nsa-access-denied-handler" title="43.1.3 &lt;access-denied-handler&gt;" style="color: #4183c4; text-decoration: none;">xml</a> and <a class="ulink" href="https://github.com/spring-projects/spring-security/blob/3.2.0.RC1/config/src/test/groovy/org/springframework/security/config/annotation/web/configurers/NamespaceHttpAccessDeniedHandlerTests.groovy#L64" target="_top">Java configuration</a>.
另一种处理方式是，自定义一个<code class="literal">AccessDeniedHandler</code>来处理<code class="literal">InvalidCsrfTokenException</code>异常。自定义<code class="literal">AccessDeniedHandler</code> 的例子看这两个超链接—— <a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#nsa-access-denied-handler" title="43.1.3 &lt;access-denied-handler&gt;" style="color: #4183c4; text-decoration: none;">xml</a>  和 <a class="ulink" href="https://github.com/spring-projects/spring-security/blob/3.2.0.RC1/config/src/test/groovy/org/springframework/security/config/annotation/web/configurers/NamespaceHttpAccessDeniedHandlerTests.groovy#L64" target="_top">Java configuration</a>

Finally, the application can be configured to use <a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-cookie" title="CookieCsrfTokenRepository">CookieCsrfTokenRepository</a> which will not expire. As previously mentioned, this is not as secure as using a session, but in many cases can be good enough.
最后程序可以配置一个不过期的<a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-cookie" title="CookieCsrfTokenRepository">CookieCsrfTokenRepository</a> 。就先前提到的，虽然这样配置没有使用session安全，但是对付大多数情况是足够了。

</div>
<div class="section">
<div class="titlepage">
<div>
<div>
<h3 class="title"><a name="csrf-login" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-login"></a>19.5.2 Logging In登录</h3>
</div>
</div>
</div>
In order to protect against <a class="ulink" href="https://en.wikipedia.org/wiki/Cross-site_request_forgery#Forging_login_requests" target="_top">forging log in requests</a> the log in form should be protected against CSRF attacks too. Since the <code class="literal">CsrfToken</code> is stored in HttpSession, this means an HttpSession will be created as soon as <code class="literal">CsrfToken</code> token attribute is accessed. While this sounds bad in a RESTful / stateless architecture the reality is that state is necessary to implement practical security. Without state, we have nothing we can do if a token is compromised. Practically speaking, the CSRF token is quite small in size and should have a negligible impact on our architecture.
为了防止登录请求伪造，使用CSRF防御来保护登录表单也是非常必要的。如果<code class="literal">CsrfToken</code> 是保存在HttpSession里的，那么就意味着<span>一旦访问了CsrfToken token属性，就会创建HttpSession。虽然在RESTful/无状态的架构里使用HttpSession的方式显得有些糟糕，但是为了实现安全，实际情况下保存状态是必要的。如果没有状态，当令牌被盗时我们什么都做不了。实际上，CSRF令牌非常小，对系统影响可以忽略。</span>

A common technique to protect the log in form is by using a JavaScript function to obtain a valid CSRF token before the form submission. By doing this, there is no need to think about session timeouts (discussed in the previous section) because the session is created right before the form submission (assuming that <a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-cookie" title="CookieCsrfTokenRepository">CookieCsrfTokenRepository</a> isn’t configured instead), so the user can stay on the login page and submit the username/password when he wants. In order to achieve this, you can take advantage of the <code class="literal">CsrfTokenArgumentResolver</code> provided by Spring Security and expose an endpoint like it’s described on <a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#mvc-csrf-resolver" title="39.5.2 Resolving the CsrfToken">here</a>.
一种常用的保护登录表单的方式是，在表单提交前使用JavaScript从后端获取CSRF令牌。这样就不用考虑session超时的问题了，因为session在表单提交前就已经创建了（这种操作是假设没有设置<a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-cookie" title="CookieCsrfTokenRepository">CookieCsrfTokenRepository</a> 时的一般处理方法）。这样，用户在任何时候都能使用登录页面和提交功能了。如果要使用这种方法，你需要使用<code class="literal">CsrfTokenArgumentResolver</code> ，并且暴露一个地址，详情参考这里<a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#mvc-csrf-resolver" title="39.5.2 Resolving the CsrfToken">here</a>

</div>
<div class="section">
<div class="titlepage">
<div>
<div>
<h3 class="title"><a name="csrf-logout" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-logout"></a>19.5.3 Logging Out登出</h3>
</div>
</div>
</div>
Adding CSRF will update the LogoutFilter to only use HTTP POST. This ensures that log out requires a CSRF token and that a malicious user cannot forcibly log out your users.
配置CSRF会更新LogoutFilter 只支持POST请求。这可以保证用户需要使用CSRF令牌才能登出，而恶意用户就不能强制把你登出了。

One approach is to use a form for log out. If you really want a link, you can use JavaScript to have the link perform a POST (i.e. maybe on a hidden form). For browsers with JavaScript that is disabled, you can optionally have the link take the user to a log out confirmation page that will perform the POST.
一种方法是使用表单登出。如果你想使用一个链接登出，你可以使用JavaScript使用链接url发出POST（也可以使用一个隐藏的表单实现）。对于浏览器禁用JavaScript的情况，你可以选择提供链接，让用户跳转到一个可以发出POST的确认页面来确认登出。

If you really want to use HTTP GET with logout you can do so, but remember this is generally not recommended. For example, the following Java Configuration will perform logout with the URL /logout is requested with any HTTP method:如果你真的想用GET请求登出，记住极不推荐。下面的配置允许所有/logout请求，不论使用什么HTTP动词。
<pre class="programlisting"><em><span class="hl-annotation">@EnableWebSecurity</span></em>
<span class="hl-keyword">public</span> <span class="hl-keyword">class</span> WebSecurityConfig <span class="hl-keyword">extends</span>
WebSecurityConfigurerAdapter {

	<em><span class="hl-annotation">@Override</span></em>
	<span class="hl-keyword">protected</span> <span class="hl-keyword">void</span> configure(HttpSecurity http) <span class="hl-keyword">throws</span> Exception {
		http
			.logout()
				.logoutRequestMatcher(<span class="hl-keyword">new</span> AntPathRequestMatcher(<span class="hl-string">"/logout"</span>));
	}
}</pre>
</div>
<div class="section">
<div class="titlepage">
<h3 class="title"><a name="csrf-multipart" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-multipart"></a></h3>
</div>
</div>
</div>]]></content:encoded>
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