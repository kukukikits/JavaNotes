# Configuration Best Practices 配置最佳实践

本文档重点介绍并整合了用户指南、入门文档和示例中介绍的配置最佳实践。

这是一份活生生的文件。如果你认为有些东西不在这个清单上，但可能对其他人有用，请不要犹豫，提交一个问题或提交一个PR。


## General Configuration Tips

- 定义配置时，请指定最新的稳定API版本。
- 在将配置文件推送到集群之前，应该将配置文件存储在版本控制中。这允许您在必要时快速回滚配置更改。它也有助于集群的重建和恢复。
- 使用YAML而不是JSON编写配置文件。尽管这些格式在几乎所有场景中都可以互换使用，但YAML更易于用户使用。
- 只要有意义，就可以将和组有关的对象放到一个单独的文件中。一个文件通常比几个文件更容易管理。查看[guestbook-all-in-one.yaml](https://github.com/kubernetes/examples/tree/master/guestbook/all-in-one/guestbook-all-in-one.yaml)文件为此语法的示例。
- 还请注意，可以在目录上调用许多kubectl命令。例如，可以对配置文件的目录调用kubectl apply。
- 不要指定不必要的默认值：简单、最小的配置会降低出错的可能性。
- 在注释中添加对象描述，以便更好地进行内省

## "Naked" Pods versus ReplicaSets, Deployments, and Jobs “裸”Pods与复制集、部署和作业

- 如果可以避免的话，不要使用裸pod（即没有绑定到复制集或部署的pod）。如果节点发生故障，则不会重新安排裸pod。

除了一些显式的[restartPolicy:Never](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#restart-policy)场景外，部署既可以创建一个复制集来确保所需数量的pod始终可用，还可以指定替换pod的策略（例如[RollingUpdate](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#rolling-update-deployment)），所以部署几乎是最适合来创建pod的方式，job可能也比较合适。

## Services
- [Service](https://kubernetes.io/docs/concepts/services-networking/service/)是在相应的后端工作负载（部署或复制集）之前，以及在需要访问它的任何工作负载之前创建的。当Kubernetes启动一个容器时，它会提供指向容器启动后正在运行的所有服务的环境变量。例如，如果存在一个名为foo的服务，则所有容器在其初始环境中都存在以下变量：
  ```
    FOO_SERVICE_HOST=<the host the Service is running on>
    FOO_SERVICE_PORT=<the port the Service is running on>
  ```
  这种环境变量的配置有一个排序要求 —— Pod想要访问的任何服务都必须在Pod本身之前创建，否则环境变量将不会被填充。DNS则没有此限制。

- 一个可选的（尽管强烈建议）群集附加组件是DNS服务器。DNS服务器监视kubernetes api以获得新的服务，并为每个服务创建一组DNS记录。如果在整个集群中启用了DNS，那么所有pod都应该能够自动执行服务的名称解析。
- 除非绝对必要，否则不要为Pod指定`hostPort`。当您将一个Pod绑定到一个hostPort时，它限制了Pod可以调度的位置数，因为每个`<hostIP，hostPort，protocol>`组合必须是唯一的。如果不显式地指定hostIP和protocol，Kubernetes将使用0.0.0.0作为默认hostIP，TCP作为默认协议。
       
    如果您只需要访问端口以进行调试，则可以使用[apiserver代理](https://kubernetes.io/docs/tasks/access-application-cluster/access-cluster/#manually-constructing-apiserver-proxy-urls)或[kubectl端口转发](https://kubernetes.io/docs/tasks/access-application-cluster/port-forward-access-application-cluster/)。

    如果明确需要在节点上公开Pod的端口，请在使用hostPort之前考虑用[NodePort](https://kubernetes.io/docs/concepts/services-networking/service/#nodeport)服务。
- 避免使用hostNetwork，原因与hostPort相同。
- 当您不需要kube-proxy负载平衡时，使用[headless Services](https://kubernetes.io/docs/concepts/services-networking/service/#headless-services)（ClusterIP为None）来发现服务。


## Using Labels 

- 定义并使用标识来区分应用程序或部署的语义属性，例如`{app:myapp，tier:frontend，phase:test，Deployment:v3}`。您可以使用这些标签为其他资源选择适当的pod；例如，选择所有的`tier: frontend`的pods，或者`app: myapp`的所有`phase: test`组件。有关这种方法的示例，请参阅[guestbook](https://github.com/kubernetes/examples/tree/master/guestbook/)应用程序。

通过从选择器中省略特定于版本的标签，可以使服务跨越多个部署。部署让正在运行的服务的更新变得很容易，且不需要停机。

Deployment描述了一个对象的期望的状态，如果更改了规范并应用了，则部署控制器将以受控速率将实际状态更改为所需状态。

- 可以操作标签进行调试。因为Kubernetes控制器（如replicset）和服务使用选择器标签与Pod匹配，所以从Pod中删除相关标签将阻止控制器考虑它，或阻止服务为其提供流量。如果移除现有pod的标签，其控制器将创建一个新的pod来代替它。这是在“隔离”环境中调试以前的“活动”pod的有用方法。要以交互方式删除或添加标签，请使用[kubectl label](https://kubernetes.io/docs/reference/generated/kubectl/kubectl-commands#label)


## Container Images
当[kubelet](https://kubernetes.io/docs/reference/command-line-tools-reference/kubelet/)尝试拉取指定的镜像时，[imagePullPolicy](https://kubernetes.io/docs/concepts/containers/images/#updating-images)和镜像的tag会产生影响。
- `imagePullPolicy: IfNotPresent`：仅当镜像在本地不存在时才拉取。
- `imagePullPolicy: Always`：每次kubelet启动一个容器时，kubelet都会查询容器镜像registry，以将名称解析为镜像摘要。如果kubelet有一个容器镜像，并在本地缓存了该摘要，那么kubelet将使用其缓存的镜像；否则，kubelet将下载（拉取）包含已解析摘要的镜像，并使用该镜像启动容器。
- 省略imagePullPolicy，并且image tag为`:latest`或省略该tag，则使用Always策略。
- 省略imagePullPolicy，image tag存在但是没有`:latest`：应用IfNotPresent。
- `imagePullPolicy: Never`：假设镜像存在于本地。不会尝试提取镜像。

:notebook: 注意：要确保容器始终使用相同版本的镜像，可以指定其摘要；将`<image-name>:<tag>`替换为`<image-name>@<digest>`（例如，`image@sha256:45b23dee08af5e43a7fea6c4cf9c25ccf269ee113168c19722f87876677c5cb2`）。摘要唯一标识镜像的特定版本，因此除非更改摘要值，否则Kubernetes不会更新镜像。

:airplane: 注意：在生产环境中部署容器时，应该避免使用`:latest` tag，因为这会使镜像版本的跟踪变得困难，并且更难正确回滚。

:warning: 注意：底层镜像提供程序的缓存语义使`imagePullPolicy: Always`策略更加有效。以Docker为例，如果镜像已经存在，那么拉取的速度会很快，因为所有的镜像层都被缓存，不需要下载镜像。

## Using kubectl

- 使用`kubectl apply -f <directory>`。该命令搜索`<directory>`文件夹中所有`.yaml`、`.yml`、`.json`格式的kuberntets配置文件，并把他们传递给`apply`。
- 对get和delete操作使用标签选择器，而不是特定的对象名称。请参阅有关[标签选择器](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors)和[有效使用标签](https://kubernetes.io/docs/concepts/cluster-administration/manage-deployment/#using-labels-effectively)的部分。
- 使用`kubectl create deployment`和`kubectl expose`快速创建单个容器部署和服务。有关示例，请参阅[使用服务访问群集中的应用程序](https://kubernetes.io/docs/tasks/access-application-cluster/service-access-application-cluster/)。
