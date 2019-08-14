<h1>10 Core Services</h1>
<span>现在我们对Spring Security体系结构及其核心类有了一个总体的认识，让我们更近一步看一下几个核心接口及其实现，特别是 <code>AuthenticationManager</code>, <code>UserDetailsService</code> 和 <code>AccessDecisionManager</code>。这些服务在文档的其他部分都有出现，所以知道怎么配置和使用是非常重要的。</span>
<h2>10.1 AuthenticationManager, ProviderManager and AuthenticationProvider</h2>
<span><code class="cye-lm-tag">AuthenticationManager</code> 只是一个接口，所以其实现可以任选，但是在实际中如何操作呢？如果我们需要检查多个身份验证数据库，或者检查不同的身份验证服务（如数据库和LDAP服务）时该怎么做呢？</span>

Spring Security中默认的实现类是<span> </span><code>ProviderManager</code>，<span>它不处理身份验证请求，而是将其委托给已经配置好的一系列<code>AuthenticationProvider</code>们，每一个<code>AuthenticationProvider</code>都会被查询，查看它是否能够处理身份验证。每个provider要么抛出一个异常，要么返回一个完全填充的<code>Authentication</code> 对象。还记得 <code class="cye-lm-tag">UserDetails</code> 和 <code>UserDetailsService</code>吗?如果不记得，那就回到前一章，更新你的记忆。验证身份验证请求的最常见方法是加载相应的<code>UserDetails</code> ，并根据用户输入的密码检查已加载的密码。这是使用 <code>DaoAuthenticationProvider</code>实现的。加载的<code>UserDetails</code>对象——尤其是它所包含的<code>GrantedAuthority</code> ，在构建完全填充的 <code>Authentication</code>对象时会被用到，成功验证后返回<code>Authentication</code> 对象并把它存储在<code>SecurityContext</code>里。</span>

<span>如果您正在使用名称空间，那么 <code class="cye-lm-tag">ProviderManager</code>的实例就会在内部创建和维护，然后你再使用命名空间的authentication provider 元素（详见 <a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#ns-auth-manager">the namespace chapter</a>）来添加providers。在这种情况下，您不应该在应用程序上下文中声明<code>ProviderManager</code>  bean。但是，如果您没有使用名称空间，那么您就会这样声明：</span>
<pre class="prettyprint">&lt;bean id="authenticationManager"
		class="org.springframework.security.authentication.ProviderManager"&gt;
	&lt;constructor-arg&gt;
		&lt;list&gt;
			&lt;ref local="daoAuthenticationProvider"/&gt;
			&lt;ref local="anonymousAuthenticationProvider"/&gt;
			&lt;ref local="ldapAuthenticationProvider"/&gt;
		&lt;/list&gt;
	&lt;/constructor-arg&gt;
&lt;/bean&gt;</pre>
上面的例子中有三个providers。<span>它们按照显示的顺序进行，每个提供者都可以尝试认证，或者通过简单返回null来跳过认证。如果所有的实现都返回null，那么<code>ProviderManager</code> 将抛出一个 <code class="cye-lm-tag">ProviderNotFoundException</code>。如果您有兴趣了解更多关于链接providers的信息，请参考ProviderManager Javadoc。</span>

身份验证机制，如web表单登录过程过滤器，会被注入到<code>ProviderManager</code>中，然后会被调用用来验证请求。<span>您所需要的providers 有时可以与身份验证机制互换，在其他时候，它们将依赖于特定的身份验证机制。例如，<code class="cye-lm-tag">DaoAuthenticationProvider</code> 和<code>LdapAuthenticationProvider</code> 与简单的用户名/密码请求验证机制相互兼容，所以可以和表单登录或HTTP基本身份验证一起使用。另一方面，一些认证机制创建了身份验证请求对象，只能由特定的<code>AuthenticationProvider</code>来解释。例如JA-SIG CAS，它使用服务票的概念来进行身份验证，所以只能用<code>CasAuthenticationProvider</code>来验证身份。你不必太在意这个，因为如果你忘了注册一个合适的provider，当进行身份验证时，您将简单地接收到一个<code class="cye-lm-tag">ProviderNotFoundException</code> 。</span>
<h3>10.1.1 身份验证成功后删除凭证</h3>
<span>默认情况下（从Spring Security 3.1开始），当成功验证身份后，<code class="cye-lm-tag">ProviderManager</code> 将尝试从<code>Authentication</code> 中清除任何敏感的凭证信息。这可以防止密码保留时间超过必要的时间。</span>

