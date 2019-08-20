<h2>跨站请求伪造相关内容摘自<a href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf">Spring Security</a>手册</h2>
<h2 class="title">19. Cross Site Request Forgery (CSRF)</h2>

This section discusses Spring Security’s <a class="ulink" href="https://en.wikipedia.org/wiki/Cross-site_request_forgery" target="_top">Cross Site Request Forgery (CSRF)</a> support.

## 19.1 [CSRF Attacks](https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-attacks)

Before we discuss how Spring Security can protect applications from CSRF attacks, we will explain what a CSRF attack is. Let’s take a look at a concrete example to get a better understanding.
在讨论如何使用Spring Security防御CSRF攻击前，先说一说什么是CSRF攻击。让我们看看一个具体的例子来理解一下。

Assume that your bank’s website provides a form that allows transferring money from the currently logged in user to another bank account. For example, the HTTP request might look like:
假设你的银行网站提供了一个表单，让你把账户下的钱转到其他的银行账户上。举个例子：这个表单提交的HTTP请求可能和下面的类似：
```text
POST /transfer HTTP/1.1
Host: bank.example.com
Cookie: JSESSIONID=randomid; Domain=bank.example.com; Secure; HttpOnly
Content-Type: application/x-www-form-urlencoded

amount=100.00&routingNumber=1234&account=9876
```
Now pretend you authenticate to your bank’s website and then, without logging out, visit an evil website. The evil website contains an HTML page with the following form:
现在假设你已经登陆了你的银行账户，但是没有登出，然后你访问了一个恶意的网站。这个恶意网站的HTML页面包含了下面的表单：
```html
<form action="https://bank.example.com/transfer" method="post">
<input type="hidden" name="amount" value="100.00">
<input type="hidden" name="routingNumber" value="evilsRoutingNumber">
<input type="hidden" name="account" value="evilsAccountNumber">
<input type="submit" value="Win Money!">
</form>
```

You like to win money, so you click on the submit button. In the process, you have unintentionally transferred $100 to a malicious user. This happens because, while the evil website cannot see your cookies, the cookies associated with your bank are still sent along with the request.
你很可能想点击Win Money的按钮来赢钱，你经不住诱惑手贱点了这个按钮。接下来，你就会毫无防备地把自己的100美元转给了恶意用户。为什么会这样呢？这个恶意网站虽然看不到你的cookies，但是他仍然能把和你银行相关的cookies随着请求发出去。（没搞懂具体是怎么把cookies发出去的）

Worst yet, this whole process could have been automated using JavaScript. This means you didn’t even need to click on the button. So how do we protect ourselves from such attacks?
更蛋疼的是，这些操作可以直接使用JavaScript代码自动完成，根本就不需要你点那个赢钱的按钮。所以，我们怎么防止这样的攻击呢？？

## 19.2 [Synchronizer Token Pattern令牌同步模式](https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#synchronizer-token-pattern)

The issue is that the HTTP request from the bank’s website and the request from the evil website are exactly the same. This means there is no way to reject requests coming from the evil website and allow requests coming from the bank’s website. To protect against CSRF attacks we need to ensure there is something in the request that the evil site is unable to provide.
一个重要的问题是银行网站的HTTP请求和恶意网站的HTTP请求是一模一样，就是说没有办法拒绝恶意网站的请求，同时允许银行的请求（也就是没法区分这两种请求）。所以为了阻止CSRF攻击，我们需要在请求里加一样恶意网站没办法提供的东西。

One solution is to use the <a class="ulink" href="https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)_Prevention_Cheat_Sheet#General_Recommendation:_Synchronizer_Token_Pattern" target="_top">Synchronizer Token Pattern</a>. This solution is to ensure that each request requires, in addition to our session cookie, a randomly generated token as an HTTP parameter. When a request is submitted, the server must look up the expected value for the parameter and compare it against the actual value in the request. If the values do not match, the request should fail.
一种解决方案就是令牌同步模式。这种解决方案要求每一个请求里，还有session cookie，都要包含一个随机生成的token作为HTTP参数。当请求提交时，服务器必须检查这个请求参数是否是期望的值，如果不是期望的值，那么这次请求就应该做失败处理。

