# Node-specific Volume Limits

此页描述各种云提供商可附加到节点的最大卷数。

像Google、Amazon和Microsoft这样的云提供商通常对一个节点可以连接多少卷有限制。对Kubernetes来说，遵守这些限制是很重要的。否则，在节点上调度的pod可能会在等待卷连接时卡住。

## Kubernetes default limits 

Kubernetes计划程序对可以连接到节点的卷数有默认限制：

Cloud service | Maximum volumes per Node
--------------|-------------------------
Amazon Elastic Block Store (EBS) | 39
Google Persistent Disk | 16
Microsoft Azure Disk Storage | 16

## Custom limits

通过设置KUBE_MAX_PD_VOLS环境变量的值，然后启动调度程序，可以更改这些限制。CSI驱动程序可能有不同的程序，请参阅他们的文档，了解如何自定义其限制。

如果设置的限制高于默认限制，请小心。请参考云提供商的文档，以确保节点实际上可以支持您设置的限制。

该限制适用于整个集群，因此它会影响所有节点。

## Dynamic volume limits 
*FEATURE STATE: Kubernetes v1.17 [stable]*

以下卷类型支持动态卷限制。

* Amazon EBS
* Google Persistent Disk
* Azure Disk
* CSI

对于由树内卷插件管理的卷，Kubernetes会自动确定节点类型，并为节点强制执行适当的最大卷数。例如：

- 在[google compute Engine](https://cloud.google.com/compute/)上，[根据节点类型](https://cloud.google.com/compute/docs/disks/#pdnumberlimits)，最多可以将127个卷附加到节点。
- 对于M5、C5、R5、T3和Z1D实例类型的Amazon EBS磁盘，Kubernetes只允许25个卷连接到一个节点。对于[Amazon Elastic Compute Cloud（EC2）](https://aws.amazon.com/ec2/)上的其他实例类型，Kubernetes允许39个卷连接到一个节点。
- 在Azure上，最多可以将64个磁盘连接到节点，具体取决于节点类型。有关更多详细信息，请参阅[Azure中虚拟机的大小](https://docs.microsoft.com/en-us/azure/virtual-machines/windows/sizes)。
- 如果CSI存储驱动程序公布了一个节点的最大卷数（使用`NodeGetInfo`），kube-scheduler程序将遵守该限制。有关详细信息，请参阅[CSI规范](https://github.com/container-storage-interface/spec/blob/master/spec.md#nodegetinfo)。
- 对于已迁移到CSI驱动程序的树内插件管理的卷，最大卷数将是CSI驱动程序报告的卷数。



