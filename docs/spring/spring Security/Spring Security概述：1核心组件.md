<h3>1 核心组件</h3>
从Spring Security 3.0开始，spring-security-core jar包做了精简，已经不再包含任何web-application、LDAP或者namespace configuration的内容。现在核心包组是框架的基础支撑，使用Spring Security前理解这些基础的东西是非常必要的。
<h4>1.1 SecurityContextHolder, SecurityContext 和 Authentication 对象</h4>
<code class="cye-lm-tag">SecurityContextHolder</code>是最基础的一个对象，应用程序当前安全上下文的详细信息都存储在这个对象中，也保存了使用应用的主体对象的信息。默认情况下<code class="cye-lm-tag">SecurityContextHolder</code>使用<code>ThreadLocal</code><span> 来保存这些数据，这也意味着在即使security context没有显式传入方法，方法体中依然可以使用security context。如果用户主体的请求已经处理完，那么Spring Security会自动帮我们清除线程中的数据，所以使用<code>ThreadLocal</code>是非常安全的。</span>

有一些应用使用了多线程来实现特殊的任务，这时<span><code>ThreadLocal</code>并不能完全满足需求。举个例子，一个Swing客户端可能需要JVM的所有线程使用同一个security context。没关系，使用启动配置可以修改<code>SecurityContextHolder</code>保存数据的方式。对于一个独立应用程序，使用<code class="cye-lm-tag">SecurityContextHolder.MODE_GLOBAL</code> 策略。其他的应用程序中可能会希望，安全线程派生的线程也能使用相同的security context，这时可以使用<code class="cye-lm-tag">SecurityContextHolder.MODE_INHERITABLETHREADLOCAL</code>策略。两种修改默认的<code>SecurityContextHolder.MODE_THREADLOCAL</code> 策略的方法是：</span>
<ul>
 	<li>设置系统属性</li>
 	<li>调用<span>SecurityContextHolder的静态方法</span></li>
</ul>
对于大多数程序而言，默认的配置已经够了。
<h5>1.1.1 获取当前用户信息</h5>
<code>SecurityContextHolder</code><span> 内部存储了当前与应用程序进行交互的主体。Spring Security使用<code>Authentication</code> 对象来代表主体的信息。<code>Authentication</code>对象通常不需要程序员自己创建，但是查询<code>Authentication</code> 对象的情况是非常普遍的。所以可以使用下面的代码来获取授权用户的信息：</span>
<pre class="prettyprint">Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

if (principal instanceof UserDetails) {
String username = ((UserDetails)principal).getUsername();
} else {
String username = principal.toString();
}</pre>
方法<code class="cye-lm-tag">getContext()</code><span> 返回的是一个<code>SecurityContext</code> 接口实例，这个实例是thread-local存储器中的对象。Spring Security的大多数authentication（身份验证）机制将返回的<code>UserDetails</code>实例作为principal（主体）。</span>
<h4>1.2 UserDetailsService</h4>
从上面的代码中还可以发现，<span>principal对象是从<code class="cye-lm-tag">Authentication</code> 对象中获取的，而这个principal对象仅仅是一个 <code>Object</code>。大多数情况下principal对象可以转换为<code>UserDetails</code>对象。<code>UserDetails</code> 是Spring Security的一个核心接口，它代表了扩展的、面向应用的一个主体。你可以把<code>UserDetails</code> 当成是你自己的user database和Spring Security之间的一个代理。<code>UserDetails</code> 代表了从你的user database中获取的用户的信息，所以通常会把<code>UserDetails</code>转换为你自己应用程序使用的用户对象，这样你就可以使用一些业务逻辑方法，比如<code>getEmail()</code>, <code>getEmployeeNumber()</code> 这样的。</span>

现在你可能会有这样的疑问，什么时候应该提供<span><code>UserDetails</code> 对象？我该怎么做？我认为你说的这些都需要显式声明，我不需要自己写任何Java代码吗？最直接的回答是，这儿有一个叫<code>UserDetailsService</code>的接口，它唯一的一个方法只接收一个字符串username，然后返回<code>UserDetails</code>对象：</span>
<pre class="prettyprint">UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;</pre>
这个方法是获取用户信息最常用的，你可以在任何时候、任何地方使用它。

