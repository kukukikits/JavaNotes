# Eviction Policy

本页概述了kubernetes的驱逐政策。

## Eviction Policy

kubelet会主动监视并防止计算资源完全不足。在这种情况下，kubelet可以通过使一个或多个Pod失效来回收匮乏的资源。当kubelet使一个Pod出现故障时，它终止其所有容器并将其Pod阶段标记为Failed。如果被逐出的Pod由Deployment管理，那么Deployment将创建另一个Pod, 并由Kubernetes调度。

## What's next
Learn how to [configure out of resource handling](https://kubernetes.io/docs/tasks/administer-cluster/out-of-resource/) with eviction signals and thresholds.