# Authorization授权
Spring Security中的高级授权功能是其受欢迎的最引人注目的原因之一。不管您选择如何进行身份验证：是否使用Spring Security提供的机制和提供者，还是与容器或其他非spring安全认证工具集成，你会发现授权服务的使用方法是一致的，而且使用简单。

在本部分中，我们将探讨不同的AbstractSecurityInterceptor实现，这是在第一部分中介绍的。然后我们继续探索如何通过使用域访问控制列表来调整授权。
# 25 Authorization Architecture授权架构
## 25.1 Autorities权限
正如我们在技术概述中看到的，所有Authentication的实现都存储了一个GrantedAuthority对象列表。这个列表代表了授权给主体的所有权限。
GrantedAuthority对象会被AuthenticationManager插入到Authentication对象里面，之后当需要做授权决定的时候会被AccessDecisionManager 读取。

GrantedAuthority接口只有一个方法：
```java
String getAuthority();
```
这种方法允许AccessDecisionManager获得一个代表GrantedAuthority的精确的字符串。使用字符串来代表 GrantedAuthority对象，
可以使大多数 AccessDecisionManager更容易阅读。如果一个GrantedAuthority对象不能精确地表示为一个字符串的话，GrantedAuthority
对象就会被认为是复杂的，getAuthority()方法就必须返回null。

存储一系列操作和权限限制，并用于不同客户帐户号码，这样的GrantedAuthority对象会被认为是复杂的。将这种GrantedAuthority对象转换
成字符串是非常复杂的，所以getAuthority()方法必须返回null。这表明，为了理解getAuthority()返回的内容，所有AccessDecisionManager
都需要专门支持GrantedAuthority实现。

Spring Security有一个具体的GrantedAuthority实现—— SimpleGrantedAuthority。它可以把任何用户指定的字符串转换为GrantedAuthority
对象。Spring Security框架中的所有AuthenticationProvider对象都使用SimpleGrantedAuthority来生成Authenticationd对象。

## 25.2 Pre-Invocation Handling预调用处理
同<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#secure-objects">Technical Overview</a> 一章描述的那样，Spring Security提供了拦截器来控制安全对象的访问，比如invocations方法和web请求。
pre-invocation决定是否允许调用执行，它是由AccessDecisionManager创建的。

### 25.2.1 The AccessDecisionManager
AccessDecisionManager会被AbstractSecurityInterceptor调用，用来做最终的访问控制决定。AccessDecisionManager接口包含三个方法：
```java
void decide(Authentication authentication, Object secureObject,
	Collection<ConfigAttribute> attrs) throws AccessDeniedException;

boolean supports(ConfigAttribute attribute);

boolean supports(Class clazz);
```
AccessDecisionManager的decide方法接收它所需要的所有相关信息并做出授权决策。特别的，传入secureObject来检查实际的
secure object invocation里的参数。例如，假设secure object是一个MethodInvocation方法调用，查询任何自定义参数的MethodInvocation
都很容易，然后在 AccessDecisionManager中实现某种安全逻辑，以确保允许主体对自定义参数进行操作。如果访问被拒绝，要求抛出
AccessDeniedException 异常。

supports(ConfigAttribute) 方法在启动阶段被AbstractSecurityInterceptor 调用，用来决定AccessDecisionManager是否能处理传递的
ConfigAttribute。supports(Class) 方法由一个安全拦截器的实现类调用，用来确保配置的AccessDecisionManager可以支持响应的
secure object类型。

### 25.2.2 Voting-Based AccessDecisionManager Implementations 基于投票的访问决策管理实现
用户可以实现自己的 AccessDecisionManager 来控制授权的所有内容，Spring Security包括几个基于投票的AccessDecisionManager 的实现。
下图说明了相关的类。

<img src='https://docs.spring.io/spring-security/site/docs/5.2.0.BUILD-SNAPSHOT/reference/htmlsingle/images/access-decision-voting.png'/>

