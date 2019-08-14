# 9.5 Access-Control (Authorization) in Spring Security权限控制（授权）
Spring Security中负责进行访问控制的 AccessDecisionManager。它有一个 decide方法，接收一个Authentication 对象，这个对象代表了主体的请求访问，也是一个“secure object” （下面会解释），并且包含了一系列安全元数据属性（例如，要授予访问权限所需要的角色列表）。
## 9.5.1 Security and AOP Advice
如果你熟悉面向切面AOP，那你对这些不同的Advice也比较熟悉：before, after, throws and around。Around advice是非常有用的，因为advisor 可以选择是否继续进行方法调用，是否要修改响应，是否要抛出异常。Spring Security为方法调用和web请求提供了一个around advice。我们使用Spring的标准AOP为方法调用提供了一个around advice，使用标准过滤器为web请求实现了around advice。

对于那些对AOP不熟悉的，要理解的关键一点是，Spring Security可以帮助您保护方法调用和web请求。大多数用户对如何保护服务层方法调用感兴趣，因为大多数的业务逻辑都是在service层实现的。如果您只需要保护服务层的方法调用，那么Spring标准的AOP就足够了。如果您需要直接保护domain objects，您可能会需要考虑下AspectJ。

您可以选择使用AspectJ或Spring AOP来执行方法授权，或者您可以选择使用过滤器执行web请求授权。您可以使用一种、两种或三种结合的方式。主流的使用模式是执行一些web请求授权，并结合在服务层上使用Spring AOP方法调用授权。
## 9.5.2 Secure Objects and the AbstractSecurityInterceptor
那么什么是“安全对象”呢？Spring Security使用这个术语来指代任何可以具有安全性（例如授权决策）的对象。最常见的例子是方法调用和web请求。

每个受支持的安全对象类型都有自己的拦截器类，它是AbstractSecurityInterceptor拦截器的子类。重要的是，当调用AbstractSecurityInterceptor 拦截器时，如果主体已经过身份验证， SecurityContextHolder将包含有效的 Authentication。

AbstractSecurityInterceptor拦截器提供了一个用于处理安全对象请求的一致工作流，通常是：

1. 查找与当前请求相关联的“配置属性”
2. 向 AccessDecisionManager提交安全对象、当前Authentication 和配置属性，以获得授权
3. 可选地改变Authentication 
4. 允许安全对象调用继续进行（假定访问被授予）
5. 如果配置了 AfterInvocationManager ，当调用返回时，就会调用AfterInvocationManager。如果调用引发了异常，那么AfterInvocationManager将不会被调用。

### 配置属性都有哪些？
一个“配置属性”可以认为是对AbstractSecurityInterceptor使用的类具有特殊含义的字符串。它们由框架内的接口ConfigAttribute表示。它们可能是简单的角色名称，或者具有更复杂的含义，这取决于AccessDecisionManager实现的复杂程度。AbstractSecurityInterceptor 拦截器配置了一个SecurityMetadataSource属性，它用来查找安全对象的属性。通常，这种配置对用户不可见。可以通过被保护方法的注解或被保护url上的访问属性，来设置配置属性。例如，当我们在名称空间介绍中看到类似于<intercept-url pattern='/secure/**' access='ROLE_A,ROLE_B'/> 的东西时，这就是说， ROLE_A和 ROLE_B的配置属性就会应用到匹配的web请求。在实践中，使用默认的AccessDecisionManager配置时，这意味着只要用户的授权权限中有这两个配置属性中的任意一个，该用户就被允许访问。严格地说，它们只是属性，具体如何使用跟 AccessDecisionManager的实现相关。前缀 ROLE_是一个标记，用来表明这些属性是角色，并且会被Spring Security的RoleVoter消费，这只有在使用基于 voter的AccessDecisionManager 中才有意义。我们将在[authorization chapter](https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#authz-arch)一章中详细讨论AccessDecisionManager的实现。

### RunAsManager
假设 AccessDecisionManager决定允许请求，AbstractSecurityInterceptor 通常会继续请求。话虽如此，在极少数情况下，用户可能希望用不同的Authentication来替换安全上下文中的Authentication，这是由AccessDecisionManager 调用RunAsManager来实现的。这可能在相当不寻常的情况下有用。比如，服务层方法需要调用远程系统并且使用不同的标识。因为Spring Security自动将安全标识从一台服务器传播到另一台服务器（假设您使用的是配置适当的RMI或HttpInvoker远程协议客户端），这个方法可能会有用。
### AfterinvocationManager

在安全对象调用过程之后，返回AbstractSecurityInterceptor 拦截器，这时可能意味着方法调用正在完成或过滤器链会继续执行，而此时也是处理调用的最后一次机会。在这个阶段，AbstractSecurityInterceptor 拦截器可能会修改返回对象。我们可能希望这种情况发生，因为授权决策不能“在进入”到安全对象调用的过程中进行（We might want this to happen because an authorization decision couldn’t be made “on the way in” to a secure object invocation. ）。作为高度可插拔的，AbstractSecurityInterceptor拦截器将把控制权交给给AfterInvocationManager ，以便在实际需要时修改对象。这个类甚至可以完全替换安全对象，或者抛出异常，或者不按它所选择的方式改变它。只有在调用成功时才会执行后续调用检查。如果发生异常，则会跳过额外的检查。

AbstractSecurityInterceptor 和它相关的对象详见[Security interceptors and the “secure object” model](https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/html5/#abstract-security-interceptor)

Figure 9.1. Security interceptors and the "secure object" model
![Figure 9.1. Security interceptors and the "secure object" model](https://docs.spring.io/spring-security/site/docs/5.0.7.RELEASE/reference/htmlsingle/images/security-interception.png "Security interceptors and the \"secure object\" model")

### 扩展安全对象模型
只有开发人员考虑一种全新的拦截和授权请求的方式，才需要直接使用安全对象。例如，有可能构建一个新的安全对象来保护对消息传递系统的调用。任何需要安全性的，并且提供拦截调用方法（比如AOP的围绕通知语义）的东西，都能被看做是一个安全的对象。话虽如此，大多数Spring应用程序都只需简单地使用当前支持的三种安全对象类型（AOP Alliance  MethodInvocation、AspectJ JoinPoint 和web请求 FilterInvocation），并且完全透明。

