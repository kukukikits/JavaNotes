# Web Application Security
大多数Spring Security用户将在使用HTTP和Servlet API的应用程序中使用该框架。在这一部分中，我们将看一看Spring Security如何为应用程序的web层提供身份验证和访问控制功能。我们将深入介绍命名空间的实现，查看是哪些类和接口被用来组装以提供web层安全。在某些情况下，有必要使用传统的bean配置来提供对配置的完全控制，因此我们还将看到如何在没有名称空间的情况下直接配置这些类。
# 14. The Security Filter Chain  安全过滤器链
<span>Spring Security的web基础设施完全基于标准的servlet过滤器。它不使用servlet或任何其他基于servlet的框架（比如Spring MVC），因此它与任何特定的web技术没有紧密的联系。它仅处理<code class="cye-lm-tag">HttpServletRequest</code>和<code>HttpServletResponse</code>，并不关心这些请求是否来自浏览器、web服务客户端、HttpInvoker或AJAX应用程序。</span>

<span>Spring Security在内部维护一个过滤器链，其中每个过滤器都有特定的责任，并且根据需要的服务，从配置中添加或删除过滤器。过由于过滤器之间有依赖关系，所以他们的顺序非常重要。如果您一直在使用 <a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#ns-config">namespace configuration</a>，那么过滤器将自动为您配置，您不需要显式地定义任何Spring bean。但如果要添加一些namespace配置中不支持的功能，或者添加自定义的类，这时你就需要对安全过滤器进行完全的控制。</span>
<h2>14.1 DelegatingFilterProxy</h2>
<span>当使用servlet过滤器时，显然需要在web.xml中声明它们，否则它们会被servlet容器忽略。在Spring Security中，过滤器类也是在应用程序上下文中定义的Spring bean，因此在Spring Security中也能够利用Spring丰富的依赖注入功能和生命周期接口。Spring的委托过滤代理<code class="cye-lm-tag">DelegatingFilterProxy</code> 提供了web.xml和应用程序上下文之间的链接。</span>

当使用<code>DelegatingFilterProxy</code>时，在web.xml文件中你能看到与下面类似代码：
```xml
<filter>
<filter-name>myFilter</filter-name>
<filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>
</filter>

<filter-mapping>
<filter-name>myFilter</filter-name>
<url-pattern>/*</url-pattern>
</filter-mapping>
```
<span>请注意，过滤器实际上是一个<code>DelegatingFilterProxy</code>，而不是真正实现过滤器逻辑的类。<code>DelegatingFilterProxy</code>所做的是将过滤器的方法委托给从Spring应用程序上下文中获得的bean。这使得bean能够从Spring web应用程序上下文生命周期支持和配置灵活性中获益。bean必须实现javax.servlet.Filter，它的名称必须与<code>filter-name</code> 元素中的名称相同。请阅读用于<code>DelegatingFilterProxy</code>的Javadoc以获得更多信息。</span>
<h2>14.2 FilterChainProxy</h2>
<span>Spring Security的web基础设施只能通过委托给<code class="cye-lm-tag">FilterChainProxy</code>的实例来使用。安全过滤器不应该单独使用。理论上，您可以声明您在应用程序上下文文件中需要的每个Spring Security filter bean，为每一个过滤器添加相应的<code class="cye-lm-tag">DelegatingFilterProxy</code> 条目到web.xml文件中，并确保它们的排序正确，但是这很麻烦，并且一旦过滤器数量过多，web.xml文件会非常混乱。使用<code class="cye-lm-tag">FilterChainProxy</code>可以让我们在web.xml上仅添加一个条目，就能使用应用程序上下文文件来管理我们的web security beans。它是使用<code>DelegatingFilterProxy</code>来连接的，就像上面的例子一样，但是需要把<code>filter-name</code> 元素的值设置为<code class="cye-lm-tag">FilterChainProxy </code>bean的名字“filterChainProxy”。然后，过滤器链在应用程序上下文中以相同的bean名称声明。如:</span>
```xml
<bean id="filterChainProxy" class="org.springframework.security.web.FilterChainProxy">
<constructor-arg>
	<list>
	<sec:filter-chain pattern="/restful/**" filters="
		securityContextPersistenceFilterWithASCFalse,
		basicAuthenticationFilter,
		exceptionTranslationFilter,
		filterSecurityInterceptor" />
	<sec:filter-chain pattern="/**" filters="
		securityContextPersistenceFilterWithASCTrue,
		formLoginFilter,
		exceptionTranslationFilter,
		filterSecurityInterceptor" />
	</list>
</constructor-arg>
</bean>
```