<span>当您使用缓存的用户对象时，这可能会导致问题，例如，会对提高无状态应用程序的性能产生影响。如果一个已经清除了凭证的<code>Authentication</code> 包含对缓存中的对象的引用（如<code class="cye-lm-tag">UserDetails</code> 实例），那么它将无法对缓存的值进行身份验证。如果你使用了缓存，你需要考虑到这一点。一个明显的解决方案是首先复制对象的副本，要么在缓存实现中，要么在AuthenticationProvider中实现。或者，您可以禁用<code class="cye-lm-tag">ProviderManager</code>的eraseCredentialsAfterAuthentication属性。有关更多信息，请参见Javadoc。</span>
<h3>10.1.2 DaoAuthenticationProvider</h3>
<span>Spring Security实现的最简单的<code>AuthenticationProvider</code> 是<code>DaoAuthenticationProvider</code>，这也是该框架最早支持的一种。它利用<code class="cye-lm-tag">UserDetailsService</code>（作为一个DAO）来查找用户名、密码和<code>GrantedAuthority</code>。它通过比较<code class="cye-lm-tag">UsernamePasswordAuthenticationToken</code>中提交的密码与<code>UserDetailsService</code>加载的密码进行比较，从而对用户进行身份验证。配置非常简单：</span>
<pre class="prettyprint">&lt;bean id="daoAuthenticationProvider"
	class="org.springframework.security.authentication.dao.DaoAuthenticationProvider"&gt;
&lt;property name="userDetailsService" ref="inMemoryDaoImpl"/&gt;
&lt;property name="passwordEncoder" ref="passwordEncoder"/&gt;
&lt;/bean&gt;</pre>
<span><code>PasswordEncoder</code> 是可选的。<code>PasswordEncoder</code>为<code class="cye-lm-tag">UserDetailsService</code>返回的<code class="cye-lm-tag">UserDetails</code>对象提供密码的加密和解密。<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#core-services-password-encoding">稍后对此进行更详细的讨论</a>。</span>
<h2>10.2 UserDetailsService实现</h2>
<span>正如在前面的参考指南中所提到的，大多数认证providers都利用<code>UserDetails</code> 和 <code>UserDetailsService</code> 接口。回想一下 <code>UserDetailsService</code>只提供了一个方法：</span>
<pre class="prettyprint">UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;</pre>
<span>返回的<code>UserDetails</code>是一个接口，它提供了一个getter方法，能够保证提供非空的身份验证信息（如用户名、密码、授权的权限，以及用户是否被禁）。大多数身份验证providers都会使用<code>UserDetailsService</code> ，即使在身份验证时不使用用户名和密码。他们可能会使用返回的<code>UserDetails</code>对象，从而获取<code class="cye-lm-tag">GrantedAuthority</code>，因为其他一些系统（如LDAP或x.509或CAS等）已经承担了验证凭证的责任。</span>