We can relax the expectations to only require the token for each HTTP request that updates state. This can be safely done since the same origin policy ensures the evil site cannot read the response. Additionally, we do not want to include the random token in HTTP GET as this can cause the tokens to be leaked.
我们可以降低一下要求，仅仅给那些改变状态的HTTP请求添加token。由于同源策略的保护，恶意网站是无法读取响应的，所以我们可以安全地这样干。另外还有一点，不要使用GET请求，因为这样会把token暴露在url里。

Let’s take a look at how our example would change. Assume the randomly generated token is present in an HTTP parameter named _csrf. For example, the request to transfer money would look like this:
现在我们来看看怎么修改。假如随机生成的token设置在HTTP的_csrf参数里，那么这个HTTP请求就应该是这样的：

```text
POST /transfer HTTP/1.1
Host: bank.example.com
Cookie: JSESSIONID=randomid; Domain=bank.example.com; Secure; HttpOnly
Content-Type: application/x-www-form-urlencoded

amount=100.00&routingNumber=1234&account=9876&_csrf=<secure-random>
```

You will notice that we added the _csrf parameter with a random value. Now the evil website will not be able to guess the correct value for the _csrf parameter (which must be explicitly provided on the evil website) and the transfer will fail when the server compares the actual token to the expected token.
你可能已经注意到我们把随即值放在了_csrf参数里。现在恶意网站就没办法知道正确的_csrf值（恶意网站必须提供正确的_csrf值来进行伪装），那么它也就没办法成功伪装了。

## 19.3 [When to use CSRF protection](https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#when-to-use-csrf-protection)

When should you use CSRF protection? Our recommendation is to use CSRF protection for any request that could be processed by a browser by normal users. If you are only creating a service that is used by non-browser clients, you will likely want to disable CSRF protection.
我们应该什么时候使用对CSRF采取防御措施呢？我们建议只要是通过浏览器和普通用户处理的请求都应该采取CSRF防御措施。如果你用来创建服务的客户端不是浏览器的话，那你就可以关闭CSRF防御了。

### 19.3.1 [CSRF protection and JSON](https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-protection-and-json)

A common question is "do I need to protect JSON requests made by javascript?" The short answer is, it depends. However, you must be very careful as there are CSRF exploits that can impact JSON requests. For example, a malicious user can create a <a class="ulink" href="http://blog.opensecurityresearch.com/2012/02/json-csrf-with-parameter-padding.html" target="_top">CSRF with JSON using the following form</a>:
一个常见的问题是“我需要把javascript发起的JSON请求保护起来吗？”答案是看情况而定。然而你需要非常小心，一些操作可以利用CSRF漏洞的对JSON请求产生影响。举个例子，恶意用户可以使用下面的表单来伪造JSON数据
```html
<form action="https://bank.example.com/transfer" method="post" enctype="text/plain">
<input name='{"amount":100,"routingNumber":"evilsRoutingNumber","account":"evilsAccountNumber", "ignore_me":"' value='test"}' type='hidden'>
<input type="submit"
	value="Win Money!"/>
</form>
```

This will produce the following JSON structure上面的表单可以生成如下的JSON结构
```json
{ 
	"amount": 100,
	"routingNumber": "evilsRoutingNumber",
	"account": "evilsAccountNumber",
	"ignore_me": "=test"
}
```
If an application were not validating the Content-Type, then it would be exposed to this exploit. Depending on the setup, a Spring MVC application that validates the Content-Type could still be exploited by updating the URL suffix to end with ".json" as shown below:
如果应用没有检查Content-Type，那么它就存在这个漏洞。根据设置，如果URL的后缀是.json，那么Spring MVC应用即使对Content-Type进行了格式检查，同样存在这个漏洞。示例如下：
```html
<form action="https://bank.example.com/transfer.json" method="post" enctype="text/plain">
<input name='{"amount":100,"routingNumber":"evilsRoutingNumber","account":"evilsAccountNumber", "ignore_me":"' value='test"}' type='hidden'>
<input type="submit"
	value="Win Money!"/>
</form>
```
### 19.3.2 [CSRF and Stateless Browser Applications跨站请求伪造和无状态浏览器应用](https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-and-stateless-browser-applications)

