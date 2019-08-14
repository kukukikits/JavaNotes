# 12 Spring MVC Test Integration
Spring Security提供了与<a href='https://docs.spring.io/spring/docs/current/spring-framework-reference/testing.html#spring-mvc-test-framework'>Spring MVC Test</a>的全面集成。

## 12.1 Setting Up MockMvc and Spring Security
为了在Spring MVC Test中使用Spring Security，有必要将Spring Security  FilterChainProxy作为过滤器添加进来。还需要添加Spring Security的TestSecurityContextHolderPostProcessor 来支持在<a href='https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#running-as-a-user-in-spring-mvc-test-with-annotations'>Spring MVC Test中使用注释作为用户</a>运行。这些可以通过Spring Security的SecurityMockMvcConfigurers.springSecurity()来完成。

&#9728;注意：Spring Security Test支持需要spring-test-4.1.3.RELEASE 或者更高版本

```java
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
public class CsrfShowcaseTests {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@Before
	public void setup() {
		mvc = MockMvcBuilders
				.webAppContextSetup(context)
				.apply(springSecurity()) 
				.build();
	}

...
```
SecurityMockMvcConfigurers.springSecurity() 将执行我们需要的所有初始设置，从而将Spring Security与Spring MVC Test集成在一起。

## 12.2 SecurityMockMvcRequestPostProcessors

Spring MVC Test提供了一个称为RequestPostProcessor 的接口，可以用来修改请求。Spring Security提供了大量RequestPostProcessor 的实现，使测试更加容易。为了使用Spring Security的RequestPostProcessor实现，确保使用以下静态导入：
```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
```

### 12.1.1 Testing with CSRF Protection
在测试任何非安全的HTTP方法和使用Spring Security的CSRF保护时，您必须确保在请求中包含一个有效的CSRF令牌。要使用下列方法指定有效的CSRF令牌作为请求参数：
```java
mvc
	.perform(post("/").with(csrf()))
```
如果您喜欢，可以在请求头中包含CSRF令牌：
```java
mvc
	.perform(post("/").with(csrf().asHeader()))
```
您还可以使用以下方法测试提供无效的CSRF令牌：
```java
mvc
	.perform(post("/").with(csrf().useInvalidToken()))
```
### 12.2.2 在Spring MVC Test中使用用户进行测试
作为一个特定的用户运行测试通常是比较合理的。有两种简单的方法来填充用户：
- 使用RequestPostProcessor
- 使用注释
接下来就对这两种方法进行介绍

### 12.2.3 使用RequestPostProcessor

有许多选项可以将用户与当前的HttpServletRequest联系起来。例如，下面的设置会（不需要存在）以用户名“user”、密码“password”、角色”ROLE_USER”来运行。

&#9728; 注意：只有将用户与HttpServletRequest联系起来这才会有效。为了将请求与SecurityContextHolder关联起来，您需要确保SecurityContextPersistenceFilter与MockMvc实例相关联。有好几种方式可以实现：

- 调用<a href='https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#test-mockmvc-setup'>apply(springSecurity())</a>
- 将Spring Security的FilterChainProxy 添加到MockMvc
- 当使用 MockMvcBuilders.standaloneSetup时，手动添加SecurityContextPersistenceFilter 到MockMvc实例比较合理的
```java
mvc
	.perform(get("/").with(user("user")))
```
您可以轻松地进行定制。例如，下面的定义了一个用户（不需要存在），用户名是“admin”、密码“pass”，以及角色“ROLE_USER”和“ROLE_ADMIN”。
```java
mvc
	.perform(get("/admin").with(user("admin").password("pass").roles("USER","ADMIN")))
```
如果你想要使用UserDetails，你也可以很容易地指定它。例如，下面的用户将使用指定的UserDetails（不需要存在），并使用 UsernamePasswordAuthenticationToken来运行，该令牌具有指定用户详细信息的主体：
```java
mvc
	.perform(get("/").with(user(userDetails)))
```
您可以使用以下方式作为匿名用户运行：
```java
mvc
	.perform(get("/").with(anonymous()))
```
如果您使用的是默认用户，并且希望以匿名用户的身份执行一些请求，那么这一点特别有用。

如果您想要一个自定义Authentication（不需要存在），您可以使用以下内容：
```java
mvc
	.perform(get("/").with(authentication(authentication)))
```
您甚至可以使用以下内容定制SecurityContext：
```java
mvc
	.perform(get("/").with(securityContext(securityContext)))
```
我们还可以通过使用MockMvcBuilders的默认请求来确保每一个请求都运行特定的用户。例如，下面定义的用户（不需要存在），用户名为“admin”、密码“password”，角色是“ROLE_ADMIN”：
```java
mvc = MockMvcBuilders
		.webAppContextSetup(context)
		.defaultRequest(get("/").with(user("user").roles("ADMIN")))
		.apply(springSecurity())
		.build();
```
如果您发现在许多测试中使用了相同的用户，建议将用户移动到一个方法。例如，您可以在您自己名为CustomSecurityMockMvcRequestPostProcessors的类中指定下方法：
```java
public static RequestPostProcessor rob() {
	return user("rob").roles("ADMIN");
}
```
现在您可以在SecurityMockMvcRequestPostProcessors 上执行静态导入，并在测试中使用它
```java
import static sample.CustomSecurityMockMvcRequestPostProcessors.*;

...

mvc
	.perform(get("/").with(rob()))
```

#### Running as a User in Spring MVC Test with Annotations使用注释添加用户
作为RequestPostProcessor的另一种选择，您可以使用[Testing Method Security](https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#test-method)中描述的注释来创建用户。

例如，下面的测试用户，用户名为“user”、密码“password”，角色“ROLE_USER”。
```java
@Test
@WithMockUser
public void requestProtectedUrlWithUser() throws Exception {
mvc
		.perform(get("/"))
		...
}
```
或者，下面的测试用户，用户名为“user”、密码“password”、角色“ROLE_ADMIN”。
```java
@Test
@WithMockUser(roles="ADMIN")
public void requestProtectedUrlWithUser() throws Exception {
mvc
		.perform(get("/"))
		...
}
```
### 12.2.4 使用基本的HTTP身份验证来进行测试
虽然始终可以通过HTTP Basic进行身份验证，但记住请求头名、格式和编码值有点麻烦。现在可以使用Spring Security的httpBasic RequestPostProcessor来完成这项工作。例如，下面的代码片段：
```java
mvc
	.perform(get("/").with(httpBasic("user","password")))
```
这将尝试使用HTTP Basic来认证用户名“用户”和密码“密码”，并在HTTP请求中填充以下头部：
```text
Authorization: Basic dXNlcjpwYXNzd29yZA==
```




