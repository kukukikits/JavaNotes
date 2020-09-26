

# 本文描述所有Controllers

---

# TTL Controller for Finished Resources
*FEATURE STATE: Kubernetes v1.12 [alpha]*

TTL控制器提供了TTL（生存时间time to live）机制来限制已完成执行的资源对象的生存期。TTL控制器目前只处理Job，并且可能会扩展到处理其他执行直至finished的资源，例如pod和自定义资源。

Alpha免责声明：此功能目前是Alpha，可以同时使用kube apiserver和kube controller manager [feature gate](https://kubernetes.io/docs/reference/command-line-tools-reference/feature-gates/) ttlafinished启用


