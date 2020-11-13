# Overview of Cloud Native Security
此概述定义了一个在云原生环境中Kubernetes的安全模型。

:warning: 此容器安全模型只是提供了一些建议，而不是经过验证的信息安全策略。

## The 4C's of Cloud Native security

你可以在不同的分层考虑安全性问题。云原生安全的4C指的是Cloud、Clusters、Containers和Code。

> :notebook: 注意：这种分层方法增强了[防御深度](https://en.wikipedia.org/wiki/Defense_in_depth_(computing))计算方法的安全性，它被广泛认为是保护软件系统的最佳实践。

```
    -----------------------------------------------------------------------------
    |  ----------------------------------------------------------------------    |
    |  |  ------------------------------------------------------------------ |   |
    |  |  |   -----------------------------------------------------------  | |   |
    |  |  |   |                                                         |  | |   |
    |  |  |   |                                                         |  | |   |
    |  |  |   |                                                         |  | |   |
    |  |  |   |                                                         |  | |   |
    |  |  |   |                                                         |  | |   |
    |  |  |   |                         Code                            |  | |   |
    |  |  |   -----------------------------------------------------------  | |   |
    |  |  | Docker                    container                            | |   |
    |  |  -----------------------------------------------------------------  |   |
    |  |  kubernetes                   Cluster                               |   |
    |  -----------------------------------------------------------------------   |
    |  Computers and                 Cloud/Co-Lo/Corporate                       |
    |  Networks                        Datacenter                                |
    -----------------------------------------------------------------------------

```

**The 4C's of Cloud Native Security**

云原生安全模型的每一层都建立在下一个最外层之上。代码层受益于强大的基础（云、集群、容器）安全层。您无法通过在代码级别解决安全性问题来针对底层的低级安全标准进行保护。

## Cloud

在许多方面，云（或同一位置的服务器，或企业数据中心）是Kubernetes集群的可信计算基础。如果云层易受攻击（或以易受攻击的方式配置），则无法保证在此基础上构建的组件是安全的。每个云提供商需要为在其环境中安全运行的工作负载提供安全建议。

### Cloud provider security

如果您在自己的硬件或其他云提供商上运行Kubernetes集群，请参考您的文档以获得安全性最佳实践。以下是一些流行的云提供商安全文档的链接：


IaaS Provider | Link
--------------|-----
Alibaba Cloud | https://www.alibabacloud.com/trust-center 
Amazon Web Services | https://aws.amazon.com/security/ 
Google Cloud Platform | https://cloud.google.com/security/ 
IBM Cloud | https://www.ibm.com/cloud/security 
Microsoft Azure | https://docs.microsoft.com/en-us/azure/security/azure-security 
VMWare VSphere | https://www.vmware.com/security/hardening-guides.html 

### Infrastructure security

在Kubernetes集群中保护基础设施的建议：

1. Network access to API Server (Control plane) 对API服务器（控制平面）的网络访问
   对Kubernetes控制平面的所有访问都不允许在internet上公开，而是由网络访问控制列表控制，这些列表仅限于管理集群所需的一组IP地址 。

2. Network access to Nodes (nodes) 对节点的网络访问（节点）
   节点应配置为仅接受来自指定端口的控制平面control plane的连接（通过网络访问控制列表），并接受类型为NodePort和LoadBalancer的Kubernetes中的服务的连接。如果可能，这些节点不应该完全暴露在公共互联网上。

3. Kubernetes access to Cloud Provider API --  Kubernetes访问云提供商API
   每个云提供商需要向Kubernetes控制平面和节点授予一组不同的权限。最好为集群提供云提供商访问，该访问遵循对其需要管理的资源的[最低权限原则](https://en.wikipedia.org/wiki/Principle_of_least_privilege)。[Kops文档](https://github.com/kubernetes/kops/blob/master/docs/iam_roles.md#iam-roles)提供有关IAM策略和角色的信息。

4. Access to etcd 访问etcd
   访问etcd（Kubernetes的数据存储）应仅限于控制平面。根据您的配置，您应该尝试在TLS上使用etcd。更多信息可在[etcd文档](https://github.com/etcd-io/etcd/tree/master/Documentation)中找到。

5. etcd Encryption
   在任何可能的情况下，加密静态的所有驱动器都是一个很好的做法，但是由于etcd保存着整个集群的状态（包括机密），所以它的磁盘尤其应该在静止时(at rest，是不是说休息的时候)加密。
    
## Cluster
保护Kubernetes有两个值得关注的方面：
- 保护可配置的群集组件
- 保护群集中运行的应用程序

### Components of the Cluster

如果您想保护您的集群免受意外或恶意访问，并采用良好的信息实践，请阅读并遵循有关[保护集群的建议](https://kubernetes.io/docs/tasks/administer-cluster/securing-a-cluster/)。

### Components in the cluster (your application)

根据应用程序的攻击面，您可能需要关注安全性的特定方面。例如：如果您正在运行一个在其他资源链中至关重要的服务（服务a）和易受资源耗尽攻击的独立工作负载（服务B），则如果不限制服务B的资源，则危害服务a的风险很高。下表列出了安全问题和关于确保在Kubernetes中运行的工作负载的建议：


Area of Concern for Workload Security | Recommendation
--------------------------------------|---------------
RBAC Authorization (Access to the Kubernetes API) | https://kubernetes.io/docs/reference/access-authn-authz/rbac/ 
Authentication | https://kubernetes.io/docs/concepts/security/controlling-access/ 
Application secrets management (and encrypting them in etcd at rest) | https://kubernetes.io/docs/concepts/configuration/secret/ https://kubernetes.io/docs/tasks/administer-cluster/encrypt-data/ 
Pod Security Policies | https://kubernetes.io/docs/concepts/policy/pod-security-policy/ 
Quality of Service (and Cluster resource management) | https://kubernetes.io/docs/tasks/configure-pod-container/quality-service-pod/ 
Network Policies | https://kubernetes.io/docs/concepts/services-networking/network-policies/ 
TLS For Kubernetes Ingress | https://kubernetes.io/docs/concepts/services-networking/ingress/#tls 


## Container

容器安全不在本指南的范围之内。以下是探索此主题的一般建议和链接：


Area of Concern for Containers | Recommendation
-------------------------------|---------------
容器漏洞扫描与操作系统依赖安全 | 作为映像构建步骤的一部分，您应该扫描容器中的已知漏洞。
镜像的签名和Enforcement | 签名容器镜像以维护容器内容的可信系统。
禁止特权用户 | 在构造容器时，请查阅文档，了解如何在容器内创建具有实现容器目标所需的最低级别操作系统权限的用户。


## Code

应用程序代码是您最有控制权的主要攻击面之一。虽然保护应用程序代码不在Kubernetes安全主题之内，但下面还是提供了一下保护应用程序代码的建议：

### Code security

1. Access over TLS only
   如果您的代码需要通过TCP通信，请提前与客户机执行TLS握手。除少数情况外，加密传输中的所有内容。更进一步，加密服务之间的网络流量是个好主意。这可以通过称为mutual或[mTLS](https://en.wikipedia.org/wiki/Mutual_authentication)的过程来完成，该过程对两个证书持有服务之间的通信执行双边验证。

2. 限制通信端口范围
   这个建议可能有点不言自明，但只要可能，您应该只公开服务上对通信或metric收集绝对必要的端口。

3. 3rd Party Dependency Security
   定期扫描应用程序的第三方库以了解已知的安全漏洞是一个很好的做法。每个编程语言都有一个工具来自动执行此检查。

4. 静态代码分析
   大多数语言都提供了一种方法来分析代码片段中是否存在任何潜在的不安全的编码。只要可能，您应该使用能够扫描代码库以查找常见安全错误的自动化工具来执行检查。一些工具可以在以下位置找到：https://owasp.org/www-community/Source_Code_Analysis_Tools

5. 动态探测攻击
   有一些自动化工具可以针对您的服务运行，以尝试一些众所周知的服务攻击。其中包括SQL注入、CSRF和XSS。最流行的动态分析工具之一是[OWASP-Zed攻击代理](https://owasp.org/www-project-zap/)工具。


## What's next

Learn about related Kubernetes security topics:
* [Pod security standards](https://kubernetes.io/docs/concepts/security/pod-security-standards/)
* [Network policies for Pods](https://kubernetes.io/docs/concepts/services-networking/network-policies/)
* [Controlling Access to the Kubernetes API](https://kubernetes.io/docs/concepts/security/controlling-access)
* [Securing your cluster](https://kubernetes.io/docs/tasks/administer-cluster/securing-a-cluster/)
* [Data encryption in transit](https://kubernetes.io/docs/tasks/tls/managing-tls-in-a-cluster/) for the control plane
* [Data encryption at rest](https://kubernetes.io/docs/tasks/administer-cluster/encrypt-data/)
* [Secrets in Kubernetes](https://kubernetes.io/docs/concepts/configuration/secret/)