使用这种方法，在做授权决策时，会轮询一系列的AccessDecisionVoter 的实现。然后 AccessDecisionManager根据对投票的评估结果来决定
是否要抛出AccessDeniedException异常。

AccessDecisionVoter 接口有三个方法：

```java
int vote(Authentication authentication, Object object, Collection<ConfigAttribute> attrs);

boolean supports(ConfigAttribute attribute);

boolean supports(Class clazz);
```

vote方法返回int，int的值是 AccessDecisionVoter静态字段ACCESS_ABSTAIN, ACCESS_DENIED 或ACCESS_GRANTED的值。如果该接口的实现
类无法做出授权决定时，需要返回ACCESS_ABSTAIN 访问放弃。如果能够做出决定，必须返回ACCESS_DENIED 和ACCESS_GRANTED中的一个。

Spring Security提供了三个具体的 AccessDecisionManager来统计票数。 ConsensusBased基于少数服从多数的实现会根据非弃权票的共识来
授予或拒绝访问。在票数相等或全部弃权的情况下，会使用Properties（估计说的是ConsensusBased的属性，具体什么属性就不知道了）来控制
实现的行为。 AffirmativeBased基于赞成的实现在接收到一个或多个ACCESS_GRANTED选票时，会授予访问权限（也就是说反对票会被忽略，只
需要至少一张赞成选票就同意授权）。和 ConsensusBased实现一样，同样有一个参数用来处理全部弃权的情况。UnanimousBased全体一致的实
现要求全体一致投 ACCESS_GRANTED才能授权访问，忽略弃权的。如果有任何一张 ACCESS_DENIED 票，访问都会被拒绝。和其他类型的实现一
样，同样有个参数来控制如何处理全部弃权的情况。

也可以自定义AccessDecisionManager 来实现不同的投票机制。例如，来自特殊的AccessDecisionVoter的票有更高的权重，而来自特定选民的
否决票可能会有否决权。

#### RoleVoter角色投票者
Spring Security提供的最常用的 AccessDecisionVoter实现是 RoleVoter，它的配置属性是简单的角色名，并在用户被分配该角色的情况下投赞成票。

如果 ConfigAttribute 以前缀 ROLE_开始，它就会投票。如果GrantedAuthority的 getAuthority() 方法返回的字符串完全等于一个或多个
以ROLE_为前缀的 ConfigAttribute的时候，RoleVoter就会投赞成票。如果匹配不到任何以ROLE_为前缀的ConfigAttribute，RoleVoter就会
投反对票。如果没有以ROLE_打头的ConfigAttribute，那么RoleVoter就会弃权。

#### AuthenticatedVoter经过身份验证的投票者

AuthenticatedVoter这个我们其实已经预见过了，它可以用来区分匿名用户、 完整验证和 remember-me验证的用户（anonymous, 
fully-authenticated and remember-me authenticated users）。许多网站对remember-me验证的用户有一些特殊的访问限制，如果要获得
完全访问权限，就要求用户通过登录来确认他们的身份。

我们之前用来给匿名用户授权时，使用的IS_AUTHENTICATED_ANONYMOUSLY属性就是被AuthenticatedVoter处理的。请查看该类的Javadoc获取
更多信息。

#### Custom Voters 自定义投票者

显然，你也可以实现自己的AccessDecisionVoter，把你自己想要的任何访问-控制逻辑放到里面实现。自定义的AccessDecisionVoter可以是
应用程序相关的（即业务逻辑相关），也可以用来实现一些安全管理逻辑。例如，Spring的网站上就有一篇<a href="https://spring.io/blog/2009/01/03/spring-security-customization-part-2-adjusting-secured-session-in-real-time">博客</a>
，描述了如何使用voter来拒绝
那些账户被暂停的用户的实时访问。

## 25.3 After Invocation Handling 调用后处理
在继续处理安全对象调用（secure object invocation）前，AbstractSecurityInterceptor调用AccessDecisionManager的时候，一些程序
需要修改安全对象调用返回的实际对象。虽然你可以通过自己写AOP来实现，但是Spring Securiy已经提供了很多方便的，已经集成了ACL功能的
具体实现。

