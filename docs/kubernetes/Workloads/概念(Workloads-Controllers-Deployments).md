

# 本文描述所有Controllers

---

# Deployments
Deployment为Pods ReplicaSets提供了声明式的更新

在Deployment中描述一个期望的状态，然后Deployment控制器将实际状态更改为所需状态，并以受控速率进行更改。您可以定义Deployment来创建新的ReplicaSets，或删除现有Deployments并在新的deployment中采用其所有资源。

:warning: 注意：不要管理Deployment拥有的ReplicaSets。如果下面没有介绍您遇到的问题，请考虑在Kubernetes仓库中打开一个问题。

## Use Case

以下是Deployment的典型用例：
- [创建一个Deployment来部署一个复制集ReplicaSet](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#creating-a-deployment)。复制集在后台创建pod。检查部署的状态以查看它是否成功。
- [通过更新Deployment的PodTemplateSpec来声明Pods的新状态](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#updating-a-deployment)。新的复制集会被创建，Deployment管理器以受控的速率将pod从旧的复制集移动到新的复制集。每个新的复制集都会更新Deployment的修订版本。
- 如果Deployment的当前状态不稳定，则[回滚到较早的部署修订版](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#rolling-back-a-deployment)。每次回滚都会更新Deployment的修订版本。
- [扩大Deployment以促进更多负载](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#scaling-a-deployment)。
- [暂停部署](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#pausing-and-resuming-a-deployment)，然后对PodTemplateSpec进行多处修复，然后恢复部署以开始新的发布。
- [使用Deployment的状态来指示发布阻塞](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#deployment-status)。
- [清理不再需要的旧复制集](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#clean-up-policy)

## Creating a Deployment
下面是一个部署的示例。它创建了一个复制集来启动三个nginx pod：
controllers/nginx-deployment.yaml：
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
  labels:
    app: nginx
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.14.2
        ports:
        - containerPort: 80
```
1. Create the Deployment by running the following command:
```shell
kubectl apply -f https://k8s.io/examples/controllers/nginx-deployment.yaml
```
:heartpulse: 注意：您可以指定--record标志，把执行的命令写入资源注释中kubernetes.io/change-cause里。记录下来的变化对将来的introspection很有用。例如，查看在每个Deployment修订版本中执行的命令。

2. Run `kubectl get deployments` to check if the Deployment was created.
3. To see the Deployment rollout status, run `kubectl rollout status deployment.v1.apps/nginx-deployment`.
4. Run the `kubectl get deployments` again a few seconds later. The output is similar to this:
5. To see the ReplicaSet (rs) created by the Deployment, run `kubectl get rs`. The output is similar to this:
6. To see the labels automatically generated for each Pod, run `kubectl get pods --show-labels`. The output is similar to:

> :a: You must specify an appropriate selector and Pod template labels in a Deployment (in this case, app: nginx).
> 
> Do not overlap labels or selectors with other controllers (including other Deployments and StatefulSets). Kubernetes doesn't stop you from overlapping, and if multiple controllers have overlapping selectors those controllers might conflict and behave unexpectedly.

## Updating a Deployment 
> Note: 当且仅当Deployment的Pod模板（即.spec.template)更改，例如，如果模板的label或container image更新时才会触发更新，其他更新（如scaling the Deployment）不会触发更新。

1. Let's update the nginx Pods to use the nginx:1.16.1 image instead of the nginx:1.14.2 image.
    ```shell
    kubectl --record deployment.apps/nginx-deployment set image deployment.v1.apps/nginx-deployment nginx=nginx:1.16.1
    ```
    or simply use the following command:
    ```shell
    kubectl set image deployment/nginx-deployment nginx=nginx:1.16.1 --record
    ```
    The output is similar to this:
    ```shell
    deployment.apps/nginx-deployment image updated
    ```
    Alternatively, you can edit the Deployment and change .spec.template.spec.containers[0].image from nginx:1.14.2 to nginx:1.16.1:
    ```shell
    kubectl edit deployment.v1.apps/nginx-deployment
    ```
    The output is similar to this:
    ```shell
    deployment.apps/nginx-deployment edited
    ```

2. To see the rollout status, run: 
    ```shell
    kubectl rollout status deployment.v1.apps/nginx-deployment
    ```
    Deployment可以确保只有一定数量的pod在更新时关闭。默认情况下，它可以确保至少75%的所需Pod已启动（最大25%的Pod不可用）。
    
    Deployment还可以确保创建的pod比期望的Pod在数量上只高出一个确定的数值。默认情况下，它确保最多125%的期望的Pod（25%的最大上浮）。
    
    例如，如果仔细观察上面的部署，您将看到它首先创建了一个新的Pod，然后删除了一些旧的Pod，然后又创建了新的Pod。它不会杀死旧pod，除非有足够数量的新pod出现，也不会产生新pod，除非有足够数量的旧pod被杀死。它确保至少有2个pod可用，并且最多总共有4个pod可用。

    - Get details of your Deployment:
        ```shell
        kubectl describe deployments
        ```
        The output is similar to this:
        ```
            Name:                   nginx-deployment
            Namespace:              default
            CreationTimestamp:      Thu, 30 Nov 2017 10:56:25 +0000
            Labels:                 app=nginx
            Annotations:            deployment.kubernetes.io/revision=2
            Selector:               app=nginx
            Replicas:               3 desired | 3 updated | 3 total | 3 available | 0 unavailable
            StrategyType:           RollingUpdate
            MinReadySeconds:        0
            RollingUpdateStrategy:  25% max unavailable, 25% max surge
            Pod Template:
            Labels:  app=nginx
            Containers:
                nginx:
                Image:        nginx:1.16.1
                Port:         80/TCP
                Environment:  <none>
                Mounts:       <none>
                Volumes:        <none>
            Conditions:
                Type           Status  Reason
                ----           ------  ------
                Available      True    MinimumReplicasAvailable
                Progressing    True    NewReplicaSetAvailable
            OldReplicaSets:  <none>
            NewReplicaSet:   nginx-deployment-1564180365 (3/3 replicas created)
            Events:
                Type    Reason             Age   From                   Message
                ----    ------             ----  ----                   -------
                Normal  ScalingReplicaSet  2m    deployment-controller  Scaled up replica set nginx-deployment-2035384211 to 3
                Normal  ScalingReplicaSet  24s   deployment-controller  Scaled up replica set nginx-deployment-1564180365 to 1
                Normal  ScalingReplicaSet  22s   deployment-controller  Scaled down replica set nginx-deployment-2035384211 to 2
                Normal  ScalingReplicaSet  22s   deployment-controller  Scaled up replica set nginx-deployment-1564180365 to 2
                Normal  ScalingReplicaSet  19s   deployment-controller  Scaled down replica set nginx-deployment-2035384211 to 1
                Normal  ScalingReplicaSet  19s   deployment-controller  Scaled up replica set nginx-deployment-1564180365 to 3
                Normal  ScalingReplicaSet  14s   deployment-controller  Scaled down replica set nginx-deployment-2035384211 to 0
        ```

        在这里您可以看到，当您第一次创建部署时，它创建了一个复制集（nginx-Deployment-2035384211），并将其直接扩展到3个副本。当您更新部署时，它创建了一个新的复制集（nginx-Deployment-1564180365），并将其放大到1，然后将旧的复制集缩小到2，这样至少有2个pod可用，并且在任何时候最多创建4个pod。然后，它继续使用相同的滚动更新策略对新的和旧的复制集进行上下扩展。最后，在新的复制集中有3个可用的副本，旧的复制集缩小到0。

### Rollover (aka multiple updates in-flight) 滚动更新（也称为飞行中的多个更新）

每次Deployment controller观察到新部署时，都会创建一个复制集来启动所需的pod。如果部署已更新，则已经存在的ReplicaSet会被缩放scaled down，这些ReplicaSet是控制那些labels匹配.spec.selector但是template不匹配.spec.template的Pod。最终，新的ReplicaSet扩展，旧的ReplicaSet则缩放到0.

如果在更新Deployment的时候有正在进行中的滚动更新，那么每次更新Deployment就会创建一个新的ReplicaSet，然后启动这个新的ReplicaSet，并在之前scaling up的ReplicaSet的基础上继续滚动更新 -- 这会将之前旧的ReplicaSet放到一个列表里并把旧的ReplicaSet缩小。

例如，假设您创建一个部署来创建5个nginx:1.14.2，然后在只有3个nginx:1.14.2副本被创建出来的情况下更新部署以创建5个nginx:1.16.1。在这种情况下，部署会立即开始杀死3个已经创建的nginx:1.14.2 pod，并开始创造nginx:1.16.1 pod。它不会等待5个nginx:1.14.2的副本集完全创建。

### Label selector updates 标签选择器更新

通常不建议更新标签选择器，建议预先计划选择器。在任何情况下，如果您需要执行标签选择器更新，请格外小心，并确保您已经掌握了所有的含义。

> 注意：在API版本apps/v1中，部署的标签选择器在创建后是不可变的

- 添加新的Selector的同时需要用新的标签更新deployment spec中的Pod template labels，否则将返回验证错误。这是一个非重叠的更改，这意味着新选择器不会选中使用旧选择器创建的复制集和pod，最终结果是所有旧的复制集被隔离，然后创建一个新的复制集。
- 更新selector即更改selector key中的现有值 —— 和新增Selector的约束相同。
- 移除Selector即从Deployment selector中移除现有的key —— 不需要对Pod template label 进行任何更改。现有的复制集不会隔离，也不会创建新的复制集，但是请注意，删除的label仍然存在于任何现有的pod和replicaSet中

## Rolling Back a Deployment 回滚部署
有时，您可能希望回滚部署；例如，当部署不稳定时，例如循环崩溃。默认情况下，所有Deployment的部署历史都保存在系统中，可以随时进行回滚（您可以通过修改revision history限制来更改）。

> 注意：当Deployment的rollout行为触发时，一条Deployment revision记录就会创建。这意味着只有在Deployment的Pod template（.spec.template)更改时才会有新的revision记录创建，例如，如果更新template的标签或container image。其他更新（如放大缩小Deployment）不会创建Deployment revision，因此您可以和手动或自动缩放同时使用。这意味着当您回滚到早期版本时，只有Deployment的Pod template部分被回滚。

- 假设您在更新Deployment时犯了一个错误，将镜像名称nginx:1.16.1写成了nginx:1.161
  ```shell
    kubectl set image deployment.v1.apps/nginx-deployment nginx=nginx:1.161 --record=true
  ```
  The output is similar to this:
  ```
    deployment.apps/nginx-deployment image updated
  ```
- 更新卡住了。可以通过检查更新状态来验证：
  ```shell
    kubectl rollout status deployment.v1.apps/nginx-deployment
    Waiting for rollout to finish: 1 out of 3 new replicas have been updated...
  ```
- Press Ctrl-C to stop the above rollout status watch. For more information on stuck rollouts, [read more here](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#deployment-status).
- 您可以看到旧副本（nginx-deployment-1564180365和nginx-deployment-2035384211）的数量是2，新副本（nginx-deployment-3066724191）是1。
  ```shell
  kubectl get rs

  NAME                          DESIRED   CURRENT   READY   AGE
  nginx-deployment-1564180365   3         3         3       25s
  nginx-deployment-2035384211   0         0         0       36s
  nginx-deployment-3066724191   1         1         0       6s
  ```
- Looking at the Pods created, you see that 1 Pod created by new ReplicaSet is stuck in an image pull loop.
  ```shell
    kubectl get pods
    NAME                                READY     STATUS             RESTARTS   AGE
    nginx-deployment-1564180365-70iae   1/1       Running            0          25s
    nginx-deployment-1564180365-jbqqo   1/1       Running            0          25s
    nginx-deployment-1564180365-hysrc   1/1       Running            0          25s
    nginx-deployment-3066724191-08mng   0/1       ImagePullBackOff   0          6s
  ```

- Get the description of the Deployment:
  ```shell
    kubectl describe deployment

    Name:           nginx-deployment
    Namespace:      default
    CreationTimestamp:  Tue, 15 Mar 2016 14:48:04 -0700
    Labels:         app=nginx
    Selector:       app=nginx
    Replicas:       3 desired | 1 updated | 4 total | 3 available | 1 unavailable
    StrategyType:       RollingUpdate
    MinReadySeconds:    0
    RollingUpdateStrategy:  25% max unavailable, 25% max surge
    Pod Template:
    Labels:  app=nginx
    Containers:
    nginx:
        Image:        nginx:1.161
        Port:         80/TCP
        Host Port:    0/TCP
        Environment:  <none>
        Mounts:       <none>
    Volumes:        <none>
    Conditions:
    Type           Status  Reason
    ----           ------  ------
    Available      True    MinimumReplicasAvailable
    Progressing    True    ReplicaSetUpdated
    OldReplicaSets:     nginx-deployment-1564180365 (3/3 replicas created)
    NewReplicaSet:      nginx-deployment-3066724191 (1/1 replicas created)
    Events:
    FirstSeen LastSeen    Count   From                    SubObjectPath   Type        Reason              Message
    --------- --------    -----   ----                    -------------   --------    ------              -------
    1m        1m          1       {deployment-controller }                Normal      ScalingReplicaSet   Scaled up replica set nginx-deployment-2035384211 to 3
    22s       22s         1       {deployment-controller }                Normal      ScalingReplicaSet   Scaled up replica set nginx-deployment-1564180365 to 1
    22s       22s         1       {deployment-controller }                Normal      ScalingReplicaSet   Scaled down replica set nginx-deployment-2035384211 to 2
    22s       22s         1       {deployment-controller }                Normal      ScalingReplicaSet   Scaled up replica set nginx-deployment-1564180365 to 2
    21s       21s         1       {deployment-controller }                Normal      ScalingReplicaSet   Scaled down replica set nginx-deployment-2035384211 to 1
    21s       21s         1       {deployment-controller }                Normal      ScalingReplicaSet   Scaled up replica set nginx-deployment-1564180365 to 3
    13s       13s         1       {deployment-controller }                Normal      ScalingReplicaSet   Scaled down replica set nginx-deployment-2035384211 to 0
    13s       13s         1       {deployment-controller }                Normal      ScalingReplicaSet   Scaled up replica set nginx-deployment-3066724191 to 1
  ```
  
:a:  要解决此问题，您需要回滚到以前稳定的部署版本

### Checking Rollout History of a Deployment 检查Deployment的rollout历史记录

1. First, check the revisions of this Deployment:
   ```shell
    kubectl rollout history deployment.v1.apps/nginx-deployment

    deployments "nginx-deployment"
    REVISION    CHANGE-CAUSE
    1           kubectl apply --filename=https://k8s.io/examples/controllers/nginx-deployment.yaml --record=true
    2           kubectl set image deployment.v1.apps/nginx-deployment nginx=nginx:1.16.1 --record=true
    3           kubectl set image deployment.v1.apps/nginx-deployment nginx=nginx:1.161 --record=true
   ```
2. To see the details of each revision, run:
   ```shell
    kubectl rollout history deployment.v1.apps/nginx-deployment --revision=2

    deployments "nginx-deployment" revision 2
    Labels:       app=nginx
            pod-template-hash=1159050644
    Annotations:  kubernetes.io/change-cause=kubectl set image deployment.v1.apps/nginx-deployment nginx=nginx:1.16.1 --record=true
    Containers:
    nginx:
        Image:      nginx:1.16.1
        Port:       80/TCP
        QoS Tier:
            cpu:      BestEffort
            memory:   BestEffort
        Environment Variables:      <none>
    No volumes.
   ```

### Rolling Back to a Previous Revision 回滚到以前的修订版

Follow the steps given below to rollback the Deployment from the current version to the previous version, which is version 2.

1. Now you've decided to undo the current rollout and rollback to the previous revision:
   ```shell
    # 回滚到前一个版本
    kubectl rollout undo deployment.v1.apps/nginx-deployment

    # 回滚到指定的版本
    kubectl rollout undo deployment.v1.apps/nginx-deployment --to-revision=2
   ```
   The Deployment is now rolled back to a previous stable revision. As you can see, a `DeploymentRollback` event for rolling back to revision 2 is generated from Deployment controller.

2. Check if the rollback was successful and the Deployment is running as expected, run:
   ```shell
   kubectl get deployment nginx-deployment
   ```

## Scaling a Deployment 放大缩小

You can scale a Deployment by using the following command:
```shell
kubectl scale deployment.v1.apps/nginx-deployment --replicas=10
```

假设集群中启用了[水平pod自动调整](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale-walkthrough/)，您可以为部署设置一个自动缩放器，并根据现有Pod的CPU利用率选择要运行的最小和最大Pod数量。
```shell
kubectl autoscale deployment.v1.apps/nginx-deployment --min=10 --max=15 --cpu-percent=80
```

### Proportional scaling 比例缩放 

RollingUpdate Deployment支持同时运行应用程序的多个版本。当您或自动缩放器缩放处于rollout过程中（正在进行或已暂停）的RollingUpdate Deployment时，部署控制器会平衡现有活动复制集中的其他副本（带有pod的复制集），以降低风险。这称为按比例缩放。

例如，您正在运行一个包含10个副本的部署，maxSurge=3，maxUnavailable=2。

- Ensure that the 10 replicas in your Deployment are running.
- You update to a new image which happens to be unresolvable from inside the cluster.
  ```shell
    kubectl set image deployment.v1.apps/nginx-deployment nginx=nginx:sometag
  ```
- The image update starts a new rollout with ReplicaSet nginx-deployment-1989198191, but it's blocked due to the maxUnavailable requirement that you mentioned above. Check out the rollout status:
  ```shell
    kubectl get rs

    NAME                          DESIRED   CURRENT   READY     AGE
    nginx-deployment-1989198191   5         5         0         9s
    nginx-deployment-618515232    8         8         8         1m
  ```
- 然后出现了一个新的Deployment scaling请求。autoscaler将Deployment副本增加到15个。部署控制器需要决定将这些新的5个副本添加到何处。如果不使用比例缩放，那么所有5个都将添加到新的复制集中。如果使用按比例扩展，您可以将附加副本分布到所有复制集中。较大的比例副本跑到了具有副本较多的复制集中，较小比例的副本跑到了具有较少副本的复制集中。任何剩余的副本都将添加到具有最多副本的复制集中。没有副本的复制集则不会扩大。

在上面的示例中，3个副本添加到旧复制集中，2个副本添加到新复制集中。部署过程最终应该将所有副本移动到新的复制集，前提是新副本变得正常。要确认这一点，请运行：
```shell
kubectl get deploy
```
输出可能如下：
```
NAME                 DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
nginx-deployment     15        18        7            8           7m
```

查看Rollout状态确认副本如何添加到每个复制集。
```shell
kubectl get rs

NAME                          DESIRED   CURRENT   READY     AGE
nginx-deployment-1989198191   7         7         0         7m
nginx-deployment-618515232    11        11        11        7m
```

## Pausing and Resuming a Deployment

您可以在触发一个或多个更新之前暂停部署，然后继续进行。这允许您在暂停和恢复之间应用多个修复，而不会触发不必要的rollouts。

- For example, with a Deployment that was just created: Get the Deployment details:
```shell
kubectl get deploy

NAME      DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
nginx     3         3         3            3           1m
```
Get the rollout status:
```shell
kubectl get rs

NAME               DESIRED   CURRENT   READY     AGE
nginx-2142116321   3         3         3         1m
```

- Pause by running the following command:
```shell
kubectl rollout pause deployment.v1.apps/nginx-deployment

deployment.apps/nginx-deployment paused
```

- Then update the image of the Deployment:
```shell
kubectl set image deployment.v1.apps/nginx-deployment nginx=nginx:1.16.1

deployment.apps/nginx-deployment image updated
```

- Notice that no new rollout started:
```shell
kubectl rollout history deployment.v1.apps/nginx-deployment

deployments "nginx"
REVISION  CHANGE-CAUSE
1   <none>
```

- Get the rollout status to ensure that the Deployment is updates successfully:
```shell
kubectl get rs

NAME               DESIRED   CURRENT   READY     AGE
nginx-2142116321   3         3         3         2m
```
- You can make as many updates as you wish, for example, update the resources that will be used:
```shell
kubectl set resources deployment.v1.apps/nginx-deployment -c=nginx --limits=cpu=200m,memory=512Mi
```
Deployment会以暂停之前的初始状态继续提供服务，只要Deployment处于暂停状态，对Deployment的更新将不会产生任何影响。

- Eventually, resume the Deployment and observe a new ReplicaSet coming up with all the new updates:
```shell
kubectl rollout resume deployment.v1.apps/nginx-deployment
```

- Watch the status of the rollout until it's done.
```shell
kubectl get rs -w

NAME               DESIRED   CURRENT   READY     AGE
nginx-2142116321   2         2         2         2m
nginx-3926361531   2         2         0         6s
nginx-3926361531   2         2         1         18s
nginx-2142116321   1         2         2         2m
nginx-2142116321   1         2         2         2m
nginx-3926361531   3         2         1         18s
nginx-3926361531   3         2         1         18s
nginx-2142116321   1         1         1         2m
nginx-3926361531   3         3         1         18s
nginx-3926361531   3         3         2         19s
nginx-2142116321   0         1         1         2m
nginx-2142116321   0         1         1         2m
nginx-2142116321   0         0         0         2m
nginx-3926361531   3         3         3         20s
```

- Get the status of the latest rollout:
```shell
kubectl get rs

NAME               DESIRED   CURRENT   READY     AGE
nginx-2142116321   0         0         0         2m
nginx-3926361531   3         3         3         28s
```

> 注意：在恢复之前，无法回滚暂停的Deployment


---
## Deployment status 

部署在其生命周期中进入各种状态。在推出新的ReplicaSet的同时可以是[progressing](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#progressing-deployment)状态，也可以[complete](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#complete-deployment)，也可以是[fail to progress](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#failed-deployment)。

### Progressing Deployment 正在进行部署
当执行以下任务之一时，Kubernetes将Deployment标记为正在进行：
- Deployment创建一个新的复制集。
- Deployment正在扩大其最新的复制集。
- Deployment正在缩减其旧的复制集。
- 新的Pod变为ready或者available（至少已经ready [MinReadySeconds](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#min-ready-seconds)这么久）。

可以使用 `kubectl rollout status` 监视Deployment的进度
 
### Complete Deployment 完成部署
当部署具有以下特征时，Kubernetes将其标记为已完成：
- 与部署关联的所有副本都已更新到您指定的最新版本，这意味着您请求的任何更新都已完成。
- 与部署关联的所有复制副本都available。
- 没有旧的Deployment副本运行。
  
您可以使用`kubectl rollout status`检查部署是否已完成。如果`kubectl rollout status`返回零并退出，那么rollout说明已经成功完成。
```shell
kubectl rollout status deployment.v1.apps/nginx-deployment

Waiting for rollout to finish: 2 of 3 updated replicas are available...
deployment.apps/nginx-deployment successfully rolled out
```

### Failed Deployment 部署失败

您的部署可能会在尝试部署其最新的复制集时卡住而不能complete。这可能是由于以下一些因素造成的：
- Insufficient quota 配额不足
- readiness probe故障
- 镜像拉取错误
- 没有足够的权限
- limit ranges限制范围
- 应用程序运行时配置错误

检测这种情况的一种方法是在Deployment spec中指定一个deadline参数：（`.spec.progressDeadlineSeconds`) [.spec.progressDeadlineSeconds](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#progress-deadline-seconds)表示Deployment controller认为Deployment进度停滞不动前等待的秒数。

以下kubectl命令设置spec中progressDeadlineSeconds的值，使控制器在10分钟后报告Deployment的进度丢失：
```shell
kubectl patch deployment.v1.apps/nginx-deployment -p '{"spec":{"progressDeadlineSeconds":600}}'

deployment.apps/nginx-deployment patched
```

一旦超过了最后期限，部署控制器就会向Deployment的`.status.conditions`里添加一个具有以下属性的DeploymentCondition:
- Type=Progressing
- Status=False
- Reason=ProgressDeadlineExceeded

> 注意：Kubernetes不会对stalled停滞的部署执行任何操作，只是报告一个`Reason=ProgressDeadlineExceeded`的状态条件status condition。更高级别的编排器可以利用它并相应地执行操作，例如，将部署回滚到以前的版本。

> 如果暂停pause部署，Kubernetes不会根据指定的截止日期检查进度。您可以在rollout过程中安全地暂停部署并继续，并且不会触发超过截止日期的情况。

您可能会在部署中遇到暂时性错误，可能是由于您设置的低超时，或者是由于任何其他类型的错误（可以被视为暂时性错误）。例如，假设您的配额不足。如果查看deployment详情，您将注意到以下部分：
```shell
kubectl describe deployment nginx-deployment

<...>
Conditions:
  Type            Status  Reason
  ----            ------  ------
  Available       True    MinimumReplicasAvailable
  Progressing     True    ReplicaSetUpdated
  ReplicaFailure  True    FailedCreate
<...>
```

如果你运行`kubectl get deployment nginx-deployment -o yaml`, Deployment status会呈现：
```
status:
  availableReplicas: 2
  conditions:
  - lastTransitionTime: 2016-10-04T12:25:39Z
    lastUpdateTime: 2016-10-04T12:25:39Z
    message: Replica set "nginx-deployment-4262182780" is progressing.
    reason: ReplicaSetUpdated
    status: "True"
    type: Progressing
  - lastTransitionTime: 2016-10-04T12:25:42Z
    lastUpdateTime: 2016-10-04T12:25:42Z
    message: Deployment has minimum availability.
    reason: MinimumReplicasAvailable
    status: "True"
    type: Available
  - lastTransitionTime: 2016-10-04T12:25:39Z
    lastUpdateTime: 2016-10-04T12:25:39Z
    message: 'Error creating: pods "nginx-deployment-4262182780-" is forbidden: exceeded quota:
      object-counts, requested: pods=1, used: pods=3, limited: pods=2'
    reason: FailedCreate
    status: "True"
    type: ReplicaFailure
  observedGeneration: 3
  replicas: 2
  unavailableReplicas: 2
```

最终，一旦超过Deployment progress deadline，Kubernetes将更新status和Progressing condition的原因：
```
Conditions:
  Type            Status  Reason
  ----            ------  ------
  Available       True    MinimumReplicasAvailable
  Progressing     False   ProgressDeadlineExceeded
  ReplicaFailure  True    FailedCreate
```

您可以通过缩小部署、缩小可能正在运行的其他控制器或增加命名空间中的配额来解决配额不足的问题。如果您满足配额条件，并且部署控制器随后完成Deployment的部署，您将看到Deployment的状态更新为成功（status=True，Reason=NewReplicaSetAvailable）。

```
Conditions:
  Type          Status  Reason
  ----          ------  ------
  Available     True    MinimumReplicasAvailable
  Progressing   True    NewReplicaSetAvailable
```

`Type=Available 且 Status=True`意味着您的部署具有最低可用性。最低可用性由部署策略中指定的参数决定。`Type=Progressing 且 Status=True`意味着您的Deployment正在rollout并且Deployment本身也在进行中，或者它已经成功地完成了它的进度，并且提供了所需的最少新副本（see the Reason of the condition for the particulars - in our case `Reason=NewReplicaSetAvailable` means that the Deployment is complete）。

您可以使用`kubectl rollout status`检查Deployment是否未能进行。如果Deployment超过进度截止日期progression deadline，`kubectl rollout status`以非零退出代码。

```shell
kubectl rollout status deployment.v1.apps/nginx-deployment
```
The output is similar to this:
```
Waiting for rollout to finish: 2 out of 3 new replicas have been updated...
error: deployment "nginx" exceeded its progress deadline
```

and the exit status from kubectl rollout is 1 (indicating an error):
```shell
echo $?
1
```

### Operating on a failed deployment 在失败的部署上操作

所有complete Deployment的操作都适用于Failed Deployment。如果需要在Deployment Pod模板中应用多个调整，可以放大/缩小、回滚到以前的版本，甚至可以暂停它。


## Clean up Policy
你可以在Deployment里设置`.spec.revisionHistoryLimit`字段指定要为此Deployment保留多少个旧的ReplicaSets。其余的将在后台被垃圾收集掉。默认为10。
> 注意：将此字段显式设置为0将导致清除Deployment的所有历史记录，因此Deployment将无法回滚。

## Canary Deployment
如果要针对部分使用Deployment的用户或服务器进行发布，则可以创建多个Deployment，每个Deployment对应一个Release，并按照 [managing resources](https://kubernetes.io/docs/concepts/cluster-administration/manage-deployment/#canary-deployments) 中描述的canary pattern 进行设置。

## Writing a Deployment Spec 

与所有其他Kubernetes配置一样，Deployment需要`.apiVersion、.kind 和 .metadata`字段。有关使用配置文件的一般信息，请参阅[部署应用程序](https://kubernetes.io/docs/tasks/run-application/run-stateless-application-deployment/)、配置容器和[使用kubectl管理资源](https://kubernetes.io/docs/concepts/overview/working-with-objects/object-management/)的文档。部署对象的名称必须是有效的DNS子域名。

Deployment还需要`.spec`部分

### Pod Template
这个`.spec.template`和`.spec.selector`是.spec的唯一必需字段。

`.spec.template`是Pod模板。它的模式与Pod完全相同，只是它是嵌套的，没有apiVersion或kind。

除了Pod的必需字段外，Deployment中的Pod模板必须指定适当的标签和适当的重新启动策略。对于标签，请确保不要与其他控制器重叠。请参见[选择器](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#selector)。

`.spec.template.spec.restartPolicy`的值只能等于Always，如果未指定，则为默认值

### Replicas

`.spec.replicas` is an optional field that specifies the number of desired Pods. It defaults to 1

### Selector
`.spec.selector`是一个必需字段，它是label selector, 选中Deployment所针对的pod。
`.spec.selector`必须和`.spec.template.metadata.labels`匹配，否则将被API拒绝。

在API版本apps/v1中, `.spec.selector`还有`.metadata.labels`在没有设置的情况下默认值不是`.spec.template.metadata.labels`。所以必须显式地设置它们。还要注意`.spec.selector`在apps/v1中创建部署后是不可变的。

如果Pod的labels和上面的selector匹配，并且Pod的template和`.spec.template`定义的不一样，或者Pod的数量超过了`.spec.replicas`指定数量，那么Deployment就会停掉这些Pod。如果Pod的数量少于期望值，Deployment则会使用`.spec.template`创建新的pod并启动它。

> 注意：您不应该通过创建另一个部署或通过创建另一个控制器（如replicaSet或ReplicationController）来创建标签与此选择器匹配的其他pod。如果您这样做，第一个部署会认为是它自己创建了其他这些pod。Kubernetes 并不会阻止你这么做。

如果有多个具有重叠选择器的控制器，则这些控制器将相互冲突，并且行为不正确。

### Strategy 
`.spec.strategy`指定用新的Pod替换旧的Pod的策略。`.spec.strategy.type `可以是“Recreate”或“RollingUpdate”。默认值是RollingUpdate”

#### Recreate Deployment
当`.spec.strategy.type==Recreate`时，创建新的Pod之前，所有现有的Pod都会被杀死。

> 注意：这将只保证在创建升级之前终止Pod。如果升级Deployment，则旧版本的所有pod将立即终止。在创建新修订的任何Pod之前，系统会一直等待所有旧Pod成功删除。如果您手动删除一个Pod，则生命周期由replicaSet控制，并且将立即创建替换的Pod（即使旧Pod仍处于Terminating state）。如果您需要尽最大程度保证您的Pods，您应该考虑使用StatefulSet

#### Rolling Update Deployment
当`.spec.strategy.type==RollingUpdate`时Deployment以滚动更新方式更新Pods。可以指定`maxUnavailable`和`maxSurge`来控制滚动更新过程

1. Max Unavailable

    `.spec.strategy.rollingUpdate.maxUnavailable`是一个可选字段，指定在更新过程中不可用的pod的最大数量。该值可以是绝对数（例如，5）或期望pod数的百分比（例如，10%）。绝对数是通过四舍五入从百分比中计算出来的。如果`.spec.strategy.rollingUpdate.maxSurge`等于0，那么这个值就不能为0。默认值为25%。

    例如，当这个值设置为30%时，当滚动更新开始时，旧的复制集可以立即缩小到期望pod数目的70%。一旦新的Pods准备好了，旧的replicaSet就可以进一步缩小，然后扩大新的replicaSet，确保在更新期间随时可用的pod总数至少是所需pod的70%。
2. Max Surge

    `.spec.strategy.rollingUpdate.maxSurge`是一个可选字段，指定允许创建pod个数多出desired pod个数的部分。该值可以是绝对数（例如，5）或所需pod的百分比（例如，10%）。如果MaxUnavailable为0，则该值不能为0。绝对数是按百分比四舍五入计算出来的。默认值为25%。

    例如，当该值设置为30%时，新的复制集可以在滚动更新开始时立即按比例放大，但新旧pod的总数不会超过所需pod的130%。一旦旧的pod被杀死，新的复制集就可以进一步扩大，以确保在更新期间任何时候运行的pod总数最多为所需pod的130%。

### Progress Deadline Seconds 进度截止时间

`.spec.progressDeadlineSeconds`是一个可选字段，指定在系统报告Deployment [failed progressing](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#failed-deployment)之前，您希望等待Deployment继续进行的秒数。failed progressing会在资源的状态信息里以condition对象的形式出现，并且对象的`Type=Progressing, Status=False, Reason=ProgressDeadlineExceeded`。部署控制器将继续重试部署。默认值为600秒。将来，一旦实现自动回滚，部署控制器将在观察到这种情况时立即回滚部署。

如果指定此值，必须要大于`.spec.minReadySeconds`

### Min Ready Seconds
`.spec.minReadySeconds`是一个可选字段，指定新创建的Pod在没有任何容器崩溃的情况下变为ready后, 在变成available就绪之前，等待的最小秒数。默认值为0（Pod一旦ready就被视为available可用）。若要了解Pod何时ready，请参阅[容器探测器](https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#container-probes)。

### Revision History Limit

Deployment的修订历史记录存储在其控制的ReplicaSets中。

`.spec.revisionHistoryLimit`是一个可选字段，指定要保留以允许回滚的旧复制集的数量。这些旧的复制集消耗etcd中的资源并将`kubectl get rs`的输出crowd(压缩？)。每个Deployment revision的配置都存储在其复制集中；因此，一旦删除旧的复制集，您就无法回滚到该部署版本。默认保留10个旧的复制集，但是理想的值应该取决于新的复制集的跟新率和稳定性。

更具体地说，将此字段设置为0意味着将清理包含0个副本的所有旧复制集。在这种情况下，新Deployment rollout将无法撤消，因为它的修订历史记录已清除。

### Paused 

`.spec.paused`是用于暂停和恢复Deployment的可选的布尔型字段。暂停Deployment和未暂停Deployment之间的唯一区别是，只要暂停Deployment，对暂停Deployment的PodTemplateSpec的任何更改都不会触发新的rollout。默认情况下，Deployment创建后不会暂停。

