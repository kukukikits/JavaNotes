# Resource Bin Packing for Extended Resources

**FEATURE STATE: Kubernetes v1.16 [alpha]**

kube调度器可以配置开启资源的bin packing，同扩展资源一起，通过使用`RequestedToCapacityRatioResourceAllocation` priority函数实现。Priority函数可根据每个自定义的需求对kube调度程序进行微调。

## Enabling Bin Packing using RequestedToCapacityRatioResourceAllocation

在kubernetes1.15之前，Kube调度器根据CPU和内存等主要资源的请求容量比对节点进行评分。kubernetes1.16在priority函数中添加了一个新参数，允许用户指定资源以及每个资源的权重，以便根据请求容量比对节点进行评分。这允许用户使用适当的参数对扩展资源进行装箱，并提高大型集群中稀缺资源的利用率。RequestedToCapacityRatiorSourceAllocation优先级函数的行为可以由名为requestedToCapacityRatioArguments的配置项控制。此参数由shape和resources两个参数组成。Shape allows the user to tune the function as least requested or most requested based on utilization and score values. Shape允许用户根据利用率和分数值将函数调整为请求最少或请求最多的函数。resources由name和weight组成，name指定评分时要考虑的资源，weight指定每个资源的权重。

下面是一个示例配置，它将requestedToCapacityRatioArguments设置为扩展资源`intel.com/foo`和`intel.com/bar`的装箱行为

```yaml
{
    "kind" : "Policy",
    "apiVersion" : "v1",

    ...

    "priorities" : [

       ...

      {
        "name": "RequestedToCapacityRatioPriority",
        "weight": 2,
        "argument": {
          "requestedToCapacityRatioArguments": {
            "shape": [
              {"utilization": 0, "score": 0},
              {"utilization": 100, "score": 10}
            ],
            "resources": [
              {"name": "intel.com/foo", "weight": 3},
              {"name": "intel.com/bar", "weight": 5}
            ]
          }
        }
      }
    ],
  }
```
**This feature is disabled by default**

### Tuning RequestedToCapacityRatioResourceAllocation Priority Function 

shape 用于指定RequestedToCapacityRatioPriority函数的行为。
```js
 {"utilization": 0, "score": 0},
 {"utilization": 100, "score": 10}
```

如果利用率为0%，则上述参数为节点提供0分；如果利用率为100%，则为10分，以此来启用装箱行为。要启用最小请求，分数值必须按如下方式反转。
```js
 {"utilization": 0, "score": 100},
 {"utilization": 100, "score": 0}
```

resources是一个可选参数，默认设置为：

```json
"resources": [
              {"name": "CPU", "weight": 1},
              {"name": "Memory", "weight": 1}
            ]
```

它可用于添加扩展资源，如下所示：

```json
"resources": [
              {"name": "intel.com/foo", "weight": 5},
              {"name": "CPU", "weight": 3},
              {"name": "Memory", "weight": 1}
            ]
```

weight参数是可选的，如果未指定，则设置为1。此外，权重不能设置为负值。

### How the RequestedToCapacityRatioResourceAllocation Priority Function Scores Nodes 

本节是为那些想了解此功能的内部细节的人准备的。下面是一个如何计算节点得分的示例。

```
Requested Resources

intel.com/foo : 2
Memory: 256MB
CPU: 2

Resource Weights

intel.com/foo : 5
Memory: 1
CPU: 3

FunctionShapePoint {{0, 0}, {100, 10}}

Node 1 Spec

Available:
intel.com/foo : 4
Memory : 1 GB
CPU: 8

Used:
intel.com/foo: 1
Memory: 256MB
CPU: 1


Node Score:

intel.com/foo  = resourceScoringFunction((2+1),4)
               = (100 - ((4-3)*100/4)
               = (100 - 25)
               = 75                       # requested + used = 75% * available
               = rawScoringFunction(75) 
               = 7                        # floor(75/10) 

Memory         = resourceScoringFunction((256+256),1024)
               = (100 -((1024-512)*100/1024))
               = 50                       # requested + used = 50% * available
               = rawScoringFunction(50)
               = 5                        # floor(50/10)

CPU            = resourceScoringFunction((2+1),8)
               = (100 -((8-3)*100/8))
               = 37.5                     # requested + used = 37.5% * available
               = rawScoringFunction(37.5)
               = 3                        # floor(37.5/10)

NodeScore   =  (7 * 5) + (5 * 1) + (3 * 3) / (5 + 1 + 3)
            =  5


Node 2 Spec

Available:
intel.com/foo: 8
Memory: 1GB
CPU: 8

Used:

intel.com/foo: 2
Memory: 512MB
CPU: 6


Node Score:

intel.com/foo  = resourceScoringFunction((2+2),8)
               =  (100 - ((8-4)*100/8)
               =  (100 - 50)
               =  50
               =  rawScoringFunction(50)
               = 5

Memory         = resourceScoringFunction((256+512),1024)
               = (100 -((1024-768)*100/1024))
               = 75
               = rawScoringFunction(75)
               = 7

CPU            = resourceScoringFunction((2+6),8)
               = (100 -((8-8)*100/8))
               = 100
               = rawScoringFunction(100)
               = 10

NodeScore   =  (5 * 5) + (7 * 1) + (10 * 3) / (5 + 1 + 3)
            =  7
```



