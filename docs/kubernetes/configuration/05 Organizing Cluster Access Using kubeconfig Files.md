# Organizing Cluster Access Using kubeconfig Files

使用kubeconfig文件来组织有关集群、用户、命名空间和身份验证机制的信息。kubectl命令行工具使用kubeconfig文件查找选择集群和与集群的API服务器通信所需的信息。

> :notes: 注意：用于配置对集群的访问的文件称为kubeconfig文件。这是引用配置文件的通用方法。并不是说这个文件的文件名是kubeconfig。

默认情况下，kubectl在`$HOME/.kube`目录中查找名为config的文件。可以通过设置`KUBECONFIG`环境变量或设置`--kubeconfig`标志来指定其他kubeconfig文件。

有关创建和指定kubeconfig文件的分步说明，请参阅[配置对多个群集的访问](https://kubernetes.io/docs/tasks/access-application-cluster/configure-access-multiple-clusters)。

## Supporting multiple clusters, users, and authentication mechanisms 

假设您有几个集群，并且您的用户和组件以各种方式进行身份验证。例如：
* 正在运行的kubelet可能使用证书进行身份验证。
* 用户可以使用令牌进行身份验证。
* 管理员可能有他们提供给单个用户的证书集。

使用kubeconfig文件，您可以组织集群、用户和命名空间。您还可以定义上下文，以便在集群和命名空间之间快速方便地切换

## Context

kubeconfig文件中的`context`元素用于将访问参数按组分到一个便捷的名称下。每个上下文有三个参数：cluster、namespace和user。默认情况下，kubectl命令行工具使用当前上下文中的参数与集群通信。

要选择当前上下文：
```sh
kubectl config use-context
```

## The KUBECONFIG environment variable

`KUBECONFIG`环境变量保存kubeconfig文件的列表。对于Linux和Mac，列表是用冒号分隔的。对于Windows，列表以分号分隔。`KUBECONFIG`环境变量不是必须的。如果`KUBECONFIG`环境变量不存在，kubectl将使用默认的kubeconfig文件，`$HOME/.kube/config`。

如果`KUBECONFIG`环境变量存在，kubectl将使用有效的配置，该配置是合并`KUBECONFIG`环境变量中列出的文件的结果。

## Merging kubeconfig files

要查看配置，请输入以下命令：

```sh
kebectl config view
```

如前所述，输出可能来自单个kubeconfig文件，也可能是合并多个kubeconfig文件的结果。

以下是kubectl在合并kubeconfig文件时使用的规则：
1. 如果设置了--kubeconfig标志，则仅使用指定的文件，不合并。此标志只允许一个实例。
   否则，如果设置了`KUBECONFIG`环境变量，将其作为合并所需的文件列表使用。根据以下列出的文件合并规则合并文件：
   - 忽略空文件名。
   - 无法反序列化内容的文件将产生错误。
   - 第一个设置某特定值或映射键的文件优先。
   - 不会更改值或映射键。示例：保留第一个文件的上下文以设置`current-context`。示例：如果两个文件都指定了`red-user`，则仅使用第一个文件的`red-user`的值。即使第二个文件的`red-user`下没有冲突条目，该配置也会被丢弃。

    有关设置KUBECONFIG环境变量的示例，请参见[设置KUBECONFIG环境变量](https://kubernetes.io/docs/tasks/access-application-cluster/configure-access-multiple-clusters/#set-the-kubeconfig-environment-variable)。

    否则，请使用默认的kubeconfig文件，`$HOME/.kube/config`，不进行合并。

2. 根据此链中的第一次命中确定要使用的上下文：
   1. 如果存在`--context`命令行标志，使用它。
   2. 使用合并的kubeconfig文件中的`current-context`。
   此时允许空上下文。

3. 确定群集和用户。这个时候，可能有一个上下文，也可能没有。根据此链中的第一次命中确定集群和用户，该链将运行两次：一次用于用户，一次用于集群：
   1. 如果命令行标志存在，则使用它：`--user`或`--cluster`。
   2. 如果上下文非空，则从上下文中获取用户或集群。
   此时用户和集群可以为空。

4. 确定要使用的实际群集信息。此时，可能有也可能没有集群信息。基于此链构建每个集群信息, 第一次命中的优先：
   1. 如果命令行标志存在，则使用它们：`--server, --certificate-authority, --insecure-skip-tls-verify`。
   2. 如果合并的kubeconfig文件中存在任何集群信息属性，则使用它们。
   3. 如果没有server location，则失败。

5. 确定要使用的实际用户信息。使用与群集信息相同的规则生成用户信息，但只允许每个用户使用一种身份验证技术：
    1. 如果存在命令行标志，请使用它们：`--client-certificate, --client-key, --username, --password, --token`。
    2. 使用kubeconfig文件合并后的`user`字段
    3. 如果存在冲突，则失败。If there are two conflicting techniques, fail.
6. 如果仍然有信息遗漏，使用默认值并可能提示输入身份验证信息。

## File references

kubeconfig文件中的文件和路径引用相对于kubeconfig文件的位置。命令行上的文件引用相对于当前工作目录。在`$HOME/.kube/config`中，相对路径相对存储，绝对路径绝对存储

## What's next
* [Configure Access to Multiple Clusters](https://kubernetes.io/docs/tasks/access-application-cluster/configure-access-multiple-clusters/)
* [kubectl config](https://kubernetes.io/docs/reference/generated/kubectl/kubectl-commands#config)