下图展示了Spring Security的AfterInvocationManager和它的具体实现：

<img src='https://docs.spring.io/spring-security/site/docs/5.2.0.BUILD-SNAPSHOT/reference/htmlsingle/images/after-invocation.png' />

和很多其他的Spring security组件一样，AfterInvocationManager只有一个具体的实现，即 AfterInvocationProviderManager，它会轮询
一个AfterInvocationProvider 的列表。AfterInvocationProvider可以修改secure object调用返回的对象，或者抛出AccessDeniedException
异常。由于前一个provider的修改结果可以传递给provider列表中的下一个provider，所以可以使用多个provider来修改安全对象调用返回的
实际对象。

需要注意，如果你使用了AfterInvocationManager，为了使MethodSecurityInterceptor的AccessDecisionManager同意/允许某个操作，你任
然需要配置属性（说的应该是接口方法supports(ConfigAttribute attribute)中配置属性）。 如果您使用的是典型的Spring Security，包
括AccessDecisionManager 实现，如果没有给安全方法调用定义配置属性，那么对应的AccessDecisionVoter就会放弃投票。反过来，如果
AccessDecisionManager的属性“allowIfAllAbstainDecisions”是false（也就是在全部弃权的时候禁止），那么就会抛出 
AccessDeniedException异常。有两个方法可以避免这些潜在的问题：1.设置“allowIfAllAbstainDecisions”属性为true（不推荐）或者2.确
保至少有一个configuration属性可以让AccessDecisionVoter投票。推荐使用第二种方法，通常通过配置ROLE_USER 或者 ROLE_AUTHENTICATED
 来实现。
 
## 25.4  Hierarchical Roles分级角色

通常程序中的某个特定角色会自动拥有其他的角色。例如，程序中如果有“admin”和“user”时，你可能想让“admin”用户干所有“user”用户能干
的事情。为了实现这一点，你可以把所有的admin用户也指定为“user”角色。另一种方法是，把那些需要“user”角色的访问限制修改为使用“admin”
角色也可以访问。如果程序里的角色太多，第二种方法可能会变得很复杂。

角色分级可以配置一个角色包含另一个角色。RoleHierarchyVoter是Spring Security  RoleVoter的扩展版本，它配置了一个RoleHierarchy
（角色层次关系），从这个角色层次关系中RoleHierarchyVoter可以获取所有分配给用户的“reachable authorities”（可用的权限）。典型的
配置如下：
```xml
<bean id="roleVoter" class="org.springframework.security.access.vote.RoleHierarchyVoter">
	<constructor-arg ref="roleHierarchy" />
</bean>
<bean id="roleHierarchy"
		class="org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl">
	<property name="hierarchy">
		<value>
			ROLE_ADMIN > ROLE_STAFF
			ROLE_STAFF > ROLE_USER
			ROLE_USER > ROLE_GUEST
		</value>
	</property>
</bean>
```
上面的四个角色的层级关系如下：ROLE_ADMIN ⇒ ROLE_STAFF ⇒ ROLE_USER ⇒ ROLE_GUEST。如果 AccessDecisionManager使用了上面配置的
 RoleHierarchyVoter，检查安全限制的时候，使用ROLE_ADMIN权限登录的用户会被认为同时拥有这4种角色。 > 符号可以认为是“包含”的意思。

角色层次结构提供了一种方便的方法来简化应用程序的访问控制配置数据，简化您需要分配给用户的权限。对于更复杂的需求，您需要在特定的
访问权限和用户角色之间定义一个逻辑映射，然后在加载用户信息的时候进行转换。

# 26 Secure Object Implementations安全对象的实现
## 26.1 AOP Alliance(MethodInvocation) Security Interceptor
在Spring Security2.0之前，保护MethodInvocation需要很多配置。现在推荐使用命名空间配置来保护方法的安全，这样的话有关方法安全的
一些基础的bean就会自动为您配置，所以你也就不需要知道具体的实现类了。我们将简要地介绍一下这里涉及的类。

