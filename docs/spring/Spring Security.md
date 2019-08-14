# Authorization授权
Spring Security中的高级授权功能是其受欢迎的最引人注目的原因之一。不管您选择如何进行身份验证：是否使用Spring Security提供的机制和提供者，还是与容器或其他非spring安全认证工具集成，你会发现授权服务的使用方法是一致的，而且使用简单。

在本部分中，我们将探讨不同的AbstractSecurityInterceptor实现，这是在第一部分中介绍的。然后我们继续探索如何通过使用域访问控制列表来调整授权。
## 25 Authorization Architecture授权架构
### 