当身份验证成功后，<code>UserDetails</code><span> 对象被用来构造 <code class="cye-lm-tag">Authentication</code>对象。好消息是Spring Security已经提供了一些 <code>UserDetailsService</code>的实现，包括 in-memory map (<code>InMemoryDaoImpl</code>) 和其他使用JDBC的实现 (<code>JdbcDaoImpl</code>)。大多数用户可能倾向于自己实现 <code>UserDetailsService</code>，直接在已有的数据访问对象（DAO）之上实现一些诸如employees, customers, 或者其他的程序用户。但是有一点要记住，不论<code class="cye-lm-tag">UserDetailsService</code> 返回什么类型的主体，你都能从<code>SecurityContextHolder</code>对象上获取。</span>

&#x2660; 注意：<code class="cye-lm-tag">UserDetailsService</code>仅仅是一个用于获取用户数据的DAO，没有提供其他的功能，它的作用就是把用户数据提供给框架的其他组件。要区别的是，它不用来对用户进行授权，授权功能是由<code>AuthenticationManager</code>完成的。如果你想要提供自己的授权流程，可以通过<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#core-services-authentication-manager" class="cye-lm-tag">implement<span> </span><code>AuthenticationProvider</code></a><span> </span><span>来实现。</span>
<h4>1.3 GrantedAuthority</h4>
除了提供principal，<span> </span><code>Authentication</code>对象还提供了一个重要的方法：<code class="cye-lm-tag">getAuthorities()</code>。这个方法返回一个<span> </span><code class="cye-lm-tag">GrantedAuthority</code>对象数组。<span> </span><code class="cye-lm-tag">GrantedAuthority</code>对象就是已经授权给用户主体的权限。这种权限通常是用户“roles”（角色），就像<span> </span><code>ROLE_ADMINISTRATOR</code>或<span> </span><code>ROLE_HR_SUPERVISOR</code>。这类角色会在之后的web身份验证、方法验证和<span> domain object 验证的时候进行配置。Spring Security的其他部分能够解释这些权限。<code class="cye-lm-tag">GrantedAuthority</code>对象通常由<code class="cye-lm-tag">UserDetailsService</code>进行加载。</span>

<span><code class="cye-lm-tag">GrantedAuthority</code>通常是应用程序范围的权限，而不是某个特定域对象的权限。因此一般不会使用<code class="cye-lm-tag">GrantedAuthority</code>来代表对某个对象的权限，比如代表number 54的<code>Employee</code> 对象的访问权限。原因是，假如有成千上万的这样的权限，那么内存很快就会溢出（或者程序会花很长的时间来进行用户验证）。</span>
<h4>1.4 总结</h4>
主要内容如下：
<ul class="cye-lm-tag">
 	<li>
<p class="cye-lm-tag"><code>SecurityContextHolder</code>, to provide access to the<span> </span><code>SecurityContext</code>.</p>
</li>
 	<li>
<p class="cye-lm-tag"><code>SecurityContext</code>, to hold the<span> </span><code>Authentication</code><span> </span>and possibly request-specific security information.</p>
</li>
 	<li>
<p class="cye-lm-tag"><code>Authentication</code>, to represent the principal in a Spring Security-specific manner.</p>
</li>
 	<li>
<p class="cye-lm-tag"><code>GrantedAuthority</code>, to reflect the application-wide permissions granted to a principal.</p>
</li>
 	<li>
<p class="cye-lm-tag"><code>UserDetails</code>, to provide the necessary information to build an Authentication object from your application’s DAOs or other source of security data.</p>
</li>
 	<li class="cye-lm-tag">
<p class="cye-lm-tag"><code>UserDetailsService</code>, to create a<span> </span><code>UserDetails</code><span> </span>when passed in a<span> </span><code>String</code>-based username (or certificate ID or the like).</p>
</li>
</ul>