方法安全性强制使用MethodSecurityInterceptor来保护 MethodInvocation（方法调用）。根据配置的不同，该拦截器可能只针对单个bean或
在多个bean之间共享。拦截器使用一个MethodSecurityMetadataSource实例来获得应用于特定方法调用的配置属性。
MapBasedMethodSecurityMetadataSource 用来储存以方法名（可以使用通配符）为key值的配置属性。当应用程序使用<intercept-methods>
或<protect-point> 元素在程序上下文中定义了配置属性时，MapBasedMethodSecurityMetadataSource就会被内部调用。其他的拦截器的实现
用来处理基于注释的配置。

### 26.1.1 Explicit MethodSecurityInterceptor Configuration显示配置方法安全拦截器
当然，您可以在您的应用程序上下文中直接配置一个MethodSecurityIterceptor，以便与Spring AOP的代理机制一起使用：
```xml
<bean id="bankManagerSecurity" class=
	"org.springframework.security.access.intercept.aopalliance.MethodSecurityInterceptor">
<property name="authenticationManager" ref="authenticationManager"/>
<property name="accessDecisionManager" ref="accessDecisionManager"/>
<property name="afterInvocationManager" ref="afterInvocationManager"/>
<property name="securityMetadataSource">
	<sec:method-security-metadata-source>
	<sec:protect method="com.mycompany.BankManager.delete*" access="ROLE_SUPERVISOR"/>
	<sec:protect method="com.mycompany.BankManager.getBalance" access="ROLE_TELLER,ROLE_SUPERVISOR"/>
	</sec:method-security-metadata-source>
</property>
</bean>
```

## 26.2 AspectJ(JoinPoint) Security Interceptor
AspectJ安全拦截器和上一节的AOP Alliance拦截器很相似。所以这一节只介绍一下两者的不同

AspectJ拦截器的实现是AspectJSecurityInterceptor。AOP Alliance安全拦截器依赖Spring的程序上下文，通过代理来使用，
AspectJSecurityInterceptor是通过AspectJ编译器来使用的。在同一个应用程序中使用这两种类型的安全拦截器并不少见，通常使用
AspectJSecurityInterceptor用来保护domain object实例的安全性，使用AOP Alliance MethodSecurityInterceptor来保护service层的安
全性。

首先来看一下在Spring程序里怎么配置AspectJSecurityInterceptor：
```xml
<bean id="bankManagerSecurity" class=
	"org.springframework.security.access.intercept.aspectj.AspectJMethodSecurityInterceptor">
<property name="authenticationManager" ref="authenticationManager"/>
<property name="accessDecisionManager" ref="accessDecisionManager"/>
<property name="afterInvocationManager" ref="afterInvocationManager"/>
<property name="securityMetadataSource">
	<sec:method-security-metadata-source>
	<sec:protect method="com.mycompany.BankManager.delete*" access="ROLE_SUPERVISOR"/>
	<sec:protect method="com.mycompany.BankManager.getBalance" access="ROLE_TELLER,ROLE_SUPERVISOR"/>
	</sec:method-security-metadata-source>
</property>
</bean>
```
正如你所看到的，除了类名外，其他的配置和 AOP Alliance安全拦截器的一模一样。实际上，这两个拦截器可以共享同一个
securityMetadataSource，因为securityMetadataSource与java.lang.reflect.Method一起工作，而不是AOP库中特定类。当然，您的访问决
策可以访问相关的AOP调用（例如MethodInvocation 或JoinPoint），所以在做出访问决定（例如方法参数）时你可以充分考虑一系列条件。

下一步，需要定义AspectJ aspect。例如：