<span>鉴于<code>UserDetailsService</code> 的实现如此简单，对于用户来说，使用他们选择的持久性策略来检索认证信息应该很容易。话虽如此，Spring Security确实包含了一些有用的基本实现，我们将在下面看到。</span>
<h3>10.2.1 In-Memory Authentication</h3>
<span>自定义一个从持久性引擎中提取信息的UserDetailsService是比较容易的，但是大多数应用没必要搞得这么复杂。如果你构建的是一个应用的原型，或者仅仅是集成Spring Security，那么你就没有必要在配置数据库、实现自定义UserDetailsService的事情上浪费时间。为了满足这种需求，我们只需从security的命名空间中使用 <code>user-service</code>元素就可以了。</span>
<pre class="prettyprint">&lt;user-service id="userDetailsService"&gt;
&lt;!-- Password is prefixed with {noop} to indicate to DelegatingPasswordEncoder that
NoOpPasswordEncoder should be used. This is not safe for production, but makes reading
in samples easier. Normally passwords should be hashed using BCrypt --&gt;
&lt;user name="jimi" password="{noop}jimispassword" authorities="ROLE_USER, ROLE_ADMIN" /&gt;
&lt;user name="bob" password="{noop}bobspassword" authorities="ROLE_USER" /&gt;
&lt;/user-service&gt;</pre>
同时也支持配置外部配置文件：
<pre class="prettyprint">&lt;user-service id="userDetailsService" properties="users.properties"/&gt;</pre>
然后users.properties中只需要按照下面的模板设置用户信息：
<pre class="prettyprint">username=password,grantedAuthority[,grantedAuthority][,enabled|disabled]</pre>
比如：
<pre class="prettyprint">jimi=jimispassword,ROLE_USER,ROLE_ADMIN,enabled
bob=bobspassword,ROLE_USER,enabled</pre>
<h3>10.2.2 JdbcDaoImpl</h3>
<span>Spring Security还包括一个<code>UserDetailsService</code> ，它可以从JDBC数据源获得认证信息。为了避免使用复杂的对象关系映射器（ORM），Spring使用了内部的JDBC。如果您的应用程序确实使用了ORM工具，您可能更愿意编写一个定制的<code>UserDetailsService</code>来重用您可能已经创建的映射文件。下面是<code>JdbcDaoImpl</code>的一个例子：</span>
<pre class="prettyprint">&lt;bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource"&gt;
&lt;property name="driverClassName" value="org.hsqldb.jdbcDriver"/&gt;
&lt;property name="url" value="jdbc:hsqldb:hsql://localhost:9001"/&gt;
&lt;property name="username" value="sa"/&gt;
&lt;property name="password" value=""/&gt;
&lt;/bean&gt;

&lt;bean id="userDetailsService"
	class="org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl"&gt;