What if my application is stateless? That doesn’t necessarily mean you are protected. In fact, if a user does not need to perform any actions in the web browser for a given request, they are likely still vulnerable to CSRF attacks.
如果应用程序是无状态的，是不是意味着不需要进行CSRF防御呢？答案是同样需要。事实上，即使用户不对浏览器请求进行任何操作，同样容易受到跨站请求伪造攻击。

For example, consider an application uses a custom cookie that contains all the state within it for authentication instead of the JSESSIONID. When the CSRF attack is made the custom cookie will be sent with the request in the same manner that the JSESSIONID cookie was sent in our previous example.
举个例子，假如程序把所有身份验证相关的状态信息保存在自定义的cookie中，而不是保存在JSESSIONID里。那么当CSRF攻击发生时，这个自定义的cookie同样会随着请求发送出去（即使恶意网站看不到cookie的内容），这个先前的把JSESSIONID保存到cookie中例子一样。

Users using basic authentication are also vulnerable to CSRF attacks since the browser will automatically include the username password in any requests in the same manner that the JSESSIONID cookie was sent in our previous example.
用户使用简单的身份验证同样容易受到CSRF的攻击，因为浏览器一般会自动把用户名、密码附在所有请求上，和先前例子里的JSESSIONID cookie一样。

## 19.4 [Using Spring Security CSRF Protection使用Spring Security CSRF防御](https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-using)

So what are the steps necessary to use Spring Security’s to protect our site against CSRF attacks? The steps to using Spring Security’s CSRF protection are outlined below:那么使用Spring Security来保护网站防御CSRF攻击的步骤有哪些呢？步骤如下：
- [Use proper HTTP verbs](https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-use-proper-verbs) 使用合适的HTTP动词
- [Configure CSRF Protection](https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-configure) 配置CSRF保护
- [Include the CSRF Token](https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-include-csrf-token) 添加CSRF令牌

### 19.4.1 [Use proper HTTP verbs](https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-use-proper-verbs) 使用合适的HTTP动词

The first step to protecting against CSRF attacks is to ensure your website uses proper HTTP verbs. Specifically, before Spring Security’s CSRF support can be of use, you need to be certain that your application is using PATCH, POST, PUT, and/or DELETE for anything that modifies state.
防御CSRF的第一步就是使用合适的HTTP动词。尤其是在添加Spring Security CSRF支持前，你需要使用PATCH、POST、PUT和DELETE这些动词来修改应用状态。

This is not a limitation of Spring Security’s support, but instead a general requirement for proper CSRF prevention. The reason is that including private information in an HTTP GET can cause the information to be leaked. See <a class="ulink" href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec15.html#sec15.1.3" target="_top">RFC 2616 Section 15.1.3 Encoding Sensitive Information in URI’s</a> for general guidance on using POST instead of GET for sensitive information.
这不是Spring Security特有的规则，而是防御CSRF常用的措施。不能使用HTTP GET的原因是它会泄露信息。您可以在RFC这里了解详情。

### 19.4.2 [Configure CSRF Protection](https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-configure)配置CSRF保护

The next step is to include Spring Security’s CSRF protection within your application. Some frameworks handle invalid CSRF tokens by invaliding the user’s session, but this causes <a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-logout" title="19.5.3 Logging Out">its own problems</a>. Instead by default Spring Security’s CSRF protection will produce an HTTP 403 access denied. This can be customized by configuring the <a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#access-denied-handler" title="15.2.2 AccessDeniedHandler">AccessDeniedHandler</a> to process <code class="literal">InvalidCsrfTokenException</code> differently.
下一步就是添加Spring Security CSRF保护到程序里。<span>有些框架通过使用户会话无效来处理无效的CSRF令牌，但这样做有一些问题。Spring Security则是通过产生一个HTTP 403拒绝请求的方式来处理无效的CSRF令牌。可以通过配置<a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#access-denied-handler" title="15.2.2 AccessDeniedHandler">AccessDeniedHandler</a> 和使用<code class="literal">InvalidCsrfTokenException</code>来改变默认的配置。</span>