```java
package org.springframework.security.samples.aspectj;

import org.springframework.security.access.intercept.aspectj.AspectJSecurityInterceptor;
import org.springframework.security.access.intercept.aspectj.AspectJCallback;
import org.springframework.beans.factory.InitializingBean;

public aspect DomainObjectInstanceSecurityAspect implements InitializingBean {

	private AspectJSecurityInterceptor securityInterceptor;

	pointcut domainObjectInstanceExecution(): target(PersistableEntity)
		&& execution(public * *(..)) && !within(DomainObjectInstanceSecurityAspect);

	Object around(): domainObjectInstanceExecution() {
		if (this.securityInterceptor == null) {
			return proceed();
		}

		AspectJCallback callback = new AspectJCallback() {
			public Object proceedWithObject() {
				return proceed();
			}
		};

		return this.securityInterceptor.invoke(thisJoinPoint, callback);
	}

	public AspectJSecurityInterceptor getSecurityInterceptor() {
		return securityInterceptor;
	}

	public void setSecurityInterceptor(AspectJSecurityInterceptor securityInterceptor) {
		this.securityInterceptor = securityInterceptor;
	}

	public void afterPropertiesSet() throws Exception {
		if (this.securityInterceptor == null)
			throw new IllegalArgumentException("securityInterceptor required");
		}
	}
}
```

在上面的例子中，安全拦截器 security interceptor会应用到所有的 PersistableEntity实例上，PersistableEntity是一个抽象类
（您可以使用任何其他类或切入点表达式）。需要AspectJCallback，是因为proceed();语句只有在一个 around()中有特殊的含义。
当AspectJSecurityInterceptor 想要目标对象继续时，AspectJSecurityInterceptor 就会调用这个匿名的 AspectJCallback类。

此外，您需要配置Spring来加载aspect并将其与AspectJSecurityInterceptor连接起来。配置如下：
```xml
<bean id="domainObjectInstanceSecurityAspect"
	class="security.samples.aspectj.DomainObjectInstanceSecurityAspect"
	factory-method="aspectOf">
<property name="securityInterceptor" ref="bankManagerSecurity"/>
</bean>
```
就这些了，现在你可以在程序的任何地方，使用任何你认为合适的方法（如new Person()）来创建bean了，并且安全拦截器会在这些bean上应用。

# 27.Expression-Based Access Control基于表达式的访问控制
除了使用configuration attributes和access-decision投票外，Spring Security 3.0 还提供了使用Spring EL表达式来授权的机制。基于
表达式的访问控制建立在相同的架构上，但是允许将复杂的布尔逻辑封装在一个表达式中。

## 27.1 简介
Spring Security的表达式由Spring EL语言支持，如果你想更深入地理解这个主题，你应该看看它是如何工作的。表达式是用“root object”作
为求值上下文的一部分来进行计算的。为了提供内置的表达式，以及访问诸如当前主体这类的值，Spring Security使用特殊的类作为root object。

### 27.1.1 Common Built-In Expressions常用的内置表达式
表达式根对象的基类是 SecurityExpressionRoot。它提供了一些常用的表达式，可以在web和方法安全上使用。

|Expression |	Description|
| :---|:---|
|hasRole([role])|如果当前主体拥有指定的role，返回true。如果指定的role不是以’ROLE_’为前缀的，那么默认会添加’ROLE_’前缀 。可以通过修改 DefaultWebSecurityExpressionHandler的the defaultRolePrefix值来修改这个默认行为。|
|hasAnyRole([role1,role2])|如果当前主体拥有任意一个指定的role，返回true(使用逗号分隔多个role)。 如果指定的role不是以’ROLE_’为前缀的，那么默认会添加’ROLE_’前缀 。可以通过修改 DefaultWebSecurityExpressionHandler的the defaultRolePrefix值来修改这个默认行为。
|hasAuthority([authority])|如果当前主体拥有指定的权限，返回true
|hasAnyAuthority([authority1,authority2])|如果当前主体拥有任意一个指定的权限，返回true(使用逗号分隔分隔多个权限)
|principal|允许直接访问当前用户主体
|authentication|允许直接访问从SecurityContext获得的当前Authentication对象
|permitAll|取值总是为true
|denyAll|取值总是为false
|isAnonymous()|如果当前主体是匿名用户，返回true
|isRememberMe()|如果当前主体是remember-me用户，返回true
|isAuthenticated()|如果用户不是匿名的，返回true
|isFullyAuthenticated()|如果用户既不是匿名的，也不是remember-me，返回true
|hasPermission(Object target, Object permission)|如果用户可以使用指定的permission访问指定的target对象，返回true。例如：hasPermission(domainObject, 'read')
|hasPermission(Object targetId, String targetType, Object permission)|如果用户可以使用指定的permission访问指定的target对象，返回true。例如：hasPermission(1, 'com.example.domain.Message', 'read')

