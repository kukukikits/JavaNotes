<span>本节描述Spring Security提供的测试功能。</span>

<span>&#x2660; 要使用Spring Security测试支持，您必须在项目依赖里添加Spring Security test-5.0.7.RELEASE.jar。</span>
<h1>11  Testing Method Security</h1>
<span>本节演示如何使用Spring Security的测试支持来测试方法。</span><span>我们首先引入一个<code class="cye-lm-tag">MessageService</code> ，它要求用户通过身份验证才能访问它。</span>
<pre class="prettyprint">public class HelloMessageService implements MessageService {

	@PreAuthorize("authenticated")
	public String getMessage() {
		Authentication authentication = SecurityContextHolder.getContext()
		.getAuthentication();
		return "Hello " + authentication;
	}
}</pre>
<span>getMessage的结果是和当前Spring Security的<code class="cye-lm-tag">Authentication</code>对应的“Hello”字符串。下面显示了一些输出示例。</span>
<pre class="prettyprint">Hello 
org.springframework.security.authentication.UsernamePasswordAuthenticationToken@ca25360: 
Principal: 
org.springframework.security.core.userdetails.User@36ebcb: Username: user; Password: [PROTECTED]; Enabled: true; AccountNonExpired: true; credentialsNonExpired: true; AccountNonLocked: true; Granted Authorities: ROLE_USER; Credentials: [PROTECTED]; Authenticated: true; Details: null; Granted Authorities: ROLE_USER</pre>
<h2>11.1 安全测试设置</h2>
<span>在我们使用Spring Security测试支持之前，我们必须执行一些设置。下面可以看到一个例子：</span>
<pre class="prettyprint">1  @RunWith(SpringJUnit4ClassRunner.class) 
2  @ContextConfiguration 
   public class WithMockUserTests {</pre>
这是Spring Security Test的最简配置，其中：
<ol>
 	<li><span><code class="cye-lm-tag">@RunWith</code> 命令spring-test模块创建一个<code class="cye-lm-tag">ApplicationContext</code>。这和使用现有的Spring测试支持没有什么不同。要获得更多信息，请参考<a href="https://docs.spring.io/spring-framework/docs/4.0.x/spring-framework-reference/htmlsingle/#integration-testing-annotations-standard">Spring参考资料</a></span></li>
 	<li><code class="cye-lm-tag">@ContextConfiguration</code><span> 告诉spring-test应该用这个配置来创建<code class="cye-lm-tag">ApplicationContext</code>。由于没有指定配置，所以将尝试缺省的配置位置。这与使用现有的Spring测试支持没有什么不同。要获得更多信息，请参考<a href="https://docs.spring.io/spring-framework/docs/4.0.x/spring-framework-reference/htmlsingle/#testcontext-ctx-management">Spring参考资料</a></span></li>
</ol>
&#x2663; <span>WithSecurityContextTestExecutionListener将Spring Security挂钩到SpringTest中，它会确保使用正确的用户进行测试。它通过在运行我们的测试之前填充<code class="cye-lm-tag">SecurityContextHolder</code>来实现这一点。如果你使用的是reactive method security，您还需要<code class="cye-lm-tag">ReactorContextTestExecutionListener</code> 来填充<code class="cye-lm-tag">ReactiveSecurityContextHolder</code>。测试完成后，它将清除<code class="cye-lm-tag">SecurityContextHolder</code>。如果你只需要Spring Security相关的支持，你可以使用<code class="cye-lm-tag">@SecurityTestExecutionListeners</code>替换<code class="cye-lm-tag">@ContextConfiguration</code>。</span>

<span>由于我们在<code class="cye-lm-tag">HelloMessageService</code>上添加了<code>@PreAuthorize</code> 注释，所以需要验证用户来触发。如果我们运行下面的测试，我们会得到一个<code class="language-java cye-lm-tag" data-lang="java"><span class="typ cye-lm-tag">AuthenticationCredentialsNotFoundException</span></code>异常，所以测试是会通过的：</span>
<pre class="prettyprint">@Test(expected = AuthenticationCredentialsNotFoundException.class)
public void getMessageUnauthenticated() {
	messageService.getMessage();
}</pre>
<h2>11.2 @WithMockUser</h2>
<span>问题是"我们如何才能很方便地使用一个特定用户运行测试？”答案就是使用 <code>@WithMockUser</code>（使用虚拟用户）。下面的测试将以用户名为“user”、密码“password”，角色为"ROLE_USER"作为用户运行。</span>
<pre class="prettyprint">@Test
@WithMockUser
public void getMessageWithMockUser() {
String message = messageService.getMessage();
...
}</pre>
对于上代码的解释如下
<ol>
 	<li><span>用户名"user"不需要存在因为我们在模拟用户</span></li>
 	<li><span>在<code class="cye-lm-tag">SecurityContext</code> 中填充的<code class="cye-lm-tag">Authentication（身份）</code> 是<code class="cye-lm-tag">UsernamePasswordAuthenticationToken</code>类型的</span></li>
 	<li><code class="cye-lm-tag">Authentication</code><span> 上的主体是一个Spring Security的<code>User</code>对象</span></li>
 	<li><span>这个<code>User</code>的用户名是“user”、密码“password”，以及一个名为“ROLE_USER”的<code class="cye-lm-tag">GrantedAuthority(授权权限)</code> </span></li>
</ol>
上面的实例是非常有用的，因为我们可以使用这些默认配置。如果我们想使用其他的用户名呢？下面的测试将使用名为“customUser”的用户。同样地，这个用户不需要真实存在
<pre class="prettyprint">@Test
@WithMockUser("customUsername")
public void getMessageWithMockUserCustomUsername() {
	String message = messageService.getMessage();
...
}</pre>
同时也可以创建自定义的角色。比如：
<pre class="prettyprint">@Test
@WithMockUser(username="admin",roles={"USER","ADMIN"})
public void getMessageWithMockUserCustomUser() {
	String message = messageService.getMessage();
	...
}</pre>
如果不希望自动添加ROLE_的前缀，可以使用authorities属性。下面的测试会生成一个admin，权限为“USER”和“ADMIN”
<pre class="prettyprint">@Test
@WithMockUser(username = "admin", authorities = { "ADMIN", "USER" })
public void getMessageWithMockUserCustomAuthorities() {
	String message = messageService.getMessage();
	...
}</pre>
<span>当然，在每个测试方法上放置注释可能会有点麻烦。所以我们可以把注释放到类上面，这样每个测试都会使用这个特定的用户。比如下面的测试会使用用户名为“admin”，密码“password”，角色为“ROLE_USER”和“ROLE_ADMIN”的用户进行测试。</span>
<pre class="prettyprint">@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WithMockUser(username="admin",roles={"USER","ADMIN"})
public class WithMockUserTests {</pre>
<h2>11.3 @WithAnonymousUser</h2>
<span>使用<code class="cye-lm-tag">@WithAnonymousUser</code> 允许作为匿名用户运行。当您希望使用特定的用户运行大多数测试时，同时希望作为匿名用户运行另一些测试的时候这个是非常有用的。例如，下面的测试mockuser1和withMockUser2会使用<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#test-method-withmockuser">@WithMockUser</a>  ，而anonymous测试则会使用匿名用户</span>
<pre class="prettyprint">@RunWith(SpringJUnit4ClassRunner.class)
@WithMockUser
public class WithUserClassLevelAuthenticationTests {

	@Test
	public void withMockUser1() {
	}

	@Test
	public void withMockUser2() {
	}

	@Test
	@WithAnonymousUser
	public void anonymous() throws Exception {
		// override default to run as anonymous user
	}
}</pre>
<h2>11.4 @WithUserDetails</h2>
<span>虽然<code class="cye-lm-tag">@WithMockUser</code>是一种非常方便的启动方式，但它可能在所有情况下都不起作用。例如，应用程序期望认证主体（<code class="cye-lm-tag">Authentication</code>principal ）是特定类型的，这是很常见的。这样做是为了让应用程序可以将主体作为自定义类型，并减少与Spring Security耦合。</span>

<span>自定义主体通常是由自定义<code class="cye-lm-tag">UserDetailsService</code>返回的，该服务返回一个同时实现UserDetails和自定义类型的对象。对于这样的情况，使用自定义<code class="cye-lm-tag">UserDetailsService</code>来创建测试用户是很有用的。这正是<code class="cye-lm-tag">@WithUserDetails</code>所做的。</span>

<span>假设我们有一个<code class="cye-lm-tag">UserDetailsService</code>作为bean公开，下面的测试将使用 <code class="cye-lm-tag">UsernamePasswordAuthenticationToken</code>类型的<code class="cye-lm-tag">Authentication</code>，和<code>UserDetailsService</code>的返回值principal（用户名为“user”），作为测试用户。</span>
<pre class="prettyprint">@Test
@WithUserDetails
public void getMessageWithUserDetails() {
	String message = messageService.getMessage();
	...
}</pre>
<span>我们还可以定制用于从UserDetailsService查找到的用户的用户名。例如，这个测试将使用从UserDetailsService返回的主体，并使用“customUsername”的用户名来执行。</span>
<pre class="prettyprint">@Test
@WithUserDetails("customUsername")
public void getMessageWithUserDetailsCustomUsername() {
	String message = messageService.getMessage();
	...
}</pre>
<span>我们还可以提供一个显式的bean名称来查找UserDetailsService。例如，这个测试将使用名称为“myUserDetailsService”的UserDetailsService bean来查找“customUsername”用户。</span>
<pre class="prettyprint">@Test
@WithUserDetails(value="customUsername", userDetailsServiceBeanName="myUserDetailsService")
public void getMessageWithUserDetailsServiceBeanName() {
	String message = messageService.getMessage();
	...
}</pre>
<span>与<code class="cye-lm-tag">@WithMockUser</code> 一样，我们也可以将注释放置在类级别，以便每个测试都使用相同的用户。然而，与<code>@WithMockUser</code>不同的是，<code>@WithUserDetails</code>要求用户提前存在。</span>
<h2>11.5 @WithSecurityContext</h2>
<span>我们已经看到，如果我们没有使用自定义认证主体（<code>Authentication</code> principal），那么@withmockuser是一个很好的选择。然后就是<code>@WithUserDetails</code>允许我们使用自定义<code class="cye-lm-tag">UserDetailsService</code> 来创建我们的认证主体，但要求用户提前存在。我们现在将看到一个最灵活的选项。</span>

<span>我们可以创建自己的注解，这个注解会使用<code>@WithSecurityContext</code>创建任何我们想要的<code class="cye-lm-tag">SecurityContext</code>。例如，我们可以创建一个名为<code>@WithMockCustomUser</code>的注释，如下所示：</span>
<pre class="prettyprint">@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {

	String username() default "rob";

	String name() default "Rob Winch";
}</pre>
<span>您可以看到<code>@WithMockCustomUser</code> 是用 <code class="cye-lm-tag">@WithSecurityContext</code>注释标注的。它会给Spring Security Test发生一个信号，告诉Spring Security Test我们要创建一个<code class="cye-lm-tag">SecurityContext</code>用来测试。 <code class="cye-lm-tag">@WithSecurityContext</code>标注要求我们指定一个SecurityContextFactory，它将根据<code>@WithMockCustomUser</code> 注释创建一个新的<code>SecurityContext</code> 。</span>

<code>WithMockCustomUserSecurityContextFactory</code><span> 的实现如下：</span>
<pre class="prettyprint">public class WithMockCustomUserSecurityContextFactory
	implements WithSecurityContextFactory&lt;WithMockCustomUser&gt; {
	@Override
	public SecurityContext createSecurityContext(WithMockCustomUser customUser) {
		SecurityContext context = SecurityContextHolder.createEmptyContext();

		CustomUserDetails principal =
			new CustomUserDetails(customUser.name(), customUser.username());
		Authentication auth =
			new UsernamePasswordAuthenticationToken(principal, "password", principal.getAuthorities());
		context.setAuthentication(auth);
		return context;
	}
}</pre>
<span>我们现在可以用我们的新注解来注释一个测试类或一个测试方法。</span>Spring Security的WithSecurityContextTestExecutionListener会确保我们的SecurityContext被适当地填充。

<span>在创建自己的<code class="cye-lm-tag">WithSecurityContextFactory</code> 实现时，是可以使用标准的Spring注解来注释的。例如，WithUserDetailsSecurityContextFactory使用<code>@Autowired</code> 注解来获取UserDetailsService：</span>
<pre class="prettyprint">final class WithUserDetailsSecurityContextFactory
	implements WithSecurityContextFactory&lt;WithUserDetails&gt; {

	private UserDetailsService userDetailsService;

	@Autowired
	public WithUserDetailsSecurityContextFactory(UserDetailsService userDetailsService) {
		this.userDetailsService = userDetailsService;
	}

	public SecurityContext createSecurityContext(WithUserDetails withUser) {
		String username = withUser.value();
		Assert.hasLength(username, "value() must be non-empty String");
		UserDetails principal = userDetailsService.loadUserByUsername(username);
		Authentication authentication = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		return context;
	}
}</pre>
<h2>11.6 Test Meta Annotations</h2>
<span>如果您经常在测试中重用相同的用户，那么必须反复指定属性是非常麻烦的。例如，如果有许多测试都和管理用户相关，你可能会这样写：</span>
<pre class="prettyprint">@WithMockUser(username="admin",roles={"USER","ADMIN"})</pre>
<span>我们可以使用元注释，而不是到处重复地写上面的代码。例如，我们可以创建一个名为WithMockAdmin的元注释：</span>例如，我们可以创建一个名为WithMockAdmin的元注释：
<pre class="prettyprint">@Retention(RetentionPolicy.RUNTIME)
@WithMockUser(value="rob",roles="ADMIN")
public @interface WithMockAdmin { }</pre>
<span>现在我们可以使用 <code>@WithMockAdmin</code>，使用方法和<code>@WithMockUser</code>一样。</span>

<span>Meta注释可以和上面描述的任何测试注释一起工作。例如，这意味着我们可以为 <code class="cye-lm-tag">@WithUserDetails("admin")</code>创建一个meta标注。</span>