As of Spring Security 4.0, CSRF protection is enabled by default with XML configuration. If you would like to disable CSRF protection, the corresponding XML configuration can be seen below.
<span>在Spring Security 4.0中，使用XML配置时CSRF保护是默认开启的。如果你想关闭保护，可以使用如下的配置。</span>
```xml
<http>
	<!-- ... -->
	<csrf disabled="true"/>
</http>
```
CSRF protection is enabled by default with Java Configuration. If you would like to disable CSRF, the corresponding Java configuration can be seen below. Refer to the Javadoc of csrf() for additional customizations in how CSRF protection is configured.
使用Java配置时，CSRF也是默认开启的。如果要关闭保护，参考下面的代码。更高级的配置请参考<span style="color: #ff0000;">Javadoc（官网提供的链接是失效的）</span>
```java
@EnableWebSecurity
public class WebSecurityConfig extends
WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.csrf().disable();
	}
}
```
### 19.4.3 [Include the CSRF Token添加CSRF令牌](https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-include-csrf-token)

#### [Form Submissions表单提交](https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-include-csrf-token-form)

The last step is to ensure that you include the CSRF token in all PATCH, POST, PUT, and DELETE methods. One way to approach this is to use the <code class="literal">_csrf</code> request attribute to obtain the current <code class="literal">CsrfToken</code>. An example of doing this with a JSP is shown below:
最后一步就是保证在所有PATCH、POST、PUT和DELETE方法中添加CSRF令牌。一种实现方式是使用<code class="literal">_csrf</code>请求属性来保存<code class="literal">CsrfToken</code>。在JSP页面中可以参考下面的方式：
```jsp
<c:url var="logoutUrl" value="/logout"/>
<form action="${logoutUrl}"
	method="post">
<input type="submit"
	value="Log out" />
<input type="hidden"
	name="${_csrf.parameterName}"
	value="${_csrf.token}"/>
</form>
```
An easier approach is to use <a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#the-csrfinput-tag" title="32.5 The csrfInput Tag">the csrfInput tag</a> from the Spring Security JSP tag library.
更简洁的方法是直接使用Spring Security JSP标签库里的<a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#the-csrfinput-tag" title="32.5 The csrfInput Tag">csrfInput</a>标签。

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

#### [Ajax and JSON Requests](https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-include-csrf-token-ajax)

If you are using JSON, then it is not possible to submit the CSRF token within an HTTP parameter. Instead you can submit the token within a HTTP header. A typical pattern would be to include the CSRF token within your meta tags. An example with a JSP is shown below:
如果你使用的是JSON，那么没有办法可以把CSRF令牌放到HTTP参数里提交。但是你可以把令牌放到HTTP头部里然后提交请求。一种典型的做法是把CSRF令牌放到meta标签里。下面是一个JSP的例子

```xml
<html>
<head>
	<meta name="_csrf" content="${_csrf.token}"/>
	<!-- default header name is X-CSRF-TOKEN -->
	<meta name="_csrf_header" content="${_csrf.headerName}"/>
	<!-- ... -->
</head>
<!-- ... -->
```
Instead of manually creating the meta tags, you can use the simpler <a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#the-csrfmetatags-tag" title="32.6 The csrfMetaTags Tag">csrfMetaTags tag</a> from the Spring Security JSP tag library.
除了手动创建meta标签的方式外，你可以使用Spring Security JSP标签库里的<a class="link" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#the-csrfmetatags-tag" title="32.6 The csrfMetaTags Tag">csrfMetaTags</a>标签。