&lt;property name="dataSource" ref="dataSource"/&gt;
&lt;/bean&gt;</pre>
<span>您可以通过修改上面所示的<code class="cye-lm-tag">DriverManagerDataSource</code>来使用不同的关系数据库。您还可以使用从JNDI获得的全局数据源，就像任何其他Spring配置一样。</span>
<h5 id="authority-groups" class="cye-lm-tag"><strong>Authority Groups</strong></h5>
<span>默认情况下，<code>JdbcDaoImpl</code>会在权限信息直接映射到用户的前提假设下 (see the <a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#appendix-schema">database schema appendix</a>)，为每个用户加载权限信息。另一种方法是将权限划分为组，然后为用户分配组。有些人更喜欢这种方法作为管理用户权利的一种方式。请参阅<code>JdbcDaoImpl</code>Javadoc以获得更多关于如何启用组成员权限的信息。组策略也包含在附录中。</span>
<h2>10.3 密码加密</h2>
<span>Spring Security的<code>PasswordEncoder</code>接口用于执行密码的单向转换，以允许密码安全地存储。由于<code>PasswordEncoder</code>是用来单向加密的，它是不能用来进行双向加密的（即存储用于认证数据库的凭证）。通常，<code>PasswordEncoder</code>用于存储密码，在身份验证时需要将密码与用户提供的密码进行比较。</span>
<h3>10.3.1 密码历史</h3>
<span>多年来，存储密码的标准机制已经进化了。在开始的时候，密码是以纯文本形式存储的。这些密码被认为是安全的，因为存储密码的数据保存在凭证中。然而，恶意用户能够通过SQL注入等攻击找到用户名和密码的大量“数据诊断”。随着越来越多的用户凭证公开，公共安全专家们意识到我们需要做更多的工作来保护用户的密码。</span>

<span>随后开发人员通过单向hash加密，比如SHA-256来加密密码。当用户试图进行身份验证时，会对hash加密后的密码进行比较。这也意味着系统只需要存储hash后的密码就行了。即使发生了错误，那么也只有一种hash码被暴露出来。因为hash是单向的，而且很难猜出给定的哈希值，所以在系统中算出每个密码是不值得的。为了破解这样的系统，恶意用户会使用为<a href="https://en.wikipedia.org/wiki/Rainbow_table">彩虹表</a>的查找表。他们不是每次都要对每个密码进行猜测，而是计算一次密码并将其存储在一个查找表中。</span>

<span>为了降低彩虹表的有效性，鼓励开发人员使用加盐密码salted passwords。不要只使用密码作为哈希函数的输入，还要为每个用户的密码生成随机字节（称为salt）。盐和用户的密码将通过哈希函数运行，从而生成唯一的hash。盐会以明文形式存储在用户的密码旁边。然后，当用户试图进行身份验证时，对用户输入的密码与存储在数据库中的盐进行hash加密，然后拿这个hash密码和存储在数据库中正确的hash密码进行比较。盐的使用意味着彩虹表不再有效，因为每一个盐和密码组合都是不同的。</span>

<span>在现代，我们意识到加密hash（如SHA-256）不再安全。这是因为使用现代硬件，我们可以每秒执行数十亿次的hash计算。这意味着我们可以轻松地破解每个密码。</span>

<span>现在，鼓励开发人员利用自适应的单向函数(adaptive one-way functions )来存储密码。使用自适应的单向函数来验证密码是一件非常消耗系统资源（如CPU、内存等）的事。自适应的单向函数允许配置一个“work factor”，当硬件变得更好时，它可以增长。建议将“work factor”调到，系统验证密码大约花费1秒的程度。这种取舍是为了让攻击者很难破解密码，同时不会给你自己的系统带来过多的负担。Spring Security会提供一个合适的“work factor”，但是，鼓励用户自定义他们自己的系统的“work factor”，因为不同系统的性能会有很大的不同。应该使用的自适应单向函数的例子包括<a href="https://en.wikipedia.org/wiki/Bcrypt">bcrypt</a>、 <a href="https://en.wikipedia.org/wiki/PBKDF2">PBKDF2</a>, <a href="https://en.wikipedia.org/wiki/Scrypt">scrypt</a>, 和<a href="https://en.wikipedia.org/wiki/Argon2">Argon2</a>。</span>

<span>因为自适应的单向函数是资源密集型的，所以为每个请求验证用户名和密码将大大降低应用程序的性能。Spring Security或其他任何库都无法加快密码的验证，因为安全性是通过资源密集来保证的。鼓励用户将长期凭证（即用户名和密码）交换为短期凭证（即会话、OAuth令牌等）。短期凭证可以快速有效地验证，而不会造成任何安全损失。</span>
<h3>10.3.2 DelegatingPasswordEncoder</h3>
<span>在Spring Security 5.0之前，默认的<code>PasswordEncoder</code> 是 <code>NoOpPasswordEncoder</code>，它需要纯文本密码。基于密码历史部分，您可能会认为默认的 <code class="cye-lm-tag">PasswordEncoder</code>现在是类似于<code>BCryptPasswordEncoder</code>的东西。然而，这忽略了三个现实世界的问题：</span>
<ul>
 	<li><span>有许多应用程序是无法轻易迁移旧密码编码的</span></li>
 	<li>用于演示密码存储的例子将再次改变</li>
 	<li><span>作为一个框架，Spring Security不能频繁地进行破坏更改</span></li>
</ul>
<span>相反，Spring Security引入了 <code class="cye-lm-tag">DelegatingPasswordEncoder</code>，它解决了下面所有的问题：</span>
<ul>
 	<li><span>确保密码使用当前的密码存储规范进行编码</span></li>
 	<li><span>允许以现代和遗留格式验证密码</span></li>
 	<li><span>允许在未来升级编码</span></li>
</ul>
<span>您可以很容易地使用<code>PasswordEncoderFactories</code>构造委托<code>DelegatingPasswordEncoder</code>的实例：</span>
<pre class="prettyprint">PasswordEncoder passwordEncoder =
    PasswordEncoderFactories.createDelegatingPasswordEncoder();</pre>
<span>或者，您可以创建自己的自定义实例。例如:</span>
<pre class="prettyprint">String idForEncode = "bcrypt";
Map encoders = new HashMap&lt;&gt;();
encoders.put(idForEncode, new BCryptPasswordEncoder());
encoders.put("noop", NoOpPasswordEncoder.getInstance());
encoders.put("pbkdf2", new Pbkdf2PasswordEncoder());
encoders.put("scrypt", new SCryptPasswordEncoder());
encoders.put("sha256", new StandardPasswordEncoder());

PasswordEncoder passwordEncoder =
    new DelegatingPasswordEncoder(idForEncode, encoders);</pre>
<h5 id="pe-dpe-format" class="cye-lm-tag"><strong>Password Storage Format密码存储格式</strong></h5>
<span>密码的一般格式是：</span>
<pre class="prettyprint">{id}encodedPassword</pre>
<span>id是用来标识使用的是哪种<code>PasswordEncoder</code>，encodedPassword是所选的PasswordEncoder的原始加密后密码。这个id必须在密码的开头，以{开始，以}结束。如果无法找到id，则id将为null。例如，下面可能是使用不同id编码的密码列表。所有的原始密码都是“password”:</span>
<pre class="prettyprint">1  {bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG 
2  {noop}password 
3  {pbkdf2}5d923b44a6d129f3ddf3e3c8d29412723dcbde72445e8ef6bf3b508fbf17fa4ed4d6b99ca763d8dc 
4  {scrypt}$e0801$8bWJaSu2IKSn9Z9kM+TPXfOc/9bdYSrN1oD9qfVThWEwdRTnO7re7Ei+fUZRJ68k9lTyuTeUp4of4g24hHnazw==$OAOec05+bXxvuu/1qZ6NUR+xQYvYv7BeL1QxwRpY5Pc=  
5  {sha256}97cde38028ad898ebc02e690819fa220e88c62e0699403e94fff291cfffaf8410849f27605abcbc0</pre>
<ol>
 	<li><span>第一个密码的<code>PasswordEncoder</code>id是<code>bcrypt</code> ，加密后的密码是<code>$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG</code>。当验证密码时，会代理给 <code class="cye-lm-tag">BCryptPasswordEncoder</code>进行验证。</span></li>
 	<li><span>第二个密码的<code>PasswordEncoder</code>id是<code>noop</code> ，加密后的密码是 <code>password</code>。密码验证会代理给 <code class="cye-lm-tag">NoOpPasswordEncoder</code></span></li>
 	<li><span>第三个密码的<code>PasswordEncoder</code>id是<code>pbkdf2</code>，加密后的密码是<code class="cye-lm-tag">5d923b44a6d129f3ddf3e3c8d29412723dcbde72445e8ef6bf3b508fbf17fa4ed4d6b99ca763d8dc</code>。密码验证会代理给 <code>Pbkdf2PasswordEncoder</code></span></li>
 	<li><span>第四个密码的<code>PasswordEncoder</code>id是 <code>scrypt</code>，加密后的密码是<code class="cye-lm-tag">$e0801$8bWJaSu2IKSn9Z9kM+TPXfOc/9bdYSrN1oD9qfVThWEwdRTnO7re7Ei+fUZRJ68k9lTyuTeUp4of4g24hHnazw==$OAOec05+bXxvuu/1qZ6NUR+xQYvYv7BeL1QxwRpY5Pc=</code>。密码验证会代理给 <code>SCryptPasswordEncoder</code></span></li>
 	<li><span>第五个密码的<code>PasswordEncoder</code>id是<code>sha256</code>，加密后的密码是<code class="cye-lm-tag">97cde38028ad898ebc02e690819fa220e88c62e0699403e94fff291cfffaf8410849f27605abcbc0</code>。密码验证会代理给 <code class="cye-lm-tag">StandardPasswordEncoder</code></span></li>
</ol>
&#x2665;<span>Some users might be concerned that the storage format is provided for a potential hacker. T<span style="color: #ff0000;">his is not a concern because the storage of the password does not rely on the algorithm being a secret</span>. Additionally, most formats are easy for an attacker to figure out without the prefix. For example, BCrypt passwords often start with </span><code>$2a$</code><span>.</span> <span>一些用户可能会担心存储格式会被黑客利用。<span style="color: #ff0000;">这不需要担心，因为黑客并不知道保存密码的时候用的是什么算法。</span>相反，没有前缀的时候大多数格式对于攻击者来说都是很容易破解的。比如，BCrypt密码通常以$2a$开始。</span>
<h5 id="password-encoding"><strong>Password Encoding密码加密</strong></h5>
<span>传递给构造函数的用于标识加密方式的id决定了使用哪个<code>PasswordEncoder</code>进行加密。在我们上面构建的 <code>DelegatingPasswordEncoder</code>中，这意味着加密的密码会委托给<code>BCryptPasswordEncoder</code>，且密码使用<code>{bcrypt}</code>前缀。最终结果如下：</span>
<pre class="prettyprint">{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG</pre>
<h5 id="password-matching" class="cye-lm-tag"><strong>Password Matching密码匹配</strong></h5>
密码匹配是基于<code>{id}</code>和id对应的<code>PasswordEncoder</code><span> 。我们在<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#pe-dpe-format">Password Storage Format</a>中的示例，说明了如何完成此工作。默认情况下，使用密码和未映射的id（包括null id）调用<code>matches(CharSequence, String)</code> 将导致 <code>IllegalArgumentException</code>。这种异常可以通过设置默认<code>PasswordEncoder</code>来避免：</span>
<pre class="prettyprint">DelegatingPasswordEncoder.setDefaultPasswordEncoderForMatches(PasswordEncoder)</pre>
<span>通过使用id我们可以匹配任何密码编码，但是加密的时候我们还是会使用最现代的加密方式。这很重要，因为与加密不同的是，使用密码散列目的是为了防止使用一些比较简单的方法来恢复明文。由于没有办法恢复明文，所以很难迁移密码。用户可以很容易地迁移NoOpPasswordEncoder，所以为了使刚Getting Started Experience简单一点，我们默认选择这种方式。</span>
<h5 id="getting-started-experience"><strong>Getting Started Experience</strong></h5>
<span>如果您正在组装一个演示或一个样品，那么花时间对用户的密码进行hash加密是有点麻烦的。我们提供了一些便捷的方法，但是你不要在生成环境中使用它：</span>
<pre class="prettyprint">User user = User.withDefaultPasswordEncoder()
  .username("user")
  .password("password")
  .roles("user")
  .build();
System.out.println(user.getPassword());
// {bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG</pre>
创建多个用户：
<pre class="prettyprint">UserBuilder users = User.withDefaultPasswordEncoder();
User user = users
  .username("user")
  .password("password")
  .roles("USER")
  .build();
User admin = users
  .username("admin")
  .password("password")
  .roles("USER","ADMIN")
  .build();</pre>
<span>上面的处理确实是对存储的密码进行了hash加密，但是密码仍然暴露在内存和编译后的源代码中。</span>
<h5 id="troubleshooting" class="cye-lm-tag"><strong>troubleshooting</strong></h5>
<div class="paragraph">
<p class="cye-lm-tag"><span>当储存的一个密码没有id时，就会发生以下错误</span></p>

<pre class="prettyprint">java.lang.IllegalArgumentException: There is no PasswordEncoder mapped for the id "null"
	at org.springframework.security.crypto.password.DelegatingPasswordEncoder$UnmappedIdPasswordEncoder.matches(DelegatingPasswordEncoder.java:233)
	at org.springframework.security.crypto.password.DelegatingPasswordEncoder.matches(DelegatingPasswordEncoder.java:196)</pre>
<span>解决这个问题的最简单方法是弄清楚你的密码的存储方式，并且提供了正确的<code>PasswordEncoder</code>。如果您是从Spring Security 4.2迁移过来的。你可以通过暴露一个NoOpPasswordEncoder bean来恢复到以前的行为。例如，如果您正在使用Java配置，您可以创建下面的配置：</span>

&#x2666; 警告：<span>恢复到NoOpPasswordEncoder是不安全的。相反，您应该迁移到使用<code class="cye-lm-tag">DelegatingPasswordEncoder</code> 来支持安全的密码编码。</span>
<pre class="prettyprint">@Bean
public static NoOpPasswordEncoder passwordEncoder() {
    return NoOpPasswordEncoder.getInstance();
}</pre>
<span>如果您正在使用XML配置，您可以使用id PasswordEncoder来公开一个passwordEncoder：</span>
<pre class="prettyprint">&lt;b:bean id="passwordEncoder"
        class="org.springframework.security.crypto.password.NoOpPasswordEncoder" factory-method="getInstance"/&gt;</pre>
<span>或者，您给所有密码添加id前缀，并继续使用<code class="cye-lm-tag">DelegatingPasswordEncoder</code>。例如，如果您正在使用BCrypt，您可以将密码从以下内容：</span>
<pre class="prettyprint">$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG</pre>
修改为：
<pre class="prettyprint">{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG</pre>
<span>要获得映射的完整清单，请参考Javadoc  <a href="https://docs.spring.io/spring-security/site/docs/5.0.x/api/org/springframework/security/crypto/factory/PasswordEncoderFactories.html">PasswordEncoderFactories</a></span>
<h3>10.3.3 BCryptPasswordEncoder</h3>
<span><code class="cye-lm-tag">BCryptPasswordEncoder</code> 使用广泛使用的 <a href="https://en.wikipedia.org/wiki/Bcrypt">bcrypt</a>算法对密码进行hash。为了防止密码被破解，bcrypt有意地让计算变的很慢。与其他自适应单向函数一样，系统验证密码的时间应该调整到大约1秒。</span>
<pre class="prettyprint">// Create an encoder with strength 16
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(16);
String result = encoder.encode("myPassword");
assertTrue(encoder.matches("myPassword", result));</pre>
<h3>10.3.4 Pbkdf2PasswordEncoder</h3>
<p id="pe-pbkdf2pe" class="cye-lm-tag"><code>Pbkdf2PasswordEncoder</code><span> 使用了 <a href="https://en.wikipedia.org/wiki/PBKDF2">PBKDF2</a> 算法。和bcrypt一样，也是有意地让算法变的很慢，同样需要调参到1秒。当需要使用 FIPS 证书时，这个算法是个不错的选择。</span></p>

<pre class="prettyprint">// Create an encoder with all the defaults
Pbkdf2PasswordEncoder encoder = new Pbkdf2PasswordEncoder();
String result = encoder.encode("myPassword");
assertTrue(encoder.matches("myPassword", result));</pre>
</div>
<h3>10.3.5 SCryptPasswordEncoder</h3>
<code class="cye-lm-tag">SCryptPasswordEncoder</code><span> 使用 <a href="https://en.wikipedia.org/wiki/Scrypt">scrypt</a>算法。该算法也是故意让计算很慢，同时还会消耗大量内存。同样地调参到1s。</span>
<pre class="prettyprint">// Create an encoder with all the defaults
SCryptPasswordEncoder encoder = new SCryptPasswordEncoder();
String result = encoder.encode("myPassword");
assertTrue(encoder.matches("myPassword", result));</pre>
<h3>10.3.6 Other PasswordEncoders</h3>
<span>为了保持后向兼容，有很多其他的PasswordEncoder。它们都被弃用，因为它们都不再安全。然而，由于很难迁移现有的遗留系统，所以这些PasswordEncoder还会保留。</span>
<h2>10.4 Jackson支持</h2>
<span>Spring Security增加了Jackson对Spring Security相关类的支持。这可以提高在分布式会话中序列化Spring Security相关类的性能（如：会话复制、Spring Session等）。</span>

<span>要使用它，把<code class="cye-lm-tag">JacksonJacksonModules.getModules(ClassLoader)</code> 的返回值注册为一个 <a href="http://wiki.fasterxml.com/JacksonFeatureModules">Jackson Modules</a>.</span>
<pre class="prettyprint">ObjectMapper mapper = new ObjectMapper();
ClassLoader loader = getClass().getClassLoader();
List&lt;Module&gt; modules = SecurityJackson2Modules.getModules(loader);
mapper.registerModules(modules);

// ... use ObjectMapper as normally ...
SecurityContext context = new SecurityContextImpl();
// ...
String json = mapper.writeValueAsString(context);</pre>