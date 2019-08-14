<a href='https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/htmlsingle/#test-webflux'>官方文档</a>

# 13 WebFlux Support
Spring Security为方法安全性和WebFlux提供了与Spring WebFlux的测试集成。你可以参考<a href="https://github.com/spring-projects/spring-security/tree/5.0.7.RELEASE/samples/javaconfig/hellowebflux-method">hellowebflux-method</a>的例子。

## 13.1 响应式方法安全
例如，我们可以使用与<a href='https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#test-method'>Testing Method Security</a>中相同的设置和注释来测试<a href='https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#jc-erms'>EnableReactiveMethodSecurity</a>中的例子。下面是测试需要的最简配置：

```java
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = HelloWebfluxMethodApplication.class)
public class HelloWorldMessageServiceTests {
	@Autowired
	HelloWorldMessageService messages;

	@Test
	public void messagesWhenNotAuthenticatedThenDenied() {
		StepVerifier.create(this.messages.findMessage())
			.expectError(AccessDeniedException.class)
			.verify();
	}

	@Test
	@WithMockUser
	public void messagesWhenUserThenDenied() {
		StepVerifier.create(this.messages.findMessage())
			.expectError(AccessDeniedException.class)
			.verify();
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	public void messagesWhenAdminThenOk() {
		StepVerifier.create(this.messages.findMessage())
			.expectNext("Hello World!")
			.verifyComplete();
	}
}
```java

## 13.2 WebTestClientSupport
Spring Security提供了与WebTestClient的集成。设置如下：
```java
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = HelloWebfluxMethodApplication.class)
public class HelloWebfluxMethodApplicationTests {
	@Autowired
	ApplicationContext context;

	WebTestClient rest;

	@Before
	public void setup() {
		this.rest = WebTestClient
			.bindToApplicationContext(this.context)
			// add Spring Security test Support
			.apply(springSecurity())
			.configureClient()
			.filter(basicAuthentication())
			.build();
	}
	// ...
}
```

### 13.2.1 身份认证
添加对WebTestClient 的支持后，我们可以使用注释或者mutateWith 来进行测试：
```java
@Test
public void messageWhenNotAuthenticated() throws Exception {
	this.rest
		.get()
		.uri("/message")
		.exchange()
		.expectStatus().isUnauthorized();
}

// --- WithMockUser ---

@Test
@WithMockUser
public void messageWhenWithMockUserThenForbidden() throws Exception {
	this.rest
		.get()
		.uri("/message")
		.exchange()
		.expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
}

@Test
@WithMockUser(roles = "ADMIN")
public void messageWhenWithMockAdminThenOk() throws Exception {
	this.rest
		.get()
		.uri("/message")
		.exchange()
		.expectStatus().isOk()
		.expectBody(String.class).isEqualTo("Hello World!");
}

// --- mutateWith mockUser ---

@Test
public void messageWhenMutateWithMockUserThenForbidden() throws Exception {
	this.rest
		.mutateWith(mockUser())
		.get()
		.uri("/message")
		.exchange()
		.expectStatus().isEqualTo(HttpStatus.FORBIDDEN);
}

@Test
public void messageWhenMutateWithMockAdminThenOk() throws Exception {
	this.rest
		.mutateWith(mockUser().roles("ADMIN"))
		.get()
		.uri("/message")
		.exchange()
		.expectStatus().isOk()
		.expectBody(String.class).isEqualTo("Hello World!");
}
```

### 13.2.2 CSRF支持
Spring Security还为WebTestClient测试增加了CSRF的支持
```java
this.rest
	// provide a valid CSRF token
	.mutateWith(csrf())
	.post()
	.uri("/login")
	...
```