You can then include the token within all your Ajax requests. If you were using jQuery, this could be done with the following:
你可以把令牌放到所有的Ajax请求里。如果你用的是jQuery，那你可以这么干
```javascript
$(function () {
var token = $("meta[name='_csrf']").attr("content");
var header = $("meta[name='_csrf_header']").attr("content");
$(document).ajaxSend(function(e, xhr, options) {
	xhr.setRequestHeader(header, token);
});
});
```
As an alternative to jQuery, we recommend using <a class="ulink" href="http://cujojs.com/" target="_top">cujoJS’s</a> rest.js. The <a class="ulink" href="https://github.com/cujojs/rest" target="_top">rest.js</a> module provides advanced support for working with HTTP requests and responses in RESTful ways. A core capability is the ability to contextualize the HTTP client adding behavior as needed by chaining interceptors on to the client.
另一种替代jQuery的方式是，我们推荐的 <a class="ulink" href="http://cujojs.com/" target="_top">cujoJS</a>的rest.js。rest.js的模块提供了更高级一点的处理，支持对HTTP请求和响应以RESTful方式进行处理。一个核心的功能是，<span style="color: #ff0000;">通过链式拦截器来提供HTTP客户端的上下文处理能力（这句不知道怎么翻译）</span>。
```javascript
var client = rest.chain(csrf, {
token: $("meta[name='_csrf']").attr("content"),
name: $("meta[name='_csrf_header']").attr("content")
});
```
The configured client can be shared with any component of the application that needs to make a request to the CSRF protected resource. One significant difference between rest.js and jQuery is that only requests made with the configured client will contain the CSRF token, vs jQuery where <span class="emphasis"><em>all</em></span> requests will include the token. The ability to scope which requests receive the token helps guard against leaking the CSRF token to a third party. Please refer to the <a class="ulink" href="https://github.com/cujojs/rest/tree/master/docs" target="_top">rest.js reference documentation</a> for more information on rest.js.
这个client可以被前端应用的任何需要采取CSRF防御的组件共享。rest.js和jQuery的一个明显的不同是，rest.js只有经过配置的client生成的请求才会有CSRF令牌，而jQuery的所有请求都会包含令牌。这种能够管理哪个请求能拿到令牌的能力，能够防止CSRF令牌泄露给其他第三方组件。

#### [CookieCsrfTokenRepository  CSRF令牌cookie仓库](https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-cookie)

There can be cases where users will want to persist the <code class="literal">CsrfToken</code> in a cookie. By default the <code class="literal">CookieCsrfTokenRepository</code> will write to a cookie named <code class="literal">XSRF-TOKEN</code> and read it from a header named <code class="literal">X-XSRF-TOKEN</code> or the HTTP parameter <code class="literal">_csrf</code>. These defaults come from<a class="ulink" href="https://docs.angularjs.org/api/ng/service/$http#cross-site-request-forgery-xsrf-protection" target="_top">AngularJS</a>
一些情况下程序员可能想把<code class="literal">CsrfToken</code> 持久化到cookie里。默认情况下<code class="literal">CookieCsrfTokenRepository</code>会在把令牌写到一个名为<code class="literal">XSRF-TOKEN</code>的cookie里，从名为<code class="literal">X-XSRF-TOKEN</code>的请求头或，名为<code class="literal">_csrf</code>的HTTP参数里读取令牌的值。这些默认操作都来自AngularJS

You can configure <code class="literal">CookieCsrfTokenRepository</code> in XML using the following:   XML配置如下：
```xml
<http>
	<!-- ... -->
	<csrf token-repository-ref="tokenRepository"/>
</http>
<b:bean id="tokenRepository"
	class="org.springframework.security.web.csrf.CookieCsrfTokenRepository"
	p:cookieHttpOnly="false"/>
```
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

You can configure <code class="literal">CookieCsrfTokenRepository</code> in Java Configuration using: Java配置如下：
```java
@EnableWebSecurity
public class WebSecurityConfig extends
		WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.csrf()
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
	}
}
```

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

## [19.5 CSRF Caveats 跨站请求伪造的陷阱](https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-caveats)
There are a few caveats when implementing CSRF.
### [19.5.1 Timeouts 超时](https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-timeouts)

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
```java
@EnableWebSecurity
public class WebSecurityConfig extends
WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.logout()
				.logoutRequestMatcher(new AntPathRequestMatcher("/logout"));
	}
}
```
</div>
<div class="section">
<div class="titlepage">
<h3 class="title"><a name="csrf-multipart" href="https://docs.spring.io/spring-security/site/docs/5.0.6.RELEASE/reference/htmlsingle/#csrf-multipart"></a></h3>
</div>
</div>
</div>