## 27.2. Web Security Expressions web安全表达式
要使用表达式来保护URL，首先要把<http>元素的use-expressions属性设置为ture。然后Spring Security才会使用<intercept-url>元素的
access属性包含的Spring EL表达式。表达式的值应该是Boolean，用来表示允许访问还是拒绝访问。例如：
```xml
<http>
	<intercept-url pattern="/admin*"
		access="hasRole('admin') and hasIpAddress('192.168.1.0/24')"/>
	...
</http>
```
这里我们定义了一个属于“admin”的权限范围，只有授权了“admin”权限的用户，并且该用户的IP地址匹配这个本地子网（192.168.1.0/24）时，
才能访问/admin*的URL。前面一节已经介绍了内置的hasRole表达式。而hasIpAddress表达式是web安全特有的，它是由
WebSecurityExpressionRoot类定义的，当计算web-access表达式的时候，使用WebSecurityExpressionRoot类的实例作为表达式的根对象。
该实例对象还把HttpServletRequest对象暴露给了request，所以你可以直接在表达式里使用request。如果使用了表达式，那么会把一个
WebExpressionVoter添加到命名空间所使用的AccessDecisionManager中。如果你没有使用命名空间，想使用表达式就需要自己添加一个
WebExpressionVoter到配置里。

### 27.2.1 Referring to Beans in Web Security Expressions 在web安全表达式中引用bean

如果想扩展已有的表达式，可以在表达式中引用任何你已经公开的bean。例如，假设有一个名为 webSecurity的bean，并且这个bean有如下的方法：
```java
public class WebSecurity {
    public boolean check(Authentication authentication, HttpServletRequest request) {
            ...
    }
}
```
你可以在表达式中引用这个方法：
```xml
<http>
	<intercept-url pattern="/user/**"
		access="@webSecurity.check(authentication,request)"/>
	...
</http>
```
或者在Java配置中引用：
```java
http
    .authorizeRequests()
    .antMatchers("/user/**").access("@webSecurity.check(authentication,request)")
    ...
```
### 27.2.2 Path Variables in Web Security Expressions web安全表达式中的路径参数
有时，能够在表达式中引用URL中的路径参数是一件非常棒的事情。例如，一个RESTfull的应用使用/user/{userId}的URL路径来查询用户，
其中用户用userId来指定。

在表达式中你可以很容易地引用路径参数。例如，假设有一个名为 webSecurity的bean，并且这个bean有如下的方法：
```java
public class WebSecurity {
    public boolean checkUserId(Authentication authentication, int id) {
            ...
    }
}
```

你可以在表达式中引用这个方法：
```xml
<http>
	<intercept-url pattern="/user/{userId}/**"
		access="@webSecurity.checkUserId(authentication,#userId)"/>
	...
</http>
```
或者在Java配置中引用：
```java
http
    .authorizeRequests()
    .antMatchers("/user/{userId}/**").access("@webSecurity.checkUserId(authentication,#userId)")
    ...
```
在上面的两种配置中，匹配的URL会把路径参数转换并传入checkUserId方法。例如，如果URL是 /user/123/resource，那么传入的路径参数就是123。