<span>名称空间元素<code>filter-chain</code>用于方便设置应用程序中所需的安全过滤器链 <sup class="footnote">[<a id="_footnoteref_6" class="footnote" href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#_footnote_6" title="View footnote.">6</a>]</sup>。它将一个特定的URL模式映射到由过滤器元素中指定的bean名称构建的过滤器列表，并将它们组合在一类SecurityFilterChain的bean中。<code>filter-chain</code>元素的filters属性为<code>filters</code>元素声明的bean的名字，<code>filter-chain</code>将特定的URL映射到这些filters里，然后把这些URL组合到一个<code class="cye-lm-tag">SecurityFilterChain</code>的bean里。 <code>pattern</code>属性采用 Ant Paths，特殊的URI应该最先声明 <sup class="footnote cye-lm-tag">[<a id="_footnoteref_7" class="footnote" href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#_footnote_7" title="View footnote.">7</a>]</sup>。在运行时， <code class="cye-lm-tag">FilterChainProxy</code>将定位到与当前web请求相匹配的第一个URI，并且使用<code class="cye-lm-tag">filters</code>属性指定的过滤器bean对请求进行过滤。过滤器将按照它们定义的顺序调用，因此您可以完全控制过滤器链。</span>

<span>你可能已经注意到我们在过滤器链中声明了两个<code class="cye-lm-tag">SecurityContextPersistenceFilter</code> （ASC是<code>allowSessionCreation</code>的缩写，是<code>SecurityContextPersistenceFilter</code>的一个属性）。如果一个web服务在所有请求中都不使用jsessionid，那么为此类代理创建HttpSession就是浪费资源。如果您有一个大容量的应用程序，它需要最大的可伸缩性，我们建议您使用上面所示的方法。对于较小的应用程序，使用单个<code>SecurityContextPersistenceFilter</code>（其默认的<code class="cye-lm-tag">allowSessionCreation</code> 为真）可能就足够了。</span>

<span>注意，<code>FilterChainProxy</code> 没有调用它配置的过滤器上的标准过滤器生命周期方法。我们建议您使用Spring的应用程序上下文生命周期接口作为替代，就像您对其他Spring bean一样。</span>

<span>当我们研究如何使用<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#ns-web-xml">名称空间配置</a>来设置web安全性时，我们使用了一个名为“springSecurityFilterChain”的<code>DelegatingFilterProxy</code>。您现在应该能够看到，这是由名称空间创建的<code>FilterChainProxy</code>的名称。</span>
<h3>14.2.1 <span>绕过过滤器链 </span>Bypassing the Filter Chain</h3>
<span>您可以使用属性filters="none"来设置filter bean list，这会使安全过滤链完全忽略pattern属性匹配的请求。注意，任何匹配pattern的请求都不会进行身份验证或授权，并且可以自由访问。如果您想在请求期间使用SecurityContext的内容，那么这些内容必须通过安全过滤器链。否则<code class="cye-lm-tag">SecurityContextHolder</code>就不会被填充，并且他的内容为null。</span>
<h2>14.3 过滤器排序 Filter Ordering</h2>
<span>过滤器在链中定义的顺序是非常重要的。</span><span>不管您实际使用的是哪种过滤器，顺序应该如下：</span>
<ul>
 	<li><code>ChannelProcessingFilter</code>，<span>因为它可能需要重定向到另一个协议</span></li>
 	<li><code class="cye-lm-tag">SecurityContextPersistenceFilter</code>，这样<span>，可以在web请求的开始时在<code>SecurityContextHolder</code>中设置<code class="cye-lm-tag">SecurityContext</code> ，当web请求结束时，对<code class="cye-lm-tag">SecurityContext</code>的任何更改都可以复制到HttpSession中（准备好与下一个web请求一起使用）。</span></li>
 	<li><span><code>ConcurrentSessionFilter</code>，因为它使用了<code>SecurityContextHolder</code>的功能，需要更新<code class="cye-lm-tag">SessionRegistry</code>，以反映来自主体的持续请求。</span></li>
 	<li><span>身份验证处理机制——<code>UsernamePasswordAuthenticationFilter</code>、<code>UsernamePasswordAuthenticationFilter</code>，这样，可以修改<code>SecurityContextHolder</code>，使它包含有效的<code>Authentication</code>请求令牌</span></li>
 	<li><code>SecurityContextHolderAwareRequestFilter</code>，<span>安装Spring Security感知的<code class="cye-lm-tag">HttpServletRequestWrapper</code>到servlet容器中时需要。</span></li>
 	<li><code class="cye-lm-tag">JaasApiIntegrationFilter</code>，<span>如果<code>SecurityContextHolder</code> 中有<code class="cye-lm-tag">JaasAuthenticationToken</code>，<code class="cye-lm-tag">JaasApiIntegrationFilter</code>会把<code class="cye-lm-tag">FilterChain</code>当做 <code class="cye-lm-tag">JaasAuthenticationToken</code>中的<code>Subject</code>进行处理</span></li>
 	<li><code>RememberMeAuthenticationFilter</code>，<span>如果前面的身份认证处理机制没有更新 <code class="cye-lm-tag">SecurityContextHolder</code>，并且这个请求提供了一个cookie来开启remeber-me服务，那么使用<code>RememberMeAuthenticationFilter</code>会把一个合适的remembered <code>Authentication</code>对象放到<code class="cye-lm-tag">SecurityContextHolder</code>中。</span></li>
 	<li><code>AnonymousAuthenticationFilter</code>，如果前面的身份认证处理机制没有更新<span><code class="cye-lm-tag">SecurityContextHolder</code>，那么使用<code>AnonymousAuthenticationFilter</code>会把一个匿名的<code>Authentication</code> 对象放到<code class="cye-lm-tag">SecurityContextHolder</code>中</span></li>
 	<li><code>ExceptionTranslationFilter</code>，<span>捕捉任何Spring安全异常，以便能够返回HTTP错误响应，或者启动适当的<code class="cye-lm-tag">AuthenticationEntryPoint</code> </span></li>
 	<li><code>FilterSecurityInterceptor</code>，<span>在拒绝访问时保护web uri并抛出异常</span></li>
</ul>
<h2>14.4 Request Matching and HttpFirewall</h2>
<span>为了决定如何处理请求，Spring Security会在定义pattern的地方，针对即将传入的请求进行测试。当<code>FilterChainProxy</code>决定应该使用哪个过滤器链，或者当 <code class="cye-lm-tag">FilterSecurityInterceptor</code>决定使用哪个安全约束来处理请求时就会发生这种情况。重要的是要理解这个机制，以及在测试patterns时使用的URL值。</span>

<span>Servlet规范为HttpServletRequest定义了几个属性，这些属性可以通过getter方法进行访问，这些值也是我们进行匹配时需要的。这些属性有：<code>contextPath</code>, <code>servletPath</code>, <code>pathInfo</code> and <code>queryString</code>。Spring Security只对在应用程序中安全路径感兴趣，因此contextPath将被忽略。不幸的是，servlet规范并没有确切地定义对于特定请求URI，servletPath和pathInfo应该包含什么值。例如，URL中的每个路径段都可能包含参数，如<a href="https://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a> <sup class="footnote">[<a id="_footnoteref_8" class="footnote" href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#_footnote_8" title="View footnote.">8</a>]</sup>中所定义的那样。规范没有明确说明这些参数是否需要包含在servletPath和pathInfo的值中，是否需要在不同的servlet容器之间表现为不同的行为。这是非常危险的，当一个应用程序被部署到一个容器中时，它不会从这些值中剥离路径参数，攻击者可以将它们添加到所请求的URL中，以便使模式匹配成功或失败<sup class="footnote">[<a id="_footnoteref_9" class="footnote" href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#_footnote_9" title="View footnote.">9</a>]</sup>。传入URL的其他变体也是可能的。例如，它可以包含路径遍历序列（如/../）或多个斜杠（//），这也可能导致路径匹配失败。一些容器在执行servlet映射之前将URL标准化，但其他容器则没有。为了防止类似的问题，<code>FilterChainProxy</code>使用<code>HttpFirewall</code>策略来检查和包装请求。没有标准化的请求在默认情况下会被自动拒绝，并且为了匹配路径，路径参数和重复的斜杠会被删除[<a id="_footnoteref_10" class="footnote" href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#_footnote_10" title="View footnote.">10</a>]。因此，使用<code>FilterChainProxy</code>来管理安全过滤器链是至关重要的。请注意，servletPath和pathInfo值被容器解码，因此，您的应用程序的有效路径不能包含分号，因为这些部分会被删除。</span>

<span>如上所述，默认策略是使用ant样式路径进行匹配，这可能是大多数用户的最佳选择。该策略是在<code>AntPathRequestMatcher</code>类中实现的，它使用Spring的<code>AntPathMatcher</code>来对串接的servletPath和pathInfo执行不区分大小写的匹配，并且忽略<code class="cye-lm-tag">queryString</code>。</span>

<span>如果出于某种原因，您需要一个更强大的匹配策略，您可以使用正则表达式。该</span><span>策略由 <code>RegexRequestMatcher</code>实现。请参阅本课程的Javadoc以获得更多信息。</span>

<span>在实践中，我们建议您在服务层对方法进行保护，以控制对应用程序的访问，不要完全依赖于在web应用程序级别定义的安全约束。url会发生更改，所以很难知道应用程序支持的所有可能的url，以及如何操纵请求。你应该使用一些简单的ant路径，因为简单的ant路径比较容易理解。当万能的通配符定义在末尾的时候，应该使用“默认拒绝”的设置来拒绝访问。</span>

<span>在服务层定义的安全性要健壮得多，而且更难绕过。</span><span>因此，您应该始终利用Spring Security的方法安全选项。</span>

<span><code class="cye-lm-tag">HttpFirewall</code>还通过拒绝HTTP响应头中的换行字符来防止HTTP响应分裂 <a href="https://www.owasp.org/index.php/HTTP_Response_Splitting">HTTP Response Splitting</a> 。</span>

<span>默认情况下，使用<code>StrictHttpFirewall</code>。这个实现拒绝了看起来是恶意的请求。如果这个防护级别对您的需求过于严格，那么您可以定制哪些类型的请求被拒绝。但是降低防护级别，会增加您程序的安全风险。例如，如果您希望利用Spring MVC的矩阵变量，那么可以在XML中使用以下配置：</span>
```xml
<b:bean id="httpFirewall"
      class="org.springframework.security.web.firewall.StrictHttpFirewall"
      p:allowSemicolon="true"/>

<http-firewall ref="httpFirewall"/>
```
对应的Java配置如下：
```java
@Bean
public StrictHttpFirewall httpFirewall() {
    StrictHttpFirewall firewall = new StrictHttpFirewall();
    firewall.setAllowSemicolon(true);
    return firewall;
}
```
<h2>14.5 使用其他基于过滤器的框架</h2>
<span>如果您使用的是另一个基于过滤的框架，那么您需要确保Spring Security过滤器是第一位的。</span><span>这使得<code>SecurityContextHolder</code>能够及时填充，供其他过滤器使用。例如，使用SiteMesh来布置您的web页面，或像Wicket这样的web框架（使用一个过滤器来处理它的请求）。</span>
<h2>14.6 <span>高级的名称空间配置</span></h2>
<span>正如我们在名称空间章节中看到的，可以使用多个http元素为不同的URL模式定义不同的安全配置。每个元素在内部<code>FilterChainProxy</code>和URL模式中创建了一个过滤器链。元素按照声明的顺序添加，因此最特定的模式必须声明在前面。下面是另一个例子，应用程序既支持无状态的RESTful API，也支持一个普通的web应用程序，用户可以使用表单登录。</span>

```xml
<!-- Stateless RESTful service using Basic authentication -->
<http pattern="/restful/**" create-session="stateless">
<intercept-url pattern='/**' access="hasRole('REMOTE')" />
<http-basic />
</http>

<!-- Empty filter chain for the login page -->
<http pattern="/login.htm*" security="none"/>

<!-- Additional filter chain for normal users, matching all other requests -->
<http>
<intercept-url pattern='/**' access="hasRole('USER')" />
<form-login login-page='/login.htm' default-target-url="/home.htm"/>
<logout />
</http>
```
<h1>15 核心的Security Filters</h1>
<span>在使用Spring Security的web应用程序中，一些关键的过滤器会被使用，因此我们将首先查看它们的支持类和接口。我们不会涵盖所有的特性，所以如果您想要获得完整的内容，请看它们的Javadoc。</span>
<h2>15.1 FilterSecurityInterceptor</h2>
<span>我们在讨论访问控制<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#tech-intro-access-control">access-control in general</a>时，已经简要地看到了<code class="cye-lm-tag">FilterSecurityInterceptor</code>，我们已经在名称空间中使用了它，其中使用<code>&lt;intercept-url&gt;</code> 元素在内部配置<code class="cye-lm-tag">FilterSecurityInterceptor</code>。现在，我们将看到如何显式地配置它，以便与<code class="cye-lm-tag">FilterChainProxy</code>以及它的配套过滤器<code>ExceptionTranslationFilter</code>一起使用。下面显示了一个典型的配置示例：</span>

```xml
<bean id="filterSecurityInterceptor"
	class="org.springframework.security.web.access.intercept.FilterSecurityInterceptor">
<property name="authenticationManager" ref="authenticationManager"/>
<property name="accessDecisionManager" ref="accessDecisionManager"/>
<property name="securityMetadataSource">
	<security:filter-security-metadata-source>
	<security:intercept-url pattern="/secure/super/**" access="ROLE_WE_DONT_HAVE"/>
	<security:intercept-url pattern="/secure/**" access="ROLE_SUPERVISOR,ROLE_TELLER"/>
	</security:filter-security-metadata-source>
</property>
</bean>
```
<span><code>FilterSecurityInterceptor</code> 负责处理HTTP资源的安全性。它需要 <code>AuthenticationManager</code> 和<code class="cye-lm-tag">AccessDecisionManager</code>的引用。它还提供了应用于不同HTTP URL请求的配置属性。请参阅技术介绍中关于这些内容的原始讨论 <a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#tech-intro-config-attributes">the original discussion on these</a>。</span>

<span><code>FilterSecurityInterceptor</code> 可以通过两种方式使用配置属性进行配置。第一种，如上所示，是使用<code class="cye-lm-tag">&lt;filter-security-metadata-source&gt;</code>命名空间元素，这类似于名称空间章节中的<code>&lt;http&gt;</code> 元素，但是<code>&lt;intercept-url&gt;</code> 子元素只使用<code>pattern</code> 和 <code>access</code> 属性。使用逗号来分隔不同的URL。第二种方法是编写自己的SecurityMetadataSource，但这超出了本文的范围。不管使用的方法是什么，SecurityMetadataSource负责返回<code>List&lt;ConfigAttribute&gt;</code> ，其中包含了与单个安全HTTP URL相关联的所有配置属性。</span>

<span>值得注意的是，<code>FilterSecurityInterceptor.setSecurityMetadataSource()</code>方法实际上使用的是<code>FilterInvocationSecurityMetadataSource</code>的实例。这是一个标记接口，是 <code>SecurityMetadataSource</code>的子类。仅仅用来表示<code>SecurityMetadataSource</code>理解 <code>FilterInvocation</code>。为了简单起见，我们将继续把<code>FilterInvocationSecurityMetadataSource</code>称为<code>SecurityMetadataSource</code>，因为这种区别与大多数用户无关。</span>

<span>由名称空间语法创建的SecurityMetadataSource包含了特定 <code>FilterInvocation</code>的配置属性，这些配置属性是通过匹配请求URL和pattern属性来获得的。This behaves in the same way as it does for namespace configuration。默认情况下，所有表达式都视为Apache Ant路径，复杂情况下也支持使用正则表达式。<code>request-matcher</code>属性用来指定所使用的pattern的类型。在同一个定义中不能混合表达语法。例如，如果前面的配置使用正则表达式而不使用Ant路径，则代码如下所示：</span>

```xml
<bean id="filterInvocationInterceptor"
	class="org.springframework.security.web.access.intercept.FilterSecurityInterceptor">
<property name="authenticationManager" ref="authenticationManager"/>
<property name="accessDecisionManager" ref="accessDecisionManager"/>
<property name="runAsManager" ref="runAsManager"/>
<property name="securityMetadataSource">
	<security:filter-security-metadata-source request-matcher="regex">
	<security:intercept-url pattern="\A/secure/super/.*\Z" access="ROLE_WE_DONT_HAVE"/>
	<security:intercept-url pattern="\A/secure/.*\" access="ROLE_SUPERVISOR,ROLE_TELLER"/>
	</security:filter-security-metadata-source>
</property>
</bean>
```

Patterns总是按照他们定义的顺序进行匹配。<span>因此，在列表中具体的pattern需要定义到不太具体的pattern前面。我们上面的例子就是这样，具体的 <code class="cye-lm-tag">/secure/super/</code> 在不太具体的  <code>/secure/</code>前面定义。如果他们定义的顺序反过来的话，<code>/secure/</code>匹配成功，<code class="cye-lm-tag">/secure/super/</code>则永远都不会进行匹配。</span>

<h2>15.2 ExceptionTranslationFilter</h2>
在security filter栈中<code class="cye-lm-tag">ExceptionTranslationFilter</code><span> 位于 <code>FilterSecurityInterceptor</code>之上。它自己不会执行任何实际的安全措施，而是处理安全拦截器抛出的异常，并提供适当的HTTP响应。</span>

```xml
<bean id="exceptionTranslationFilter"
class="org.springframework.security.web.access.ExceptionTranslationFilter">
<property name="authenticationEntryPoint" ref="authenticationEntryPoint"/>
<property name="accessDeniedHandler" ref="accessDeniedHandler"/>
</bean>
<bean id="authenticationEntryPoint"
class="org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint">
<property name="loginFormUrl" value="/login.jsp"/>
</bean>


<bean id="accessDeniedHandler"
class="org.springframework.security.web.access.AccessDeniedHandlerImpl">
<property name="errorPage" value="/accessDenied.htm"/>
</bean>
```

<h3>15.2.1 AuthenticationEntryPoint</h3>
<span>如果用户请求一个安全的HTTP资源，但是他们没有经过身份验证，那么就会调用AuthenticationEntryPoint。安全拦截器可以抛出合适的AuthenticationException或AccessDeniedException扔到调用堆栈上，触发entry point上的<code class="cye-lm-tag">commence</code>方法。这样做的目的就是向用户提供适当的响应，以便身份验证可以开始。我们在这里使用的是LoginUrlAuthenticationEntryPoint，它将请求重定向到另一个URL（通常是登录页面）。实际实现将取决于应用程序中使用的认证机制。</span>
<h3>15.2.2 AccessDeniedHandler</h3>
<span>如果用户已经经过身份验证，并试图访问受保护的资源，会发生什么呢？在正常情况下，这是不应该发生的，因为应用程序工作流程应该限制在用户能够访问的操作上。例如，一个管理页面的HTML链接可能会被隐藏在没有管理员角色的用户中。但是，您不能依赖于隐藏链接来实现安全性，因为总是存在用户直接输入URL，试图绕过这些限制的可能。或者，他们可能会修改RESTful URL来改变一些参数值。您的应用程序必须受到保护，以避免这些场景，否则它肯定是不安全的。您通常会使用简单的web层安全防护来保护基本的URL，并在服务层接口上使用更具体的针对方法的安全防护，从而真正确定什么是允许的。</span>

<span>如果抛出AccessDeniedException，并且用户已经被验证了，那么这意味着用户执行了没有足够的权限操作。在这种情况下，<code>ExceptionTranslationFilter</code> 会调用第二个策略，即<code>AccessDeniedHandler</code>。默认情况下，会使用<code>AccessDeniedHandlerImpl</code> ，<code>AccessDeniedHandlerImpl</code>的作用仅仅是向客户机发送403（禁止）响应。或者，您可以显式地配置一个实例（如上面的例子），并设置一个错误页面URL，请求会被转发给这个错误页面<sup class="footnote">[<a id="_footnoteref_11" class="footnote" href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#_footnote_11" title="View footnote.">11</a>]</sup>。这可以是一个简单的“访问拒绝”页面，比如JSP，或者它可以是一个更复杂的处理程序，比如MVC controller。当然，您可以自己实现接口并使用自己的实现。</span>

<span>当你使用名称空间来配置你的应用程序时，也可以提供一个定制的<code>AccessDeniedHandler</code> 。有关更多细节，请参阅<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#nsa-access-denied-handler">命名空间附录</a>。</span>
<h3>15.2.3 SavedRequest s and the RequestCache Interface</h3>
<span> <code class="cye-lm-tag">ExceptionTranslationFilter</code>的另一个职责是在调用AuthenticationEntryPoint之前保存当前请求。这允许在用户经过身份验证后恢复请求（参见前面的<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#tech-intro-web-authentication">web认证概述</a>）。一个典型的例子是用户使用表单登录，然后使用默认<code>SavedRequestAwareAuthenticationSuccessHandler</code> 重定位到先前的URL上（参见15.4.1）。</span>

<span>RequestCache封装了存储和检索HttpServletRequest实例所需的功能。</span><span>默认情况下，HttpSessionRequestCache被使用，它将请求存储在HttpSession中。RequestCacheFilter在用户被重定向到原始URL时，可以从缓存中恢复已保存的请求。</span>

<span>在正常情况下，您不需要对这些功能做任何改动。但是，保存请求的处理过程是一种“best-effort”的形式（就是尽最大努力），可能会出现默认配置无法处理的情况。从Spring Security 3.0开始，完全可以使用这些接口来自定义实现。</span>
<h2>15.3 SecurityContextPersistenceFilter</h2>
<span>我们在<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#tech-intro-sec-context-persistence">技术概述章节</a>中讨论了所有重要的过滤器的作用，所以在阅读这一节前你可能需要复习一下技术概述中的内容。让我们首先看一看如何配置它，以便与<code class="cye-lm-tag">FilterChainProxy</code>一起使用。基本配置只需要bean本身</span>

```java
<bean id="securityContextPersistenceFilter"
class="org.springframework.security.web.context.SecurityContextPersistenceFilter"/>
```

<span>正如我们之前看到的，这个过滤器有两个主要任务。</span><span>它负责存储HTTP请求之间的SecurityContext内容，并在完成请求时清除安全上下文<code>SecurityContextHolder</code>。清理存储安全上下文的ThreadLocal是必不可少的，因为在某些情况下，线程可能被替换为servlet容器的线程池，并且用户的安全上下文依然存在。然后，这个线程可能会使用错误的凭证执行操作。</span>
<h3>15.3.1  SecurityContextRepository</h3>
<span>从Spring Security 3.0开始，加载和存储安全上下文的工作现在被委托给一个单独的策略接口：</span>

```java
public interface SecurityContextRepository {
SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder);


void saveContext(SecurityContext context, HttpServletRequest request,
HttpServletResponse response);
}
```
<span><code>HttpRequestResponseHolder</code> 只不过是传入请求和响应对象的容器，允许实现用包装器类替换请求和响应。返回的内容将被传递给过滤器链。</span>

<span>默认实现是<code>HttpSessionSecurityContextRepository</code>，它将安全上下文存储到HttpSession属性上 <sup class="footnote">[<a id="_footnoteref_12" class="footnote" href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#_footnote_12" title="View footnote.">12</a>]</sup>。这个实现的最重要的配置参数是 <code class="cye-lm-tag">allowSessionCreation</code>，它默认为true，也就是说如果需要一个会话来存储经过身份验证的用户的安全上下文，则允许该类创建一个会话（它不会创建会话，除非身份验证已经发生并且安全上下文的内容已经改变）。如果您不希望创建会话，那么您可以将该属性设置为false：</span>

```xml
<bean id="securityContextPersistenceFilter"
	class="org.springframework.security.web.context.SecurityContextPersistenceFilter">
<property name='securityContextRepository'>
	<bean class='org.springframework.security.web.context.HttpSessionSecurityContextRepository'>
	<property name='allowSessionCreation' value='false' />
	</bean>
</property>
</bean>
```

<span>您也可以提供一个 <code>NullSecurityContextRepository</code>实例，一个<a href="https://en.wikipedia.org/wiki/Null_Object_pattern">空对象实现</a>，它将防止安全上下文被存储，即使在请求期间已经创建了一个会话。</span>
<h2>15.4 UsernamePasswordAuthenticationFilter</h2>
<span>现在我们已经看到了三个主要的过滤器，它们总是出现在Spring Security web配置中。</span><span>这些也是由命名空间 <code>&lt;http&gt;</code> 元素自动创建的三个，不能用其他选项代替。现在唯一缺少的是一个实际的身份验证机制，它允许用户进行身份验证。UsernamePasswordAuthenticationFilter过滤器是最常用的身份验证过滤器，也是最常被自定义的过滤器<sup class="footnote cye-lm-tag">[<a id="_footnoteref_13" class="footnote" href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#_footnote_13" title="View footnote.">13</a>]</sup>。它还可以使用命名空间的<code>&lt;form-login&gt;</code>元素来实现。配置它需要三个阶段：</span>
<ul>
 	<li><span>用登录页面的URL配置LoginUrlAuthenticationEntryPoint，就像我们在上面做的那样，并将其设置在<code>ExceptionTranslationFilter</code>上。</span></li>
 	<li>实现登录页面（使用JSP或者MVC controller）</li>
 	<li>在应用程序上下文中配置<span> </span><code>UsernamePasswordAuthenticationFilter</code>的实例</li>
 	<li>添加filter bean到filter chain proxy中（确保你过滤器的顺序是正确的）</li>
</ul>
登录表单仅仅包含了<code class="cye-lm-tag">username</code>和<span> </span><code>password</code><span> 输入字段，然后POST到受过滤器监控的URL地址（默认是/login）。基本的过滤器配置是这样的：</span>

```xml
<bean id="authenticationFilter" class=
"org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter">
<property name="authenticationManager" ref="authenticationManager"/>
</bean>
```

<h3>15.4.1 身份验证成功或失败时的工作流</h3>
<span>过滤器调用配置的AuthenticationManager来处理每个身份验证请求。身份验证成功和验证失败的结果分别由 <code>AuthenticationSuccessHandler</code>和<code class="cye-lm-tag">AuthenticationFailureHandler</code> 接口控制。可以使用过滤器的属性来设置这两个处理器，所以你可以完全自定义处理流程<sup class="footnote">[<a id="_footnoteref_14" class="footnote" href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#_footnote_14" title="View footnote.">14</a>]</sup>。现成的标准实现有：<code class="cye-lm-tag">SimpleUrlAuthenticationSuccessHandler</code>, <code>SavedRequestAwareAuthenticationSuccessHandler</code>, <code>SimpleUrlAuthenticationFailureHandler</code>, <code class="cye-lm-tag">ExceptionMappingAuthenticationFailureHandler</code> and <code>DelegatingAuthenticationFailureHandler</code>。要了解他们如何工作，支持哪些功能，请阅读这些类和<code class="cye-lm-tag">AbstractAuthenticationProcessingFilter</code>的Javadoc文档。</span>

如果身份验证成功，<code>Authentication</code>对象就会被放到<code>SecurityContextHolder</code>中。然后配置的<code>AuthenticationSuccessHandler</code>会重定向或者引导用户到合适的地址。默认情况下使用<code>SavedRequestAwareAuthenticationSuccessHandler</code>，也就是说用户会被重定向到登录前的请求地址。

&#x2660; 注意：<code>ExceptionTranslationFilter</code>缓存了用户的原始请求。当用户进行身份认证时，<span>请求处理程序利用这个缓存的请求来获得原始的URL并重定向到它。然后原始的请求被重新构建并作为新的替代。</span>

如果身份验证失败，配置的<span> </span><code class="cye-lm-tag">AuthenticationFailureHandler</code>会被调用。
<h1>16 Servlet API integration</h1>
这一章将讲述如何集成Spring Security和Servlet API。例子<a href="https://github.com/spring-projects/spring-security/tree/master/samples/xml/servletapi">servletapi-xml</a>展示了集成的方法。
<h2>16.1 Servlet 2.5+ 集成</h2>
<h3>16.1.1 HttpServletRequest.getRemoteUser()</h3>
<a href="https://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletRequest.html#getRemoteUser()">HttpServletRequest.getRemoteUser()</a>方法会返回<code class="cye-lm-tag">SecurityContextHolder.getContext().getAuthentication().getName()</code><span> 的结果，结果代表了当前的用户名。如果你想在程序中展示当前用户名时这个方法非常有用。另外，通过检查结果是否为null可以知道用户是已经通过身份验证还是匿名状态。针对用户的身份验证状态来决定显示特定的UI界面时，这个方法也非常有用（比如，只对验证通过的用户显示链接）。</span>
<h3>16.1.2 HttpServletRequest.getUserPrincipal()</h3>
<a href="https://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletRequest.html#getUserPrincipal()">HttpServletRequest.getUserPrincipal()</a><span> 方法会返回<code>SecurityContextHolder.getContext().getAuthentication()</code>的返回值。也就是说在使用基于username和password的身份验证机制时，这个返回值就是一个 <code>Authentication</code> 对象，实际上是<code class="cye-lm-tag">UsernamePasswordAuthenticationToken</code> 对象。如果你需要用户详细的信息时这个方法比较有用。例如，自定义的 <code>UserDetailsService</code>返回的<code>UserDetails</code> 中包含用户的姓和名。你可以通过下面的方式来获取信息：</span>

```java
Authentication auth = httpServletRequest.getUserPrincipal();
// assume integrated custom UserDetails called MyCustomUserDetails
// by default, typically instance of UserDetails
MyCustomUserDetails userDetails = (MyCustomUserDetails) auth.getPrincipal();
String firstName = userDetails.getFirstName();
String lastName = userDetails.getLastName();
```

&#x2663;注意：<span>应该注意的是，在整个应用程序中执行如此多的逻辑通常是一种糟糕的做法。你应该使用集中化处理，降低Spring Security与Servlet API的耦合</span>

<h3>16.1.3 HttpServletRequest.isUserInRole(String)</h3>

<span> </span><a href="https://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletRequest.html#isUserInRole(java.lang.String)">HttpServletRequest.isUserInRole(String)</a><span> 决定<code class="cye-lm-tag">SecurityContextHolder.getContext().getAuthentication().getAuthorities()</code>返回的值是否包含Role为String的<code>GrantedAuthority</code>对象。通常，用户不应该将“ROLE_”前缀传递到这个方法中，因为它是自动添加的。例如，如果你想知道当前用户是否具有“ROLE_ADMIN”权限，你可以这样做：</span>

```java
boolean isAdmin = httpServletRequest.isUserInRole("ADMIN");
```

在决定是否显示特定的UI界面时这非常有用。例如当用户是admin时，你可以显示admin的链接。

<h2>16.2 Servlet 3+ 集成</h2>

下面的章节介绍了如何将Servlet 3的方法与Spring Security集成
<h3>16.2.1 HttpServletRequest.authenticate(HttpServletRequest,HttpServletResponse)</h3>
<span> </span><a href="https://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletRequest.html#authenticate%28javax.servlet.http.HttpServletResponse%29">HttpServletRequest.authenticate(HttpServletRequest,HttpServletResponse)</a>方法可以用来确保用户身份验证已经通过。如果没有通过验证，将使用配置的<span> AuthenticationEntryPoint让用户进行身份验证（也就是重定向到登录页面）。</span>
<h3>16.2.2 HttpServletRequest.login(String,String)</h3>
<a href="https://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletRequest.html#login%28java.lang.String,%20java.lang.String%29">HttpServletRequest.login(String,String)</a><span> 方法可以使用当前的 <code>AuthenticationManager</code>来验证用户。比如下面的代码试图验证用户名为“user”，密码为“password”的用户：</span>

```java
try {
httpServletRequest.login("user","password");
} catch(ServletException e) {
// fail to authenticate
}
```
&#x2665;注意：如果你想让Spring Security来处理失败的认证，那就不需要使用catch来捕捉<span>ServletException异常</span>
<h3>16.2.3 HttpServletRequest.logout()</h3>
<span> </span><a href="https://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletRequest.html#logout%28%29">HttpServletRequest.logout()</a><span> 方法可以用来登出当前用户。</span>

<span>通常，这意味着安全上下文将被清除，HttpSession将被注销，任何“Remember Me”身份验证都将被清理，等等。</span>
<h3>16.2.4 AsyncContext.start(Runnable)</h3>
<span> </span><a href="https://docs.oracle.com/javaee/6/api/javax/servlet/AsyncContext.html#start%28java.lang.Runnable%29">AsynchContext.start(Runnable)</a><span> 方法可以确保你的凭证能传播到新的线程。使用Spring Security的并发支持，Spring Security重写了AsyncContext.start(Runnable) 方法，确保在处理Runnable时使用的是当前的SecurityContext。例如，下面的内容将输出当前用户的Authentication对象：</span>

```java
final AsyncContext async = httpServletRequest.startAsync();
async.start(new Runnable() {
	public void run() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		try {
			final HttpServletResponse asyncResponse = (HttpServletResponse) async.getResponse();
			asyncResponse.setStatus(HttpServletResponse.SC_OK);
			asyncResponse.getWriter().write(String.valueOf(authentication));
			async.complete();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
});
```

<h3>16.2.5 <span>异步Servlet支持</span></h3>
<span>如果您使用的是基于Java的配置，那么您就可以开始了。如果您正在使用XML配置，那么有一些必要的更新。第一步是确保您已经更新了您的web.xml，至少使用3.0模式，如下所示：</span>

```xml
<web-app xmlns="http://java.sun.com/xml/ns/javaee"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd"
version="3.0">
</web-app>
```

<span>接下来，您需要确保您的springSecurityFilterChain是用来处理异步请求的。</span>

```xml
<filter>
<filter-name>springSecurityFilterChain</filter-name>
<filter-class>
	org.springframework.web.filter.DelegatingFilterProxy
</filter-class>
<async-supported>true</async-supported>
</filter>
<filter-mapping>
<filter-name>springSecurityFilterChain</filter-name>
<url-pattern>/*</url-pattern>
<dispatcher>REQUEST</dispatcher>
<dispatcher>ASYNC</dispatcher>
</filter-mapping>
```

<span>就这些。现在，Spring Security将确保您的SecurityContext也在异步请求上传播。</span>

<span>那么它是如何工作的呢？</span><span>如果您不是真正感兴趣的，请随意跳过本节的其余部分，否则请继续阅读。</span><span>其中大部分都是内置在Servlet规范中，但是Spring Security做了一些调整来确保正确地处理异步请求。在Spring Security 3.2之前，当HttpServletResponse被提交时，SecurityContextHolder的SecurityContext会自动保存。在异步环境中能会出现问题。例如，考虑以下几点：</span>

```java
httpServletRequest.startAsync();
new Thread("AsyncThread") {
	@Override
	public void run() {
		try {
			// Do work
			TimeUnit.SECONDS.sleep(1);

			// Write to and commit the httpServletResponse
			httpServletResponse.getOutputStream().flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}.start();
```

<span>问题是这个线程对于Spring Security来说是未知的，所以SecurityContext不会传播给它。这意味着当我们提交HttpServletResponse时，没有 SecuriytContext。当Spring Security自动将SecurityContext保存到HttpServletResponse时，它将丢失我们的登录用户。</span>

<span>从版本3.2开始，Spring Security已经足够智能，不再自动地在HttpServletRequest.startAsync()调用，HttpServletResponse提交时自动保存SecurityContext。</span>
<h2>16.3 Servlet 3.1+集成</h2>
<span>下面的部分描述了Spring Security集成的Servlet 3.1方法。</span>
<h3>16.3.1 HttpServletRequest#changeSessionId()</h3>
<a href="https://docs.oracle.com/javaee/7/api/javax/servlet/http/HttpServletRequest.html#changeSessionId()">HttpServletRequest.changeSessionId()</a><span> 方法是在Servlet 3.1和更高的版本中，防止会话固定攻击<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#ns-session-fixation">Session Fixation</a> 的默认方法。</span>
<h1>17 <span>基本的和摘要式身份验证</span></h1>
<span>基本和摘要身份验证是在web应用程序中流行的身份认证机制。基本认证通常用于无状态客户端，这些客户端在每个请求上传递他们的凭证。将其与基于表单的认证结合使用是很常见的，应用程序通过基于浏览器的用户界面或者作为web服务来进行身份验证。然而，基本身份验证将密码作为纯文本传输，因此它只应该在诸如HTTPS之类的加密传输层上使用。</span>
<h2>17.1 BasicAuthenticationFilter</h2>
<span>BasicAuthenticationFilter负责处理HTTP头部中的基本身份凭证。这可以用来对Spring remoting协议（比如Hessian和Burlap）发出的调用，或者对浏览器用户代理（例如Firefox和Internet Explorer）的请求进行身份验证。标准的管理HTTP基本认证是由RFC 1945第11节定义的，BasicAuthenticationFilter符合这个RFC。基本身份验证是一种很有吸引力的身份验证方法，因为它在用户代理中被广泛地部署，并且实现非常简单（它只是一个Base64编码的用户名：密码，在HTTP头中指定）。</span>
<h3>17.1.1 配置</h3>
<span>要实现HTTP基本身份验证，您需要向过滤器链添加BasicAuthenticationFilter。应用程序上下文应该包含BasicAuthenticationFilter及其所需的合作者：</span>

```xml
<bean id="basicAuthenticationFilter"
class="org.springframework.security.web.authentication.www.BasicAuthenticationFilter">
<property name="authenticationManager" ref="authenticationManager"/>
<property name="authenticationEntryPoint" ref="authenticationEntryPoint"/>
</bean>
<bean id="authenticationEntryPoint"
class="org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint">
<property name="realmName" value="Name Of Your Realm"/>
</bean>
```

<span>AuthenticationManager处理每个身份验证请求。</span><span>如果身份验证失败，配置的AuthenticationEntryPoint将被用于重试身份验证过程。</span><span>通常，过滤器与BasicAuthenticationEntryPoint结合使用，后者返回一个401响应，并使用适当的头部来重试HTTP基本身份验证。如果身份验证成功，那么产生的认证对象将会像往常一样被放置到<code>SecurityContextHolder</code> 中。</span>

<span>如果身份验证事件成功，或者因为HTTP头不包含受支持的身份验证请求而没有尝试认证，那么过滤器链将照常运行。过滤器链被中断的前提是身份验证失败，并且调用AuthenticationEntryPoint。</span>
<h2>17.2 DigestAuthenticationFilter</h2>
<span>DigestAuthenticationFilter能够处理HTTP报头中提供的摘要身份凭证。摘要身份验证试图解决基本身份验证的许多弱点，具体来说，通过确保凭证不会在整个网络中以明文形式发送。许多用户代理支持摘要认证，包括Mozilla Firefox和Internet Explorer。规范HTTP摘要认证的标准是由RFC 2617定义的，是RFC 2069规定的早期摘要认证标准的更新版本。大多数用户代理实现了RFC 2617。Spring Security的DigestAuthenticationFilter与RFC 2617规定的“auth”保护（qop）兼容，还提供了与RFC 2069的向后兼容性。如果您需要使用未加密的HTTP（即不使用tls/https），并希望最大程度地保护认证过程，那么摘要身份验证是一个更有吸引力的选项。实际上，摘要验证是WebDAV协议的强制性要求，正如RFC 2518 17.1节所指出的那样。</span>

&#x2666;注意：<span>您不应该在现代应用程序中使用摘要，因为它是不安全的。最明显的问题是，您必须将密码存储在明文、加密或MD5格式中。所有这些存储格式都被认为是不安全的。相反，您应该使用一种自适应密码散列（即bCrypt、PBKDF2、SCrypt等）。</span>

<span>摘要身份验证的核心是“nonce”。</span><span>这是服务器生成的值。Spring Security的nonce采用以下格式：</span>
<pre class="prettyprint">base64(expirationTime + ":" + md5Hex(expirationTime + ":" + key))
expirationTime:   The date and time when the nonce expires, expressed in milliseconds
key:              A private key to prevent modification of the nonce token</pre>
<span><code>DigestAuthenticatonEntryPoint</code> 有一个属性，指定用于生成nonce令牌的密钥，以及用于确定失效时间的<code>nonceValiditySeconds</code>属性（默认300，也就是5分钟）。一旦nonce是有效的，那么摘要是通过连接各种字符串来计算的，包括用户名、密码、nonce、被请求的URI、客户端生成的nonce（仅仅是用户代理为每个请求生成的随机值）、域名等，然后用MD5加密。服务器和用户代理都要执行这个摘要计算，如果摘要包含的值（例如密码）不同，那么就会产生不同的hash值。在Spring Security中，如果服务器生成的nonce已过期（但是摘要是有效的），<code>DigestAuthenticatonEntryPoint</code>将发送一个<code class="cye-lm-tag">"stale=true"</code>的消息头。这告诉用户代理不需要打扰用户（因为密码和用户名等是正确的），只需要使用一个新的nonce重试一下就行。</span>

<span>对于<code>DigestAuthenticatonEntryPoint</code>的<code>nonceValiditySeconds</code> 参数的适当值取决于您的应用程序。非常安全的应用程序应该注意，在nonce中<code>expirationTime</code>到期之前，被拦截的身份验证头可以被用来模拟主体。在选择合适的设置时，这是关键原则，但是对于非常安全的应用程序来说，在实例中不使用TLS/HTTPS是不太正常的。</span>

<span>由于摘要身份验证的实现复杂，用户代理经常出现问题。例如，Internet Explorer未能在同一会话中向后续请求呈现一个“opaque”令牌。因此，Spring Security过滤器将所有状态信息封装到“nonce”令牌中。在我们的测试中，Spring Security的实现可以可靠地与Mozilla Firefox和Internet Explorer一起工作，正确处理nonce超时等。</span>
<h3>17.2.1 配置</h3>
我们已经学习了相关的原理，现在来看一下如何使用。为了实现HTTP摘要验证，需要在过滤链中定义<span> </span><code>DigestAuthenticationFilter</code>过滤器。应用程序上下文需要定义<span> </span><code>DigestAuthenticationFilter</code>bean以及它的需要的bean：

```xml
<bean id="digestFilter" class=
	"org.springframework.security.web.authentication.www.DigestAuthenticationFilter">
<property name="userDetailsService" ref="jdbcDaoImpl"/>
<property name="authenticationEntryPoint" ref="digestEntryPoint"/>
<property name="userCache" ref="userCache"/>
</bean>
<bean id="digestEntryPoint" class=
"org.springframework.security.web.authentication.www.DigestAuthenticationEntryPoint">
<property name="realmName" value="Contacts Realm via Digest Authentication"/>
<property name="key" value="acegi"/>
<property name="nonceValiditySeconds" value="10"/>
</bean>
```

<span><code class="cye-lm-tag">UserDetailsService</code>配置是必须的，因为DigestAuthenticationFilter必须能直接访问用户的明文密码。如果您在DAO中使用了编码的密码，那么摘要认证将无法工作<sup class="footnote">[<a id="_footnoteref_15" class="footnote" href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#_footnote_15" title="View footnote.">15</a>]</sup>。DAO合作者，以及UserCache，通常都是直接与<code class="cye-lm-tag">DaoAuthenticationProvider</code>共享的。<code class="cye-lm-tag">authenticationEntryPoint</code>属性必须是<code class="cye-lm-tag">DigestAuthenticationEntryPoint</code>，这样DigestAuthenticationFilter才可以获得正确的域名<code class="cye-lm-tag">realmName</code> 和 <code class="cye-lm-tag">key</code>来计算摘要。</span>

<span>与BasicAuthenticationFilter一样，如果身份验证成功，<code class="cye-lm-tag">Authentication</code>请求令牌将被放置到<code class="cye-lm-tag">SecurityContextHolder</code>中。如果身份验证事件成功，或者由于HTTP头没有包含一个摘要身份验证请求而没有尝试认证，那么过滤器链将照常运行。过滤器链被中断的惟一条件是身份验证失败，并且调用AuthenticationEntryPoint。</span>

<span>摘要验证的RFC提供了一系列额外的特性，以进一步提高安全性。例如，更改每个请求的nonce。尽管如此，Spring Security的设计目的是将实现的复杂性降到最低（并且毫无疑问会出现用户代理的不兼容性），并且避免存储服务器端状态。如果您希望更详细地研究这些特性，请您查看RFC 2617。据我们所知，Spring Security的实现确实符合RFC的最低标准。</span>
<h1>18 Remember-Me Authentication</h1>
<h2>18.1 Overview</h2>
<span>Remember-me或 persistent-login(持久登录)认证指的是web站点能够记住会话之间的主体的身份。这通常是通过向浏览器发送cookie来完成的，在未来的会话中会检测到cookie，并导致自动登录。Spring Security为这些操作提供了必要的钩子，并且有两个具体的remember-me实现。一个使用哈希来保存基于cookie的令牌的安全性，另一个使用数据库或其他持久性存储机制来存储生成的令牌。</span>

<span>注意，这两个实现都需要一个UserDetailsService。如果您使用的是不使用UserDetailsService（例如，LDAP提供者）的身份验证提供者，那么它将无法工作，除非您在应用程序上下文中定义一个UserDetailsService bean。</span>
<h2>18.2 Simple Hash-Based Token Approach</h2>
<span>这种方法使用哈希来实现一个有用的remember-me 策略。本质上，cookie是通过成功的交互式认证发送到浏览器的，cookie的组成如下:</span>
<pre class="prettyprint">base64(username + ":" + expirationTime + ":" +
md5Hex(username + ":" + expirationTime + ":" password + ":" + key))

username:          As identifiable to the UserDetailsService
password:          That matches the one in the retrieved UserDetails
expirationTime:    The date and time when the remember-me token expires, expressed in milliseconds
key:               A private key to prevent modification of the remember-me token</pre>
<span> remember-me令牌仅在指定的时间段内有效，前提是用户名、密码和密钥没有发生更改。值得注意的是，这有一个潜在的安全问题，即在令牌过期之前，被抓取的remember-me令牌可以被任何用户代理使用。摘要身份验证也存在这个问题。如果一个主体意识到一个令牌已经被捕获，他们可以很容易地更改他们的密码，并立即将remember-me 令牌标记为无效。如果需要更强的安全性，您应该使用下一节中描述的方法。或者说，不使用remember-me 服务。</span>

<span>如果对<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#ns-config">命名空间配置章节</a>中所讨论的内容熟悉的话，您可以通过添加<code>&lt;remember-me&gt;</code>元素来启用remember-me认证：</span>

```xml
<http>
...
<remember-me key="myAppKey"/>
</http>
```

<span>通常会自动选择UserDetailsService。如果您的应用程序上下文中有多个UserDetailsService，您需要指定 <code>user-service-ref</code>属性的值，这个值是UserDetailsService bean的名称。</span>
<h2>18.3 Persistent Token Approach</h2>
<span>这种方法基于以下内容： <a href="http://jaspan.com/improved_persistent_login_cookie_best_practice">http://jaspan.com/improved_persistent_login_cookie_best_practice</a>，并进行了一些细微的修改<sup class="footnote">[<a id="_footnoteref_16" class="footnote" href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#_footnote_16" title="View footnote.">16</a>]</sup>。要使用命名空间配置这种方法，您需要提供一个数据源引用：</span>

```xml
<http>
...
<remember-me data-source-ref="someDataSource"/>
</http>
```

<span>数据库应该包含一个<code>persistent_logins</code>表，使用下列SQL（或等效）创建：</span>

```
create table persistent_logins (username varchar(64) not null,
series varchar(64) primary key,
token varchar(64) not null,
last_used timestamp not null)
```

<h2>18.4 Remember-Me接口和实现</h2>
<span>Remember-me与UsernamePasswordAuthenticationFilter一起使用，并且是通过AbstractAuthenticationProcessingFilter超类中的钩子实现的。它也在BasicAuthenticationFilter中使用。钩子将在适当的时候调用一个具体的<code>RememberMeServices</code>。Remember-me接口是这样的：</span>

```java
Authentication autoLogin(HttpServletRequest request, HttpServletResponse response);

void loginFail(HttpServletRequest request, HttpServletResponse response);

void loginSuccess(HttpServletRequest request, HttpServletResponse response,
	Authentication successfulAuthentication);
```

<span>尽管在这个阶段，AbstractAuthenticationProcessingFilter只调用了loginFail()和loginSuccess()两个方法，你可以参考Javadoc来了解方法具体做了些什么。每当<code>SecurityContextHolder</code>不包含<code class="cye-lm-tag">Authentication</code>时，<code>RememberMeAuthenticationFilter</code> 就会调用<code>autoLogin()</code>方法。因此，该接口提供了底层的remember-me实现，并提供了与认证相关事件的充分通知，并在web请求可能包含cookie并希望被记住时，将其委托给实现类。这种设计允许无数个remember-me的实现。我们已经看了两个Spring Security提供的实现，现在来重新看一下。</span>
<h3>18.4.1 TokenBasedRememberMeService</h3>
<span>这个实现支持18.2 <a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#remember-me-hash-token">Simple Hash-Based Token Approach</a>中所描述的身份验证。TokenBasedRememberMeServices生成一个<code>RememberMeAuthenticationToken</code>，这个token是由RememberMeAuthenticationProvider提供的。这个身份验证provider和TokenBasedRememberMeServices之间会共享一个key密钥。此外TokenBasedRememberMeServices需要一个UserDetailsService 来检索用户名和密码，并用来比较签名，同时生成包含正确的<code>GrantedAuthority</code> 的<code>RememberMeAuthenticationToken</code>。如果用户请求该cookie，则应用程序应该提供某种注销cookie的命令。<code>TokenBasedRememberMeServices</code> 也实现了Spring Security的LogoutHandler接口，因此可以与LogoutFilter一起使用，用于自动清除cookie。</span>

<span>下面是启用remember-me服务时，需要在应用程序上下文中添加的bean：</span>

```xml
<bean id="rememberMeFilter" class=
"org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter">
<property name="rememberMeServices" ref="rememberMeServices"/>
<property name="authenticationManager" ref="theAuthenticationManager" />
</bean>

<bean id="rememberMeServices" class=
"org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices">
<property name="userDetailsService" ref="myUserDetailsService"/>
<property name="key" value="springRocks"/>
</bean>

<bean id="rememberMeAuthenticationProvider" class=
"org.springframework.security.authentication.RememberMeAuthenticationProvider">
<property name="key" value="springRocks"/>
</bean>
```

不要忘了将你的<span> </span><code>RememberMeServices</code>实现添加到<code>UsernamePasswordAuthenticationFilter.setRememberMeServices()</code>里，把<span> </span><code>RememberMeAuthenticationProvider</code>添加到<code class="cye-lm-tag">AuthenticationManager.setProviders()</code><span> 列表中，还要把<code>RememberMeAuthenticationFilter</code> 添加都FilterChainProxy里（通常是之间在您的UsernamePasswordAuthenticationFilter后面）。</span>
<h3>18.4.2 PersistentTokenBasedRememberMeServices</h3>
<span>这个类的使用方式与<code>TokenBasedRememberMeServices</code>的一样，但是它还需要配置一个<code>PersistentTokenRepository</code>来储存令牌。有两个标准的实现。</span>
<ul>
 	<li><code>InMemoryTokenRepositoryImpl</code><span> ，只用于测试</span></li>
 	<li><code>JdbcTokenRepositoryImpl</code>，把token存储在数据库中</li>
</ul>
数据库策略在<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#remember-me-persistent-token">Persistent Token Approach</a>中有详细描述。


<h1>20 CORS</h1>
Spring Frameword为CORS（Cross Origin Resource Sharing）提供了一流的支持。<span>CORS必须在Spring Security之前进行处理，因为<a href="https://www.jianshu.com/p/b55086cbd9af">预检请求 pre-flight request </a>将不包含任何cookie（即JSESSIONID）。如果请求不包含任何cookie，并且Spring Security先于预检请求处理，那么Spring Security会认为这个请求的用户没有经过身份验证（因为请求中没有cookie）并拒绝它。</span>

<span>确保CORS首先处理的最简单的方法是使用CorsFilter。用户可以使用以下内容提供<code>CorsConfigurationSource</code>，从而将CorsFilter与Spring Security集成在一起：</span>

```java
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			// by default uses a Bean by the name of corsConfigurationSource
			.cors().and()
			...
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.asList("https://example.com"));
		configuration.setAllowedMethods(Arrays.asList("GET","POST"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
```

或者使用XML：

```xml
<http>
	<cors configuration-source-ref="corsSource"/>
	...
</http>
<b:bean id="corsSource" class="org.springframework.web.cors.UrlBasedCorsConfigurationSource">
	...
</b:bean>
```

<span>如果您正在使用Spring MVC的CORS支持，您可以忽略指定CorsConfigurationSource，Spring Security将利用提供给Spring MVC的CORS配置：</span>

```java
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			// if Spring MVC is on classpath and no CorsConfigurationSource is provided,
			// Spring Security will use CORS configuration provided to Spring MVC
			.cors().and()
			...
	}
}
```

或者XML：

```xml
<http>
	<!-- Default to Spring MVC's CORS configuration -->
	<cors />
	...
</http>
```

<h1>21 Security HTTP Response Headers</h1>
<span>本节讨论Spring Security在响应中添加各种安全头部的内容。</span>
<h2>21.1 Default Security Headers</h2>
<span>Spring Security允许用户轻松地注入默认的安全头，以帮助保护他们的应用程序。Spring Security默认添加的安全头包括：</span>

```
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Content-Type-Options: nosniff
Strict-Transport-Security: max-age=31536000 ; includeSubDomains
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
```

&#x2660;注意：<span>Strict-Transport-Security只给HTTPS请求添加</span>

要查看以上安全头部的详细介绍，请参考相关章节：
<ul>
 	<li>
<p class="cye-lm-tag"><a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#headers-cache-control">Cache Control</a></p>
</li>
 	<li>
<p class="cye-lm-tag"><a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#headers-content-type-options">Content Type Options</a></p>
</li>
 	<li>
<p class="cye-lm-tag"><a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#headers-hsts">HTTP Strict Transport Security</a></p>
</li>
 	<li>
<p class="cye-lm-tag"><a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#headers-frame-options">X-Frame-Options</a></p>
</li>
 	<li>
<p class="cye-lm-tag"><a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#headers-xss-protection">X-XSS-Protection</a></p>
</li>
</ul>
<span>虽然每个头都被认为是最佳实践，但是需要注意的是，并不是所有的客户端都会使用这些头部，因此最好做一些额外的测试。</span>

<span>您可以自定义特殊的头部。例如，假设您希望您的HTTP响应头看起来像下面这样：</span>

```
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
X-Content-Type-Options: nosniff
X-Frame-Options: SAMEORIGIN
X-XSS-Protection: 1; mode=block
```

<span>特别地，如果您希望所有头部都默认具有以下定制：</span>
<ul>
 	<li>
<p class="cye-lm-tag"><a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#headers-frame-options">X-Frame-Options</a><span> 允许相同域名下的所有请求</span></p>
</li>
 	<li>
<p class="cye-lm-tag"><a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#headers-hsts">HTTP Strict Transport Security (HSTS)</a><span> 不添加到响应中</span></p>
</li>
</ul>
你可以使用以下配置来实现：

```java
@EnableWebSecurity
public class WebSecurityConfig extends
		WebSecurityConfigurerAdapter {
@Override
protected void configure(HttpSecurity http) throws Exception {
	http
		// ...
		.headers()
			.frameOptions().sameOrigin()
			.httpStrictTransportSecurity().disable();
}

}
```

<span>或者使用XML：</span>

```xml
<http>
	<!-- ... -->

	<headers>
		<frame-options policy="SAMEORIGIN" />
		<hsts disable="true"/>
	</headers>
</http>
```
<span>如果您不希望默认添加这些头部，并且希望进行显式控制，那么可以禁用添加头部的默认行为。你可以使用Java配置或XML进行配置。</span>

<span>如果您使用的是Spring Security的Java配置，那么下面只会增加缓存控制：</span>

```xml
<http>
	<!-- ... -->

	<headers>
		<frame-options policy="SAMEORIGIN" />
		<hsts disable="true"/>
	</headers>
</http>
```
```java
@EnableWebSecurity
public class WebSecurityConfig extends
WebSecurityConfigurerAdapter {

@Override
protected void configure(HttpSecurity http) throws Exception {
	http
	// ...
	.headers()
		// do not use any default headers unless explicitly listed
		.defaultsDisabled()
		.cacheControl();
}
}
```
<span>下面的XML只会增加缓存控制：</span>

```xml
<http>
	<!-- ... -->

	<headers defaults-disabled="true">
		<cache-control/>
	</headers>
</http>
```
<span>如果有必要，您可以使用以下Java配置禁用所有HTTP安保响应头：</span>

```java
@EnableWebSecurity
public class WebSecurityConfig extends
WebSecurityConfigurerAdapter {

@Override
protected void configure(HttpSecurity http) throws Exception {
	http
	// ...
	.headers().disable();
}
}
```
<span>如果有必要，您可以使用下面的XML配置禁用所有HTTP安全响应头：</span>
```xml
<http>
	<!-- ... -->

	<headers disabled="true" />
</http>
```
<h3>21.1.1 Cache Control</h3>
以前Spring Security会要求你自己实现cache的控制。那个时候有这样的要求也是情有可原，但是现在浏览器已经可以包含安全连接的缓存<span>。这意味着用户可以查看经过身份验证的页面，登出，然后恶意用户可以使用浏览器历史查看缓存的页面。为了增加安全性，Spring Security增加了缓存控制支持，它将在您的响应中插入下列头部:</span>

```
Cache-Control: no-cache, no-store, max-age=0, must-revalidate
Pragma: no-cache
Expires: 0
```
<span>简单地添加没有子元素的 <a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#nsa-headers">&lt;headers</a>&gt;元素将自动添加缓存控制和其他一些保护措施。但是，如果您只想要缓存控制，那么您可以使用Spring Security的XML命名空间和<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#nsa-headers-defaults-disabled">headers@defaults-disabled</a> 属性来启用这个特性。</span>

```xml
<http>
	<!-- ... -->

	<headers defaults-disable="true">
		<cache-control />
	</headers>
</http>
```
<span>类似地，您可以在Java配置中只启用缓存控制，如下：</span>

```java
@EnableWebSecurity
public class WebSecurityConfig extends
WebSecurityConfigurerAdapter {

@Override
protected void configure(HttpSecurity http) throws Exception {
	http
	// ...
	.headers()
		.defaultsDisabled()
		.cacheControl();
}
}
```
<span>如果您确实想要缓存特定的响应，您的应用程序可以有选择地调用 <a href="https://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletResponse.html#setHeader(java.lang.String,java.lang.String)">HttpServletResponse.setHeader(String,String)</a> 来覆盖Spring Security设置的头部。这对于确保CSS、JavaScript和图像的正确缓存是很有用的。</span>

<span>当使用Spring Web MVC时，这通常是在您的配置中完成的。</span><span>例如，下面的配置将确保为您的所有资源设置缓存头部：</span>

```java
@EnableWebMvc
public class WebMvcConfiguration implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry
			.addResourceHandler("/resources/**")
			.addResourceLocations("/resources/")
			.setCachePeriod(31556926);
	}

	// ...
}
```
<h3>21.1.2 Content Type Options</h3>
<span>浏览器，包括Internet Explorer，会尝试使用内容嗅探<a href="https://en.wikipedia.org/wiki/Content_sniffing">content sniffing</a>来猜测请求的内容类型。通过猜测没有设置content type的资源的内容类型，可以提高浏览器的用户体验。例如，浏览器遇到没有指定content type的JavaScript文件时，通过猜测文件的内容类型来执行JavaScript。</span>

&#x2666;注意：<span>在允许上传内容时，还有很多其他的事情要做（如：也就是说，只在一个不同的域中显示文档，设置Content-Type头部，sanitize the document文档杀毒？，等等）。然而，这些操作超出了Spring Security的能力范围。还有一点需要指出，当内容嗅探关闭时，必须设置content type来确保程序正常。</span>

内容嗅探的问题是，它允许恶意用户使用<span>polyglots（也就是该文件在多种content type下都是合法的）来进行XSS跨站脚本攻击。例如，某些网站允许用户提交一个有效的postscript文档到网站，然后进行查看。恶意用户可能会创建一个<a href="http://webblaze.cs.berkeley.edu/papers/barth-caballero-song.pdf">postscript文件，而这个文档同时又是一个有效的JavaScript文件</a>，然后使用这个文件进行XSS跨站脚本攻击。</span>

内容嗅探可以通过给响应添加下面的头部消息来禁用：
<pre class="prettyprint">X-Content-Type-Options: nosniff</pre>
和cache control元素一样，使用没有子元素的&lt;headers&gt;元素时，nosniff指令也是默认添加的。<span>如果你想要对添加的头信息进行更多的控制，你可以使用 <a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#nsa-content-type-options">&lt;content-type-options</a>&gt; 元素和 <a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#nsa-headers-defaults-disabled">headers@defaults-disabled</a> 属性：</span>

```xml
<http>
	<!-- ... -->

	<headers defaults-disabled="true">
		<content-type-options />
	</headers>
</http>
```

使用Spring Security的Java配置时，<span> X-Content-Type-Options头部是默认添加的。如果你想要对添加的头信息进行更多的控制，参考下面的代码：</span>

```java
@EnableWebSecurity
public class WebSecurityConfig extends
WebSecurityConfigurerAdapter {

@Override
protected void configure(HttpSecurity http) throws Exception {
	http
	// ...
	.headers()
		.defaultsDisabled()
		.contentTypeOptions();
}
}
```
<h3>21.1.3 HTTP Strict Transport Security(HSTS)</h3>
当你输入你的银行网站的时候，你输的是mybank.example.com还是https://mybank.example.com?如果你忽略了https协议，那么你很有可能会受到中间人攻击<span> </span><a href="https://en.wikipedia.org/wiki/Man-in-the-middle_attack">Man in the Middle attacks</a>。即使网站引导你重定向到https://mybank.example.com，攻击者依然可以拦截最初的HTTP请求并篡改响应（也就是重定向到https://mibank.example.com，然后窃取你的凭证）。

许多用户忽略了https协议，所以有了<a href="https://tools.ietf.org/html/rfc6797">HTTP Strict Transport Security (HSTS)</a><span> 。一旦mybank.example.com添加为了一个<a href="https://tools.ietf.org/html/rfc6797#section-5.1">HSTS host</a>，浏览器就可以提前知道所有mybank.example.com的请求都应该解释为https://mybank.example.com。这大大降低了中间人攻击发生的可能性。</span>

&#x2660;注意：根据<a href="https://tools.ietf.org/html/rfc6797#section-7.2">RFC6797</a>，HSTS头部只能注入到HTTPS的响应中。为了让浏览器知道这个头部，<span>浏览器必须首先信任签署了SSL证书的CA（数字证书管理机构），而不仅仅是SSL证书。其中SSL证书用于建立安全连接。</span>

<span>将站点标记为HSTS主机的一种方法是将主机预先加载到浏览器中。另一种方法是在响应中加入"Strict-Transport-Security" 头部信息。例如下面的头部将指示浏览器把域名当做HSTS主机，有效期1年（一年约31536000秒）</span>
<pre class="prettyprint">Strict-Transport-Security: max-age=31536000 ; includeSubDomains</pre>
可选参数<span> includeSubDomains，指示Spring Security应该把subdomains（也就是 secure.mybank.example.com）也当做HSTS域名来看待。</span>

和其他头部一样，Spring Security也是默认添加HSTS的。你可以使用<span> </span><a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#nsa-hsts">&lt;hsts</a><span>&gt;元素来自定义HSTS头部：</span>
```xml
<http>
	<!-- ... -->

	<headers>
		<hsts
			include-subdomains="true"
			max-age-seconds="31536000" />
	</headers>
</http>
```
类似地，你可以只使用HSTS头部：
```java
@EnableWebSecurity
public class WebSecurityConfig extends
WebSecurityConfigurerAdapter {

@Override
protected void configure(HttpSecurity http) throws Exception {
	http
	// ...
	.headers()
		.httpStrictTransportSecurity()
			.includeSubdomains(true)
			.maxAgeSeconds(31536000);
}
}
```
<h3>21.1.4 HTTP Public Key Pinning（HPKP）</h3>
HTTP Public Key Pinning（公钥固定，HPKP）是一种安全机制，<span>它告诉web客户端将特定的加密公钥与特定的web服务器关联起来，以防止攻击者使用伪造的证书进行中间人（MITM）攻击。</span>

<span>为了确保TLS会话中使用的服务器公钥的真实可靠，这个公钥被通常被包装成一个x.509证书，这个证书通常由证书颁发机构（CA）签署。像浏览器这样的Web客户端信任很多这样的CA，它们都可以为任意的域名创建证书。如果攻击者能够破坏单个CA，那么他们就可以对各种TLS连接执行MITM攻击。HPKP可以通过告诉客户端哪个公钥属于哪个web服务器，让HTTPS协议规避这种威胁。HPKP是一种Trust on First Use (TOFU，先用信任？)的技术。当web服务器第一次通过特殊的HTTP头部告诉客户端哪个公钥属于它时，在给定的时间段内客户端会一直保留这些数据。当客户端再次访问服务器时，客户端期望证书中包含公钥，公钥的指纹通过HPKP就可以知道。如果服务器提供了一个未知的公钥，客户端应该向用户发出警告。</span>

&#x2663;注意：由于用户代理<span>需要在SSL证书链上验证pin码，所以HPKP报头只被注入HTTPS响应中。</span>

<span>为您的站点启用这个特性就像在您的站点通过HTTPS访问时返回 Public-Key-Pins HTTP头一样简单。例如，下面的内容指示用户代理为两个pin，向给定的URI（通过<a href="https://tools.ietf.org/html/rfc7469#section-2.1.4"><strong><em>report-uri</em></strong></a> 指令）报告pin验证故障：</span>
<pre class="prettyprint">
Public-Key-Pins-Report-Only: max-age=5184000 ; pin-sha256="d6qzRu9zOECb90Uez27xWltNsj0e1Md7GkYYkVoZWmM=" ; pin-sha256="E9CZ9INDbd+2eRQozYqqbQ2yXLVKB9+xcprMF+44U1g=" ; report-uri="http://example.net/pkp-report" ; includeSubDomains
</pre>
<a href="https://tools.ietf.org/html/rfc7469#section-3">pin验证失败报告</a>是标准的JSON格式的，<span>可以由web应用程序自己的API或公开托管的HPKP报告服务捕获，比如<a href="https://report-uri.io/"><strong><em>REPORT-URI</em></strong></a>。</span>

<span>可选的includeSubDomains指令指示浏览器也使用给定的pin来验证子域名。</span>

与其他头部相反，Spring Security没有默认启用HPKP。你可以使用<span> </span><a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#nsa-hpkp">&lt;hpkp</a><span>&gt;元素来自定义HPKP头部：</span>

```xml
<http>
	<!-- ... -->

	<headers>
		<hpkp
			include-subdomains="true"
			report-uri="http://example.net/pkp-report">
			<pins>
					<pin algorithm="sha256">d6qzRu9zOECb90Uez27xWltNsj0e1Md7GkYYkVoZWmM=</pin>
					<pin algorithm="sha256">E9CZ9INDbd+2eRQozYqqbQ2yXLVKB9+xcprMF+44U1g=</pin>
			</pins>
		</hpkp>
	</headers>
</http>
```
类似地，使用Java配置HPKP头部：

```java
@EnableWebSecurity
public class WebSecurityConfig extends
WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
	// ...
	.headers()
	.httpPublicKeyPinning()
	    .includeSubdomains(true)
	    .reportUri("http://example.net/pkp-report")
	    .addSha256Pins("d6qzRu9zOECb90Uez27xWltNsj0e1Md7GkYYkVoZWmM=", "E9CZ9INDbd+2eRQozYqqbQ2yXLVKB9+xcprMF+44U1g=";
    }
}
```
<h3>21.1.5 X-Frame-Options</h3>
<span>允许网站被嵌入frame可能存在安全隐患。例如，使用一些CSS样式可以欺骗用户去点击一些他们本不想使用的东西（<a href="https://www.youtube.com/watch?v=3mk0RySeNsU">video demo</a>）。例如，已经登陆的银行用户可能会点击一个给其他用户进行授权的按钮。这种攻击叫做<a href="https://en.wikipedia.org/wiki/Clickjacking">Clickjacking</a>（点击劫持）。</span>

&#x2663;注意：其他用户处理clickjacking的技术有<span> </span><a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#headers-csp">Content Security Policy (CSP)</a>。

有很多方法可以防御clickjacking攻击。例如为了保护传统浏览器免受clickjacking的攻击，你可以使用<span> </span><a href="https://www.owasp.org/index.php/Clickjacking_Defense_Cheat_Sheet#Best-for-now_Legacy_Browser_Frame_Breaking_Script">frame breaking code</a>。<span>虽然不是完美的，但是对于传统的浏览器来说 frame breaking code 已经是最好的解决办法了。</span>

一种更现代的处理clickjacking的方法是使用<span> </span><a href="https://developer.mozilla.org/en-US/docs/HTTP/X-Frame-Options">X-Frame-Options</a>头：
<pre class="prettyprint">X-Frame-Options: DENY</pre>
X-Frame-Options响应头指示浏览器阻止任何响应中包含这个响应头的站点在frame中渲染。Spring Security默认禁止在frame中渲染。

你可以使用<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#nsa-frame-options">frame-options</a><span> 元素来自定义X-Frame-Options。例如，下面的代码会指示Spring Security使用“X-Frame-Options: SAMEORIGIN”，允许在相同的域名下使用iframe渲染：</span>

```xml
<http>
	<!-- ... -->

	<headers>
		<frame-options
		policy="SAMEORIGIN" />
	</headers>
</http>
```
类似地，在Java配置中，你可以通过下面的代码自定义，让frame options使用相同域名：
```java
@EnableWebSecurity
public class WebSecurityConfig extends
WebSecurityConfigurerAdapter {

@Override
protected void configure(HttpSecurity http) throws Exception {
	http
	// ...
	.headers()
		.frameOptions()
			.sameOrigin();
}
}
```
<h3>21.1.6 X-XSS-Protection</h3>
一些浏览器内建支持过滤<span> </span><a href="https://www.owasp.org/index.php/Testing_for_Reflected_Cross_site_scripting_(OWASP-DV-001)">reflected XSS attacks</a>（<span>反映XSS攻击</span>）。虽然这<span>不是万无一失的，但确实有助于XSS的保护。</span>

这个过滤器默认是开启的，所以添加头部只是确保它已经开启，然后指示浏览器在检测到XSS攻击的时候应该怎么做。例如，过滤器可能尝试使用侵入性最小的方式来改变内容，达到正常渲染的目的。<span>有时，这种类型的替换本身就会成为XSS攻击的弱点（ <a href="https://hackademix.net/2009/11/21/ies-xss-filter-creates-xss-vulnerabilities/">XSS vulnerability in itself</a>）。相反，最好的方式是屏蔽内容，而不是试图修复它，如下：</span>
<pre class="prettyprint">X-XSS-Protection: 1; mode=block</pre>
这个头部是默认添加的。要自定义的话，如下：

```xml
<http>
	<!-- ... -->

	<headers>
		<xss-protection block="false"/>
	</headers>
</http>
```
类似的，使用Java设置XSS保护：

```java
@EnableWebSecurity
public class WebSecurityConfig extends
WebSecurityConfigurerAdapter {

@Override
protected void configure(HttpSecurity http) throws Exception {
	http
	// ...
	.headers()
		.xssProtection()
			.block(false);
}
}
```
<h3>21.1.7 Content Security Policy(CSP)</h3>
<a href="https://www.w3.org/TR/CSP2/">Content Security Policy (CSP，内容安全策略)</a>是web应用用来缓解内容注入<span> 漏洞的一种机制，如cross-site-scripting(XSS，跨站脚本)。CSP是一种声明式策略，它为web应用程序的作者提供了一个工具，用于声明并最终通知客户端（用户代理）web应用程序期望加载哪些资源。</span>

&#x2666;注意：内容安全策略不能解决所有的内容注入漏洞。相反，CSP可以用来减少内容注入攻击造成的伤害。作为第一道防线，web应用程序的作者应该检验用户输入，并对输出进行编码。

web程序可以在响应的中插入下面的其中一个HTTP头部来启用CSP：
<ul>
 	<li>
<p class="cye-lm-tag"><strong><em>Content-Security-Policy</em></strong></p>
</li>
 	<li>
<p class="cye-lm-tag"><strong><em>Content-Security-Policy-Report-Only</em></strong></p>
</li>
</ul>
这些头部都是用来分发<span> </span><strong class="cye-lm-tag"><em class="cye-lm-tag">security policy</em></strong>给客户端的一种机制。一个<strong class="cye-lm-tag"><em class="cye-lm-tag">security policy</em></strong>包含一系列安全策略指令（比如：<em>script-src和<span> </span>object-src</em>），每个指令声明对特定资源的限制。

例如，web程序通过给想用设置下面的响应头来声明，期望从指定的受信任的源加载script脚本：
<pre class="prettyprint">Content-Security-Policy: script-src https://trustedscripts.example.com</pre>
从非<span> </span><em>script-src</em>指令指定的源中加载script脚本会被用户代理阻止。另外，如果在security policy中声明了<span> </span><strong><em><a href="https://www.w3.org/TR/CSP2/#directive-report-uri">report-uri</a></em></strong>指令的话，用户代理会把这种违反安全策略的行为报告给指定的URL。

例如，如果一个web程序违反了声明的安全策略，下面的响应头部会指示用户代理向<span> </span><em>report-uri</em><span> 指定的地址发送违例报告。</span>
<pre class="prettyprint">Content-Security-Policy: script-src https://trustedscripts.example.com; report-uri /csp-report-endpoint/</pre>
<a href="https://www.w3.org/TR/CSP2/#violation-reports"><strong><em>Violation reports</em></strong></a><span> （违例报告）是JSON格式的，即能被web程序自己的API捕获，还能被公开托管的CSP违规报告服务（如 <a href="https://report-uri.io/"><strong><em>REPORT-URI</em></strong></a>）捕获。</span>

<strong><em>Content-Security-Policy-Report-Only</em></strong><span> 头部给web程序的作者和管理员提供了监控安全策略的能力，不强制使用。这个头部通常在试验和/或开发站点的安全策略的时候使用。当确认策略有效时，过使用 <em>Content-Security-Policy</em>头部来代替。</span>

给定下面的响应头，策略声明script只能通过两个可能的源中加载。
<pre class="prettyprint">Content-Security-Policy-Report-Only: script-src 'self' https://trustedscripts.example.com; report-uri /csp-report-endpoint/</pre>
如果站点违反了这个策略，如从evil.com加载script时，用户代理会给<span> </span><em>report-uri</em><span> 指定的地址发送违例报告，然而仍然允许从evil.com加载资源。</span>
<h5 id="headers-csp-configure" class="cye-lm-tag"><strong>Configuring Content Security Policy（配置内容安全策略）</strong></h5>
需要注意的是Spring Security默认是不添加内容安全策略的。web程序的作者必须通过显式申明安全策略的方式来强制和/或监控受保护的资源。

例如，给定下面的安全策略：
<pre class="prettyprint">script-src 'self' https://trustedscripts.example.com; object-src https://trustedplugins.example.com; report-uri /csp-report-endpoint/</pre>
你可以使用XML的<span> </span><a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#nsa-content-security-policy">&lt;content-security-policy</a><span>&gt;元素来</span>配置CSP头部：
```xml
<http>
	<!-- ... -->

	<headers>
		<content-security-policy
			policy-directives="script-src 'self' https://trustedscripts.example.com; object-src https://trustedplugins.example.com; report-uri /csp-report-endpoint/" />
	</headers>
</http>
```
使用下面的配置来启用CSP “report-only” 头部：
```xml
<http>
	<!-- ... -->

	<headers>
		<content-security-policy
			policy-directives="script-src 'self' https://trustedscripts.example.com; object-src https://trustedplugins.example.com; report-uri /csp-report-endpoint/"
			report-only="true" />
	</headers>
</http>
```
类似地，你可以使用Java来配置CSP头部：
```java
@EnableWebSecurity
public class WebSecurityConfig extends
WebSecurityConfigurerAdapter {

@Override
protected void configure(HttpSecurity http) throws Exception {
	http
	// ...
	.headers()
		.contentSecurityPolicy("script-src 'self' https://trustedscripts.example.com; object-src https://trustedplugins.example.com; report-uri /csp-report-endpoint/");
}
}
```
使用下面配置启用CSP “report-only”头部：
```java
@EnableWebSecurity
public class WebSecurityConfig extends
WebSecurityConfigurerAdapter {

@Override
protected void configure(HttpSecurity http) throws Exception {
	http
	// ...
	.headers()
		.contentSecurityPolicy("script-src 'self' https://trustedscripts.example.com; object-src https://trustedplugins.example.com; report-uri /csp-report-endpoint/")
		.reportOnly();
}
}
```
<h5 id="headers-csp-links" class="cye-lm-tag"><strong>Additional Resources（其他资源）</strong></h5>
<span>把内容安全策略应用于web应用程序并不简单。下面的资源提供了更多关于开发安全策略的内容。</span>
<div class="paragraph">
<p class="cye-lm-tag"><a href="https://www.html5rocks.com/en/tutorials/security/content-security-policy/">An Introduction to Content Security Policy</a></p>

</div>
<div class="paragraph cye-lm-tag">
<p class="cye-lm-tag"><a href="https://developer.mozilla.org/en-US/docs/Web/Security/CSP">CSP Guide - Mozilla Developer Network</a></p>

</div>
<div class="paragraph">
<p class="cye-lm-tag"><a href="https://www.w3.org/TR/CSP2/">W3C Candidate Recommendation</a></p>

<h3>21.1.8 Referrer Policy</h3>
<a href="https://www.w3.org/TR/referrer-policy">Referrer Policy</a><span> 是web应用用来管理引用字段的一种机制，它保存了用户之前访问的页面。</span>

Spring Security使用<span> </span><a href="https://www.w3.org/TR/referrer-policy/">Referrer Policy</a>头部来提供不同的策略：
<pre class="prettyprint">Referrer-Policy: same-origin</pre>
<span> Referrer-Policy响应头指示浏览器让目的地知道用户之前的来源。</span>
<h5 id="headers-referrer-configure"><strong>Configuring Referrer Policy（配置引用策略）</strong></h5>
Spring Security默认是不添加引用策略的。

你可以使用XML的<span> </span><a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#nsa-referrer-policy">&lt;referrer-policy</a><span>&gt;元素来配置：</span>
```xml
<http>
	<!-- ... -->

	<headers>
		<referrer-policy policy="same-origin" />
	</headers>
</http>
```
类似地，使用如下的Java代码来配置引用策略：
```java
@EnableWebSecurity
public class WebSecurityConfig extends
WebSecurityConfigurerAdapter {

@Override
protected void configure(HttpSecurity http) throws Exception {
	http
	// ...
	.headers()
		.referrerPolicy(ReferrerPolicy.SAME_ORIGIN);
}
}
```
<h2>21.1 Custom Headers自定义头部</h2>
Spring Security提供了方便的用于添加其他常见安全头部的机制。但是需要定义钩子来启用自定义头部。
<h3>21.2.1 Static Headers</h3>
<span>您可能希望将自定义的安全头注入到应用程序。例如下面自定义的安全头部：</span>
<pre class="prettyprint">X-Custom-Security-Header: header-value</pre>
当使用XML配置时，可以使用<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#nsa-header">&lt;header&gt;</a><span>元素把自定义头部添加到响应中：</span>

```xml
<http>
	<!-- ... -->

	<headers>
		<header name="X-Custom-Security-Header" value="header-value"/>
	</headers>
</http>
```
类似地，使用Java进行配置：
```java
@EnableWebSecurity
public class WebSecurityConfig extends
WebSecurityConfigurerAdapter {

@Override
protected void configure(HttpSecurity http) throws Exception {
	http
	// ...
	.headers()
		.addHeaderWriter(new StaticHeadersWriter("X-Custom-Security-Header","header-value"));
}
}
```
<h3>21.2.2 Headers Writer</h3>
当XML命名空间配置或Java配置不支持你需要的头部时，你可以自定义一个<code>HeadersWriter</code><span> 实例或者提供一个<code>HeadersWriter</code> 的实现。</span>

下面来看一下使用<code>XFrameOptionsHeaderWriter</code>的一个例子。比如，你想允许frame加载同一域名下的内容，可以通过设置<span> </span><a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#nsa-frame-options-policy">policy</a> 属性为“<span>SAMEORIGIN</span>”来解决，但是这里我们来看一下另一种更具体的解决方法：使用<span> </span><a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#nsa-header-ref">ref</a>属性：
```xml
<http>
	<!-- ... -->

	<headers>
		<header ref="frameOptionsWriter"/>
	</headers>
</http>
<!-- Requires the c-namespace.
See http://docs.spring.io/spring/docs/current/spring-framework-reference/htmlsingle/#beans-c-namespace
-->
<beans:bean id="frameOptionsWriter"
	class="org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter"
	c:frameOptionsMode="SAMEORIGIN"/>
```
相同的Java配置如下：
```java
@EnableWebSecurity
public class WebSecurityConfig extends
WebSecurityConfigurerAdapter {

@Override
protected void configure(HttpSecurity http) throws Exception {
	http
	// ...
	.headers()
		.addHeaderWriter(new XFrameOptionsHeaderWriter(XFrameOptionsMode.SAMEORIGIN));
}
}
```
<h3>21.2.3 DelegatingRequestMatcherHeaderWriter</h3>
如果你只想给特定的请求添加头部，例如，你可能只想保护登陆页面不被frame加载，这时你可以使用<span> </span><code>DelegatingRequestMatcherHeaderWriter</code>来实现。使用XML配置如下：

```xml
<http>
	<!-- ... -->

	<headers>
		<frame-options disabled="true"/>
		<header ref="headerWriter"/>
	</headers>
</http>

<beans:bean id="headerWriter"
	class="org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter">
	<beans:constructor-arg>
		<bean class="org.springframework.security.web.util.matcher.AntPathRequestMatcher"
			c:pattern="/login"/>
	</beans:constructor-arg>
	<beans:constructor-arg>
		<beans:bean
			class="org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter"/>
	</beans:constructor-arg>
</beans:bean>
```

相同地，使用Java配置来阻止frame加载登陆页：

```java
@EnableWebSecurity
public class WebSecurityConfig extends
WebSecurityConfigurerAdapter {

@Override
protected void configure(HttpSecurity http) throws Exception {
	RequestMatcher matcher = new AntPathRequestMatcher("/login");
	DelegatingRequestMatcherHeaderWriter headerWriter =
		new DelegatingRequestMatcherHeaderWriter(matcher,new XFrameOptionsHeaderWriter());
	http
	// ...
	.headers()
		.frameOptions().disabled()
		.addHeaderWriter(headerWriter);
}
}
```

<h1>22 Session Management</h1>
HTTP session相关的功能是由<code>SessionManagementFilter</code><span>和<code>SessionAuthenticationStrategy</code>接口的组合处理的，其中<code>SessionManagementFilter</code>委托给了这个组合。典型的用法包括会话固定攻击预防、会话超时检测，以及限制同一用户并发打开session的数量。</span>
<h2>22.1 SessionManagementFilter会话管理过滤器</h2>
<span> </span><code>SessionManagementFilter</code>会把<code class="cye-lm-tag">SecurityContextRepository</code>的内容和当前<code class="cye-lm-tag">SecurityContextHolder</code>的内容进行对照，以此来决定当前请求的用户是否已经经过身份验证，通常使用非交互式的身份验证机制，如<span>pre-authentication或remember-me<sup class="footnote">[<a id="_footnoteref_17" class="footnote" href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#_footnote_17" title="View footnote.">17</a>]</sup>。如果<code class="cye-lm-tag">SecurityContextRepository</code>包含了安全上下文<code class="cye-lm-tag">SecurityContext</code>，那么<code>SessionManagementFilter</code>就什么都不做。如果<code class="cye-lm-tag">SecurityContextRepository</code>不包含安全上下文，并且线程本地的<code class="cye-lm-tag">SecurityContext</code>对象包含（非匿名）<code>Authentication</code>对象，那么<code>SessionManagementFilter</code>就会认为用户已经通过了过滤器栈上的其他过滤器的验证。然后<code class="cye-lm-tag">SecurityContextRepository</code>会调用已经配置的<code>SessionAuthenticationStrategy</code>。</span>

如果用户还没有通过身份验证，<span><code>SessionManagementFilter</code>会检查请求是否是无效的session ID（如会话超时），此外如果配置了<code>InvalidSessionStrategy</code>，那么会接着调用这个策略。针对这种情况，最常用的处理方式是重定向到某个URL，<code>SimpleRedirectInvalidSessionStrategy</code>已经封装好了这些功能。<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#ns-session-mgmt">正如前面所描述的</a>，在通过命名空间配置invalid session URL （无效会话URL）时也使用了<code>SimpleRedirectInvalidSessionStrategy</code>。</span>
<h2>22.2 SessionAuthenticationStrategy会话验证策略</h2>
<code class="cye-lm-tag">SessionManagementFilter</code><span> 和</span><code>AbstractAuthenticationProcessingFilter</code>都使用了<code>SessionAuthenticationStrategy</code>，所以，假如你用的是自定义的form-login类，那么你需要把<code>SessionAuthenticationStrategy</code>注入到<code class="cye-lm-tag">SessionManagementFilter</code>和<code>AbstractAuthenticationProcessingFilter</code>里。以这个例子为例，需要的XML配置如下：

```xml
<http>
<custom-filter position="FORM_LOGIN_FILTER" ref="myAuthFilter" />
<session-management session-authentication-strategy-ref="sas"/>
</http>
<beans:bean id="myAuthFilter" class=
"org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter">
<beans:property name="sessionAuthenticationStrategy" ref="sas" />
...
</beans:bean>


<beans:bean id="sas" class=
"org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy" />
```

<span>请注意，如果您将bean存储在实现了<code>HttpSessionBindingListener</code>的session中，包括Spring session-scoped bean(会话作用域的bean)，那么使用默认的SessionFixationProtectionStrategy可能会导致一些问题。详细信息请参考与本节相关的Javadoc。</span>
<h2>22.3 Concurrency Control并发控制</h2>
Spring Security可以<span>不指定验证次数，就可以</span><span>防止一个主体并发地对同一个应用程序进行身份验证。许多ISVs（独立软件商）利用这一点来强制授权，然而一些网络管理员则喜欢这种特性，因为它可以有效防止人们共享登录名。例如，你可以阻止用户“Batman”从不同的会话登陆web应用。你也可以终止用户之前的登陆，或者当用户试图再次登录时，你可以报告一个错误，防止第二次登录。注意，如果您使用的是第二种方法，那么没有显式注销的用户（例如，刚刚关闭了他们的浏览器）的用户将无法再次登录，除非他们的初始会话过期为止。</span>

命名空间支持并发控制，所以相关配置方法，<span>请回顾前面的命名空间章节。但是有时候你还是要自定义一些东西。</span>

并发控制实现了一个特殊的<code>SessionAuthenticationStrategy</code>，叫<code>ConcurrentSessionControlAuthenticationStrategy</code>。

&#x2663;注意：
<p class="target">以前，并发身份验证检查是由ProviderManager进行的，ProviderManager中可以注入一个ConcurrentSessionController。ConcurrentSessionController会检查用户访问的会话数量是否超过允许的值。然而这种并发身份的验证机制需要提前创建好session，这显然是没有必要的。在Spring Security 3中，用户首先由<code class="cye-lm-tag">AuthenticationManager</code>里验证身份，一旦验证成功，就会创建一个session，并检查是否允许再开启另一次session。</p>
如果要使用concurrent session并发会话支持，你需要在web.xml里做如下配置：

```xml
<listener>
	<listener-class>
	org.springframework.security.web.session.HttpSessionEventPublisher
	</listener-class>
</listener>
```

另外，你还需要给<code>FilterChainProxy</code>添加<code>ConcurrentSessionFilter</code>。<code>ConcurrentSessionFilter</code>需要两个构造参数：<code>sessionRegistry</code>和<code class="cye-lm-tag">sessionInformationExpiredStrategy</code>，前者通常指<code>SessionRegistryImpl</code>的实例，后者用来定义会话超时的应对策略。使用命名空间创建<code>FilterChainProxy</code>的配置，以及一些默认的beans的配置如下：

```xml
<http>
<custom-filter position="CONCURRENT_SESSION_FILTER" ref="concurrencyFilter" />
<custom-filter position="FORM_LOGIN_FILTER" ref="myAuthFilter" />

<session-management session-authentication-strategy-ref="sas"/>
</http>

<beans:bean id="redirectSessionInformationExpiredStrategy"
class="org.springframework.security.web.session.SimpleRedirectSessionInformationExpiredStrategy">
<beans:constructor-arg name="invalidSessionUrl" value="/session-expired.htm" />
</beans:bean>

<beans:bean id="concurrencyFilter"
class="org.springframework.security.web.session.ConcurrentSessionFilter">
<beans:constructor-arg name="sessionRegistry" ref="sessionRegistry" />
<beans:constructor-arg name="sessionInformationExpiredStrategy" ref="redirectSessionInformationExpiredStrategy" />
</beans:bean>

<beans:bean id="myAuthFilter" class=
"org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter">
<beans:property name="sessionAuthenticationStrategy" ref="sas" />
<beans:property name="authenticationManager" ref="authenticationManager" />
</beans:bean>

<beans:bean id="sas" class="org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy">
<beans:constructor-arg>
	<beans:list>
	<beans:bean class="org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy">
		<beans:constructor-arg ref="sessionRegistry"/>
		<beans:property name="maximumSessions" value="1" />
		<beans:property name="exceptionIfMaximumExceeded" value="true" />
	</beans:bean>
	<beans:bean class="org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy">
	</beans:bean>
	<beans:bean class="org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy">
		<beans:constructor-arg ref="sessionRegistry"/>
	</beans:bean>
	</beans:list>
</beans:constructor-arg>
</beans:bean>

<beans:bean id="sessionRegistry"
	class="org.springframework.security.core.session.SessionRegistryImpl" />
```

<span>将HttpSessionEventPublisher侦听器添加到web.xml。每当<code class="cye-lm-tag">HttpSession</code>开始或终止时，一个<code>ApplicationEvent</code>就会被发布到Spring <code>ApplicationContext</code>中。这是至关重要的，因为它允许在会话结束时通知<code class="cye-lm-tag">SessionRegistryImpl</code>。如果没有设置，一旦用户超过了会话限制，那么用户将无法再次登录，即使用户退出了另一个会话或会话超时也无济于事。</span>
<h3>22.3.1 Querying the SessionRegistry for currently authenticated users and sessions 从SessionRegistry中查询当前已验证的用户和他们的sessions</h3>
<span>通过命名空间或使用普通bean来设置并发控制的一个好处是，可以提供一个对SessionRegistry的引用，你可以直接在应用程序中使用。即使你不想限制用户可以拥有的session数量，设置并发控制还是非常有用的。你可以把<code>maximumSession</code>参数设置为-1来允许任意数量的会话。如果你用的是命名空间配置，你可以使用<code>session-registry-alias</code>属性给内部创建的<code>SessionRegistry</code>对象设置一个别名，也就是提供一个引用，可以用来注入到自己的bean里。</span>

<code>getAllPrincipals()</code>方法可以列出当前已经通过验证的用户列表。你可以使用<code class="cye-lm-tag">getAllSessions(Object principal, boolean includeExpiredSessions)</code>方法里得到用户的会话，该方法返回一个<code>SessionInformation</code>的列表。你也可以直接调用<code>SessionInformation</code>对象的<code>expireNow()</code>方法来立即使用户的会话过期。当用户返回到应用时，就会被阻止继续操作。你会发现在管理平台上这些方法是非常有用的。更多信息请查看Javadoc。
<h1>23 匿名身份验证</h1>
<h2>23.1 简介</h2>
通常情况下，使用“deny-by-default”默认拒绝的安全设置是比较好的，<span>这种情况下，您可以显式地指定什么是允许的，什么是不允许的。定义未通过身份验证的用户可以浏览什么资源是一种比较常见的情况，尤其是在web应用中。许多网站要求用户必须通过身份验证，除了有限的那么几个URL（比如主页和登录页）。这种情况只需要简单地给特殊的URL定义访问配置属性就行了，不需要给每一个安全资源都进行设置。换句话说，在默认情况下，<code class="cye-lm-tag">ROLE_SOMETHING</code>是必需的，但它只允许某些例外情况，比如登录、注销和应用程序的主页。你可以直接从过滤链中忽略这些页面，也就是绕过访问控制检查，但是某些情况下，尤其是页面对验证用户有不同的表现时这时不可取的。</span>

这就是为什么要使用匿名身份验证的原因。需要注意，对于匿名验证用户和未验证用户来说，他们在概念上没有什么区别。Spring Security的匿名身份验证只是提供了一个配置<span>access-control </span>访问控制属性的便捷方法。调用servlet的接口，如<code>getCallerPrincipal</code>，依然返回null值，即使<code>SecurityContextHolder</code>中存在匿名验证对象<span>authentication。</span>

匿名验证还有一些有用的场景，比如<span>auditing interceptor</span><span>审计拦截器查询安全上下文<code>SecurityContextHolder</code>，以确定哪个主体对给定的操作负责。还有一个作用是，使用匿名验证<code>SecurityContextHolder</code>中就不会有null的<code>Authentication</code>对象，这样程序就可以编写的更加健壮。</span>
<h2>23.2 配置</h2>
使用Spring Security 3的HTTP配置时，会自动提供匿名验证支持，也可以使用<code class="cye-lm-tag">&lt;anonymous&gt;</code>元素来进行自定义。<span>除非您使用的是传统的bean配置，否则您不需要配置这里描述的bean。</span>

有<span>三个类一起提供了匿名认证功能。<code class="cye-lm-tag">AnonymousAuthenticationToken</code>是<code>Authentication</code>的实现，它保存了匿名主体的一系列<code class="cye-lm-tag">GrantedAuthority</code>（授权权利）。有一个对应的<code class="cye-lm-tag">AnonymousAuthenticationProvider</code>匿名身份验证提供者，它被链接到<code>ProviderManager</code>中，这样就可以接受<code class="cye-lm-tag">AnonymousAuthenticationToken</code>。最后，还有一个<code class="cye-lm-tag">AnonymousAuthenticationFilter</code>匿名的身份验证过滤器，它被链接在正常的身份验证机制之后，如果没有现有的<code>Authentication</code>对象存在，就会自动将一个<code>AnonymousAuthenticationToken</code>添加到安全上下文<code>SecurityContextHolder</code>。过滤器和身份验证提供者的定义如下：</span>

```xml
<bean id="anonymousAuthFilter"
	class="org.springframework.security.web.authentication.AnonymousAuthenticationFilter">
<property name="key" value="foobar"/>
<property name="userAttribute" value="anonymousUser,ROLE_ANONYMOUS"/>
</bean>

<bean id="anonymousAuthenticationProvider"
	class="org.springframework.security.authentication.AnonymousAuthenticationProvider">
<property name="key" value="foobar"/>
</bean>
```

<span>密钥<code class="cye-lm-tag">key</code>在过滤器和认证提供者之间共享，这样由过滤器中创建的令牌就可以被认证提供者所接受 <sup class="footnote">[<a id="_footnoteref_18" class="footnote" href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#_footnote_18" title="View footnote.">18</a>]</sup>。<code>userAttribute</code>属性的表示形式为<code class="cye-lm-tag">usernameInTheAuthenticationToken,grantedAuthority[,grantedAuthority]</code>。这与<code class="cye-lm-tag">InMemoryDaoImpl</code>的<code class="cye-lm-tag">userMap</code>属性的等号后面使用的语法是一样的。</span>

就像之前解释的那样，匿名身份验证的好处是所有的URI都可以被保护起来，例如：

```xml
<bean id="filterSecurityInterceptor"
	class="org.springframework.security.web.access.intercept.FilterSecurityInterceptor">
<property name="authenticationManager" ref="authenticationManager"/>
<property name="accessDecisionManager" ref="httpRequestAccessDecisionManager"/>
<property name="securityMetadata">
	<security:filter-security-metadata-source>
	<security:intercept-url pattern='/index.jsp' access='ROLE_ANONYMOUS,ROLE_USER'/>
	<security:intercept-url pattern='/hello.htm' access='ROLE_ANONYMOUS,ROLE_USER'/>
	<security:intercept-url pattern='/logoff.jsp' access='ROLE_ANONYMOUS,ROLE_USER'/>
	<security:intercept-url pattern='/login.jsp' access='ROLE_ANONYMOUS,ROLE_USER'/>
	<security:intercept-url pattern='/**' access='ROLE_USER'/>
	</security:filter-security-metadata-source>" +
</property>
</bean>
```

<h2>23.3 AuthenticationTrustResolver验证信任解析器</h2>
<span>匿名身份验证讨论的另一个问题是<code>AuthenticationTrustResolver</code>接口，以及与它相关的<code class="cye-lm-tag">AuthenticationTrustResolverImpl</code>接口实现。这个接口提供了一个<code class="cye-lm-tag">isAnonymous(Authentication)</code>方法，可以让使用它的类获取这种特殊类型的认证状态。<code>ExceptionTranslationFilter</code>在处理<code>AccessDeniedException</code>异常时就使用了这个接口。如果抛出了<code>AccessDeniedException</code>异常，并且authentication对象是匿名类型的，那么过滤器就会启用<code>AuthenticationEntryPoint</code>来验证主体身份，而不是返回一个403（拒绝）的响应。这是区别是非常必要的，否则主体总是被认为是“经过身份验证的”，并且永远不会有机会通过表单、基本、摘要或其他常规身份验证机制登陆。</span>

您将经常看到上面的拦截器配置中的<code>ROLE_ANONYMOUS</code> 属性被<code>IS_AUTHENTICATED_ANONYMOUSLY</code>替代，这在定义访问控制时实际上是一样的。这儿有一个使用<code>AuthenticatedVoter</code>的例子，可以在<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#authz-authenticated-voter">authorization chapter</a>一章中看到。其中<span><code>AuthenticatedVoter</code>使用了一个<code class="cye-lm-tag">AuthenticationTrustResolver</code> 来处理这个特殊的配置属性（<code>IS_AUTHENTICATED_ANONYMOUSLY</code>），并授予对匿名用户的访问权限。由于<code class="cye-lm-tag">AuthenticatedVoter</code>可以区分anonymous、remember-me和fully-authenticated的用户，所以它的功能更加强大。如果你不需要这个功能，那你就坚持使用<code class="cye-lm-tag">ROLE_ANONYMOUS</code>配置，它可以被标准的Spring Security <code>RoleVoter</code>处理。</span>
<h1>24 WebSocket Security web套接字安全</h1>
Spring Security 4添加了对<a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#websocket">Spring的WebSocket</a>的安全支持。这一章描述了Spring Security对WebSocket的支持。

&#x2663; 注意：你可以在<span>samples/javaconfig/chat（这个估计是Spring Security的github项目目录里的）中找到完整的WebSocket安全支持的例子。</span>
<table style="border: 1px solid black;">
<tbody>
<tr>
<td>Direct JSR-356 支持

<span>Spring Security没有提供直接的JSR-356支持，因为这样做不会带来什么价值。</span><span>这是因为格式是未知的，所以<a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#websocket">对于未知的格式Spring Security就做不了什么了</a>。另外JSR-356没有提供拦截消息的方法，所以安全是相当具有侵略性的。</span></td>
</tr>
</tbody>
</table>
<h2>24.1 WebSocket配置</h2>
Spring Security通过使用Spring Messaging抽象，引入了对WebSocket的授权支持。使用Java来配置授权，需要继承<code class="cye-lm-tag">AbstractSecurityWebSocketMessageBrokerConfigurer</code>并配置<code>MessageSecurityMetadataSourceRegistry</code>。例如：

```java
@Configuration
public class WebSocketSecurityConfig
      extends AbstractSecurityWebSocketMessageBrokerConfigurer {  1  2

    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
                .simpDestMatchers("/user/*").authenticated()   3
    }
}
```

以上配置可以保证：
<ol>
 	<li><span>任何入站的CONNECT 消息都需要有效的CSRF令牌来执行<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#websocket-sameorigin">同源策略</a></span></li>
 	<li>对于任何入站请求，simpUser头部属性中的user用户都会被填充到<span>SecurityContextHolder中</span></li>
 	<li>消息需要合适的授权。特别是任何入站的，以"/user/"打头的消息都需要<span>ROLE_USER权限。更多授权相关的内容请看 <a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#websocket-authorization">WebSocket Authorization</a></span></li>
</ol>
Spring Security也提供了XML的配置方法。与上面类似的XML配置如下：

```xml
<websocket-message-broker>  1 2
    3
    <intercept-message pattern="/user/**" access="hasRole('USER')" />
</websocket-message-broker>
```

这个XML配置和上面的Java配置实现的功能是一样的。
<h2>24.2 WebSocket Authentication websocket身份验证</h2>
WebSocket重用了建立WebSocket连接时，在HTTP请求中的身份验证信息。<span>这意味着HttpServletRequest的主体将被传递给WebSockets。</span><span>如果您使用的是Spring Security，那么HttpServletRequest的主体将被自动覆盖。</span>

<span>更具体地说，为了确保WebSocket应用程序对用户进行了身份验证，您只需要使用Spring Security来验证基于HTTP的web应用程序即可。</span>
<h2>24.3 WebSocket Authorization  webSocket授权</h2>
<span>Spring Security 4.0通过Spring消息传递抽象引入了对WebSockets的授权支持。要使用Java配置授权，只需要继承<code class="cye-lm-tag">AbstractSecurityWebSocketMessageBrokerConfigurer</code>，并配<code>MessageSecurityMetadataSourceRegistry</code>。例如：</span>

```java
@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
                .nullDestMatcher().authenticated()   1
                .simpSubscribeDestMatchers("/user/queue/errors").permitAll()   2
                .simpDestMatchers("/app/**").hasRole("USER")   3
                .simpSubscribeDestMatchers("/user/**", "/topic/friends/*").hasRole("USER")   4
                .simpTypeMatchers(MESSAGE, SUBSCRIBE).denyAll()   5
                .anyMessage().denyAll();   6

    }
}
```

上面的配置会保证：
<ol>
 	<li><span>任何没有目的地的消息（也就是MESSAGE消息或SUBSCRIBE订阅的消息类型以外的任何消息）都将要求用户进行身份验证。</span></li>
 	<li><span>任何人都可以订阅 /user/queue/errors</span></li>
 	<li><span>任何有目的地的以“/app”开始的消息都要求用户拥有ROLE_USER角色</span></li>
 	<li>任何以“/user”或者“/topic/friends/”开始的<span>SUBSCRIBE类型的消息都要求用户有ROLE_USER角色</span></li>
 	<li>其他<span> MESSAGE和SUBSCRIBE类型的消息都会被拒绝。由于使用了第6步，我们可以不需要这一步，但是这一步演示了如何匹配特殊消息类型的方法</span></li>
 	<li>其他任何消息都会被拒绝。这可以保证你不会遗漏任何消息。</li>
</ol>
Spring Security同样提供了XML配置支持。相同的配置如下：

```xml
<websocket-message-broker>
    1
    <intercept-message type="CONNECT" access="permitAll" />
    <intercept-message type="UNSUBSCRIBE" access="permitAll" />
    <intercept-message type="DISCONNECT" access="permitAll" />

    <intercept-message pattern="/user/queue/errors" type="SUBSCRIBE" access="permitAll" />  2
    <intercept-message pattern="/app/**" access="hasRole('USER')" />       3

    4
    <intercept-message pattern="/user/**" access="hasRole('USER')" />
    <intercept-message pattern="/topic/friends/*" access="hasRole('USER')" />

    5
    <intercept-message type="MESSAGE" access="denyAll" />
    <intercept-message type="SUBSCRIBE" access="denyAll" />

    <intercept-message pattern="/**" access="denyAll" />  6
</websocket-message-broker>
```

上面的配置和Java配置实现的功能是一样的。
<h3>24.3.1 WebSocket Authorization Notes webSocket授权注意事项</h3>
为了很好地保护程序，理解Spring的WebSocket支持是非常重要的。
<h5 id="websocket-authorization-notes-messagetypes" class="cye-lm-tag">WebSocket Authorization on Message Types <span>对消息类型的WebSocket授权</span></h5>
理解<span>SUBSCRIBE和MESSAGE这两种消息类型的区别，知道他们在Spring里如何运行是非常重要的。</span>

例如一个聊天程序：
<ul>
 	<li>系统可以使用目的地“topic/system/notifications”给所有用户发送<span>MESSAGE类型的通知</span></li>
 	<li>客户端通过<span>SUBSCRIBE订阅“/topic/system/notifications”来接受通知</span></li>
</ul>
当我们想让客户端可以<span>SUBSCRIBE</span>订阅"<span>/topic/system/notifications</span>"时，我们并不想让客户端发送消息<span>MESSAGE到这个地址。如果我们允许发送消息到"/topic/system/notifications"，那么客户端就可以直接发送消息到这个地址，以此来冒充系统。</span>

所以通常情况下，程序是禁止任何MESSAGE类型的消息发送到以<span> </span><a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/html/websocket.html#websocket-stomp">broker prefix</a><span> （如"/topic/" 或 "/queue/"）开始的地址。</span>
<h5 id="websocket-authorization-notes-destinations" class="cye-lm-tag">WebSocket Authorization on Destinations 对目的地的webSocket授权</h5>
<span>了解目的地是如何转换的也是很重要的。</span>

以聊天软件为例：
<ul>
 	<li><span>用户可以通过发送消息到“/app/chat”目的地，给特定的用户发送信息。</span></li>
 	<li><span>应用程序看到消息后，会确保“from”属性被指定为当前用户（我们不能信任客户端）。</span></li>
 	<li>然后程序使用<code>SimpMessageSendingOperations.convertAndSendToUser("toUser", "/queue/messages", message)</code>方法给接收者发送信息。</li>
 	<li>最终消息被传递到<span>"/queue/user/messages-&lt;sessionid&gt;"目的地</span></li>
</ul>
上面的程序中，我们希望允许客户端监听“/user/queue”，“/user/queue”可以被转换为<span>"/queue/user/messages-&lt;sessionid&gt;"。然而我们不希望客户端监听"/queue/*"，因为这样的话客户端就可以看到所有用户的消息。</span>

通常情况下，程序不允许任何<span>SUBSCRIBE发送到以 <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/html/websocket.html#websocket-stomp">broker prefix</a> （如"/topic/" or "/queue/"）开始的地址。当然，我们可以为诸如此类的事情提供例外。</span>
<h3>24.3.2 Outbound Messages出站消息</h3>
Spring有一节叫<span> </span><a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/html/websocket.html#websocket-stomp-message-flow">Flow of Messages</a><span> ，描述了消息在系统内部是如何流动的。需要注意，Spring Security只保护了<code class="cye-lm-tag">clientInboundChannel</code>。Spring Security不打算保护 <code>clientOutboundChannel</code>。</span>

最重要的原因是为了性能。<span>每进入一条信息，通常会有更多的信息流出。所以我们建议保护对端点的订阅，而不是出站的消息。</span>
<h2>24.4 Enforcing Same Origin Policy强制<span>执行同源策略</span></h2>
需要强调的是浏览器不会对WebSocket链接强制执行同源策略<span> </span><a href="https://en.wikipedia.org/wiki/Same-origin_policy">Same Origin Policy</a><span> 。这时极其重要的一点。</span>
<h3>24.4.1 为什么需要同源？</h3>
考虑一下这样的场景。用户访问bank.com并验证他的账户。然后又在浏览器的另一个tab页打开了一个evil.com的网站。同源策略可以保证evil.com不能读写bank.com的数据。

<span>对于WebSockets，同源策略并不适用。事实上，除非bank.com明确禁止它，否则邪恶的网站可以代表用户读写数据。这意味着用户可以在webSocket上做任何事情（比如转账），evil.com也可以代表用户做同样的事情。</span>

<span>由于SockJS试图模仿WebSockets，所以它也绕过了同源策略。这就意味着开发人员需要在使用SockJS时显式地保护他们的应用程序不受外部域的影响。</span>
<h3>24.4.2 Spring WebSocket Allowed Origin</h3>
幸运的是，从Spring 4.1.5的Spring WebSocket开始，以及SockJS的支持，可以限制访问当前域<span> </span><a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/html/websocket.html#websocket-server-allowed-origins">current domain</a>。<span>Spring Security添加了额外的保护层，以提供深度防御 <a href="https://en.wikipedia.org/wiki/Defense_in_depth_%28computing%29">defence in depth</a>。</span>
<h3>24.4.3 Adding CSRF to Stomp Headers</h3>
默认情况下，Spring Security要求所有CONNECT消息类型都要有<span> </span><a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#csrf">CSRF token</a>。这可以保证只有能<span>够访问CSRF令牌的站点才可以连接。由于只有同源才可以访问CSRF令牌，所以外部的域就不能建立连接了。</span>

<span>通常，我们需要将CSRF令牌包含在HTTP头或HTTP参数中。然而，SockJS不允许这些选项。相反，我们必须在Stomp头部中包含令牌。</span>

<span>应用程序可以通过访问名为_csrf的请求属性<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#csrf-include-csrf-token">获得CSRF令牌</a>。例如使用下面的代码可以在JSP中访问<code>CsrfToken</code>。</span>

```javascript
var headerName = "${_csrf.headerName}";
var token = "${_csrf.token}";
```

如果你用的是静态HTML，你可以把<span><code>CsrfToken</code>开放为一个REST接口。例如下面的代码可以把<code>CsrfToken</code>暴露给 /csrf地址：</span>

```java
@RestController
public class CsrfController {

    @RequestMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }
}
```

可以使用JavaScript代码请求这个REST接口，然后使用响应来生成<span>headerName和token。</span>

然后再我们的Stomp客户端添加token，例如：

```javascript
...
var headers = {};
headers[headerName] = token;
stompClient.connect(headers, function(frame) {
  ...

}
```

<h3>24.4.4 Disable CSRF within WebSockets</h3>
如果你想让其他的域访问你的站点，你可以禁用Spring Security的保护。例如，你可以使用下面的Java配置：

```java
@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    ...

    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }
}
```

<h2>24.5 Working with SockJS</h2>
<a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/html/websocket.html#websocket-fallback">SockJS</a><span> 为旧的浏览器提供了后向兼容。当使用后向兼容的选项时，我们需要放松一些安全约束，来使SockJS和Spring Security以前工作。</span>
<h3>24.5.1 SockJS和frame-options</h3>
<span>SockJS可以<a href="https://www.google.co.jp/search?q=SockJS+may+use+an+transport+that+leverages+an+iframe.&amp;oq=SockJS+may+use+an+transport+that+leverages+an+iframe.&amp;aqs=chrome..69i57&amp;sourceid=chrome&amp;ie=UTF-8">利用iframe来进行传输</a>。默认情况下Spring Security会拒绝被framed包裹的站点请求，以此来防止“点击劫持”攻击Clickjacking attacks。为了使基于frame来传输SockJS工作，我们需要配置Spring Security让它允许同源的frame可以包裹内容。</span>

你可以使用<span> </span><a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#nsa-frame-options">frame-options</a>元素来自定义<span>X-Frame-Options。例如，下面的配置会指示Spring Security使用"X-Frame-Options: SAMEORIGIN" 的设置，允许在同一域名下使用iframes。</span>

```xml
<http>
    <!-- ... -->

    <headers>
        <frame-options
          policy="SAMEORIGIN" />
    </headers>
</http>
```

同样，使用Java来做相同的配置：

```java
@EnableWebSecurity
public class WebSecurityConfig extends
   WebSecurityConfigurerAdapter {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http
      // ...
      .headers()
        .frameOptions()
            .sameOrigin();
  }
}
```

<h3>24.5.2 SockJS和Relaxing CSRF</h3>
<span>任何基于HTTP的传输中，SockJS在CONNECT 消息时的是POST。通常，我们需要将CSRF令牌包含在HTTP头或HTTP参数中。然而，SockJS不允许这些选项。所以我们必须在Stomp头部中包含令牌，如 <a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#websocket-sameorigin-csrf" class="cye-lm-tag">Adding CSRF to Stomp Headers</a>.中描述的那样。</span>

<span>这也意味着我们需要用web层来放松CSRF保护。具体来说，我们需要给connect URL禁用CSRF保护，而不是给所有的URL都禁用CSRF保护。否则我们的站点就很容易受到CSRF攻击。</span>

使用CSRF RequestMatcher可以很容易地做到这点。使用Java配置相当简单。例如，如果我们的stomp地址是“/chat”，我们可以只禁用针对“/chat/”开头的URL的CSRF保护，配置如下：

```java
@Configuration
@EnableWebSecurity
public class WebSecurityConfig
    extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http
            .csrf()
                // ignore our stomp endpoints since they are protected using Stomp headers
                .ignoringAntMatchers("/chat/**")
                .and()
            .headers()
                // allow same origin to frame our site to support iframe SockJS
                .frameOptions().sameOrigin()
                .and()
            .authorizeRequests()

            ...
```

如果使用XML配置，我们可以使用<span> </span><a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#nsa-csrf-request-matcher-ref">csrf@request-matcher-ref</a>：

```xml
<http ...>
    <csrf request-matcher-ref="csrfMatcher"/>

    <headers>
        <frame-options policy="SAMEORIGIN"/>
    </headers>

    ...
</http>

<b:bean id="csrfMatcher"
    class="AndRequestMatcher">
    <b:constructor-arg value="#{T(org.springframework.security.web.csrf.CsrfFilter).DEFAULT_CSRF_MATCHER}"/>
    <b:constructor-arg>
        <b:bean class="org.springframework.security.web.util.matcher.NegatedRequestMatcher">
          <b:bean class="org.springframework.security.web.util.matcher.AntPathRequestMatcher">
            <b:constructor-arg value="/chat/**"/>
          </b:bean>
        </b:bean>
    </b:constructor-arg>
</b:bean>
```
</div>