## 27.3. Method Security Expressions 方法安全表达式
方法安全表达式比简单的允许/拒绝规则要复杂一点。Spring Security 3.0引入了一些新的注释来全面支持方法安全表达式。
### 27.3.1 @Pre和@Post注释
有四种支持表达式作为属性值的注释，用来做授权检查前处理和后处理，同时支持过滤提交的参数集合，以及返回值。这四个注释分别是 
@PreAuthorize, @PreFilter, @PostAuthorizeand @PostFilter。要使用他们，需要使用命名空间 global-method-security配置来开启：
```xml
<global-method-security pre-post-annotations="enabled"/>
```
#### Access Control using @PreAuthorize and @PostAuthorize  使用@PreAuthorize and 和PostAuthorize做访问控制
最有用的一个注释是 @PreAuthorize，它决定了方法能否被真实调用。例如（“Contacts”示例程序中的一个例子）：
```java
@PreAuthorize("hasRole('USER')")
public void create(Contact contact);
```
上面的代码只允许拥有”ROLE_USER”角色的用户访问。很显然，使用传统的配置和所需角色的简单配置属性就可以很容易地实现与上面代码相同的
功能。但是如果是下面的代码呢：
```java
@PreAuthorize("hasPermission(#contact, 'admin')")
public void deletePermission(Contact contact, Sid recipient, Permission permission);
```
上面的代码中，使用了方法参数contact作为表达式的一部分，可以用来判断当前用户是否拥有所给contact的“admin”访问权限。内置的
hasPermission（）表达式通过应用程序上下文链接到Spring Security ACL模块，<a href="https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#el-permission-evaluator">如下所示</a>。
你可以使用参数名作为表达式变量来访问任何方法
参数。

Spring Security中有很多方法可以解析方法参数。Spring Security使用DefaultSecurityParameterNameDiscoverer来找参数名。默认情况
下，会尝试下面所有的方法：
- 如果方法的单个参数前定义了Spring Security的@P注解，那么表达式中使用注解的值。对于编译版本是JDK8以前的接口，这个注解非常有用，
因为JDK8之前不提供方法参数信息。举个例子：
    ```java
    import org.springframework.security.access.method.P;
    
    ...
    
    @PreAuthorize("#c.name == authentication.name")
    public void doSomething(@P("c") Contact contact);
    ```
    这种注解功能是使用 AnnotationParameterNameDiscoverer实现的，可以通过自定义这个类来支持任意的注解形式。
- 如果有至少一个方法参数使用了Spring Data的@Param注解，那么表达式中使用该注解的值。由于JDK8之前不提供方法参数信息，所以对于编
译版本是JDK8以前的接口，这个注解非常管用。举个例子：
    ```java
    import org.springframework.data.repository.query.Param;
    
    ...
    
    @PreAuthorize("#n == authentication.name")
    Contact findContactByName(@Param("n") String name);
    ```
    这种注解功能是使用 AnnotationParameterNameDiscoverer实现的，可以通过自定义这个类来支持任意的注解形式。
- 如果代码是使用JDK8加-parameters参数编译的，并且使用的是Spring 4+，那么使用标准的JDK反射API来寻找参数名。这在类和接口上都适用。
- 最后，如果代码是使用debug symbols编译的，那么会使用debug symbols来搜索参数名。由于接口的调试信息中不包含参数名，所以不支持接
口。如果要支持接口，那么必须使用注释或JDK8以上。

表达式支持所有的Spring-EL功能，所以可以在表达式中访问参数的属性。例如，如果你想让某个方法只允许和contact拥有相同name属性的用户
访问，你可以这些写：
```java
@PreAuthorize("#contact.name == authentication.name")
public void doSomething(Contact contact);
```
这里我们使用了另一个内置的表达式—— authentication，它是保存在安全上下文中的 Authentication 对象。你也可以直接在表达式中使用
 principal来获取Authentication对象的”principal” 属性。principal其实是一个UserDetails的实例，所以你可以在表达式中这样使用： 
 principal.username 或者 principal.enabled。

有些情况下，你可能想在方法调用后进行访问控制检查，这个时候可以使用@PostAuthorize注解。如果要在表达式中使用方法的返回值，可以使
用内置的 returnObject。

#### Filtering using @PreFilter and @PostFilter 使用@PreFilter 和 @PostFilter过滤
Spring Security支持集合和数组的过滤，现在也可以在表达式中进行过滤。通常情况下会过滤方法的返回值。例如：
```java
@PreAuthorize("hasRole('USER')")
@PostFilter("hasPermission(filterObject, 'read') or hasPermission(filterObject, 'admin')")
public List<Contact> getAll();
```
使用 @PostFilter注解时，Spring Security会遍历方法返回的集合，并且当过滤表达式为false时把集合元素从集合中移除。filterObject
代表遍历的当前集合元素。你也可以使用@PreFilter在方法调用前进行过滤，虽然这并不常见。@PreFilter的语法和@PostFilter的差不多，
但是如果方法参数有多个集合类型的参数时，需要使用@PreFilter注解的filterTarget属性来指定过滤的对象。

过滤处理显然不是用来做数据检索的。如果你过滤一个很大的集合，要移除很多的集合元素，那么会非常影响性能。

### 27.3.2 Built-In Expressions内置的表达式
方法安全有很多专门的内置表达式，一些已经在上面看到过了。像filterTarget 和returnValue都比较简单，而 hasPermission()表达式就需
要详细介绍一下了。
#### The PermissionEvaluator interface 权限求值器接口
hasPermission()表达式委托给了 PermissionEvaluator的实例。该实例用来连接表达式系统和Spring Security的ACL系统，允许你基于抽象
权限指定域对象上的授权约束。它没有对ACL模块的显式依赖，所以如果需要的话，您可以将其替换为另一种实现。PermissionEvaluator接口
有两个方法：
```java
boolean hasPermission(Authentication authentication, Object targetDomainObject,
							Object permission);

boolean hasPermission(Authentication authentication, Serializable targetId,
							String targetType, Object permission);
```
如果指定了第一个参数（Authentication对象），那么这两个方法可以直接映射到表达式的可用版本。当需要控制已知的domain object的访问
时，使用第一个接口方法。如果用户拥有指定对象的访问权限，那么返回true。当没有直接提供domain object，而只知道对象的ID时使用接口
的第二个方法。同时需要为domain object指定抽象的”type”说明，这样才能让系统加载正确的ACL权限。其实这个“type”字符串指的就是
domain object的java类名，但是只要它和permission的加载方式是一致的，那么也就不需要让它等于domain object的java类名了
（翻译的对吗？完整的原文如下：
which map directly to the available versions of the expression, with the exception that the first argument (the
 Authentication object) is not supplied. The first is used in situations where the domain object, to which access is
  being controlled, is already loaded. Then expression will return true if the current user has the given permission 
  for that object. The second version is used in cases where the object is not loaded, but its identifier is known. 
  An abstract “type” specifier for the domain object is also required, allowing the correct ACL permissions to be loaded.
   This has traditionally been the Java class of the object, but does not have to be as long as it is consistent with
    how the permissions are loaded.

）
要使用 hasPermission()表达式，需要在应用程序上下文显式配置PermissionEvaluator。就像这个样子：
```xml
<security:global-method-security pre-post-annotations="enabled">
<security:expression-handler ref="expressionHandler"/>
</security:global-method-security>

<bean id="expressionHandler" class=
"org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler">
	<property name="permissionEvaluator" ref="myPermissionEvaluator"/>
</bean>
```

上面的 myPermissionEvaluator是一个实现了PermissionEvaluator的bean。通常情况下PermissionEvaluator是由ACL模块实现的，叫
AclPermissionEvaluator。：有关更多细节，请参阅“Contacts”示例应用程序配置。
#### Method Security Meta Annotations 方法安全Meta注解
你可以在方法安全中使用meta注解，来增强代码的可读性。当你发现一些复杂的表达式经常在代码中重复时，使用meta注解会非常方便。例如：
```java
@PreAuthorize("#contact.name == authentication.name")
```
为了不用重复写上面的代码，我们可以创建一个meta注解来使用：
```java
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("#contact.name == authentication.name")
public @interface ContactPermission {}
```
元注释可以用于任何Spring Security方法安全注解。为了保持与规范的兼容，JSR-250注解不支持元注释。





