

# 本文描述所有Controllers

---

# CronJob
*FEATURE STATE: Kubernetes v1.8 [beta]*

CronJob按重复计划创建作业。

一个CronJob对象就像crontab（cron table）文件的一行。它以Cron格式按给定的时间表定期运行作业。

> :warning: 注意事项：
> 所有CronJob schedule: 时间都基于[kube-controller-manager](https://kubernetes.io/docs/reference/command-line-tools-reference/kube-controller-manager/)的时区。
> 
> 如果control plane在Pods或裸机容器中运行kube-controller-manager，则为 kube-controller-manager容器设置的时区用来决定cron job controller使用的时区。

## CronJob 

CronJobs对于创建周期性和重复性任务非常有用，例如运行备份或发送电子邮件。CronJobs还可以为特定的时间安排单个任务，例如在集群可能空闲时调度作业

### Example
This example CronJob manifest prints the current time and a hello message every minute:

application/job/cronjob.yaml 

```yaml
apiVersion: batch/v1beta1
kind: CronJob
metadata:
  name: hello
spec:
  schedule: "*/1 * * * *"
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: hello
            image: busybox
            args:
            - /bin/sh
            - -c
            - date; echo Hello from the Kubernetes cluster
          restartPolicy: OnFailure
```

([Running Automated Tasks with a CronJob](https://kubernetes.io/docs/tasks/job/automated-tasks-with-cron-jobs/) takes you through this example in more detail).

## CronJob limitations

cron作业大约在其调度的每个执行时间创建一个job对象。我们说“关于”是因为在某些情况下，可能会创造两个job，或者可能没有创造job。我们试图解决这些问题，但并没有完全阻止它们。因此，jobs应该是幂等的。

如果startingDeadlineSeconds设置值很大或未设置（默认值），并且concurrencyPolicy设置为Allow，则作业将始终至少运行一次。

对于每个CronJob，CronJob控制器检查从上次调度到现在的持续时间内错过了多少schedules。如果错过的schedule超过100个，则不会启动作业并记录错误

```
Cannot determine if job needs to be started. Too many missed start time (> 100). Set or decrease .spec.startingDeadlineSeconds or check clock skew.
```

需要注意的是，如果startingDeadlineSeconds字段设置（不是nil），控制器会统计从startingDeadlineSeconds值到现在错过的作业数量，而不是从上次计划的时间到现在。例如，如果startingDeadlineSeconds为200，则控制器将统计过去200秒中错过的作业数。

如果未能在计划时间创建CronJob，则将其视为未完成。例如，如果concurrency policy设置为Forbid，并在前一个计划仍在运行时尝试schedule一个CronJob，则它将被视为未完成。

例如，假设CronJob被设置为从08:30:00开始每一分钟安排一个新作业，并且它的startingDeadlineSeconds字段没有设置。如果CronJob控制器恰好从08:29:00停止运行到10:21:00，则由于错过计划的作业数大于100，所以作业将不会再启动。

为了进一步说明这个概念，假设CronJob被设置为从08:30:00开始每一分钟安排一个新作业，并且它的startingDeadlineSeconds设置为200秒。如果CronJob控制器恰好在上一个示例（08:29:00到10:21:00）相同的时间内关闭，则作业仍将在10:22:00开始。这是因为控制器现在检查在过去200秒内错过了多少个计划（即3个），而不是从上次计划的时间到现在。

CronJob只负责创建与其时间表相匹配的作业，而作业则负责管理它所代表的pod。

## What's next
[Cron expression format](https://en.wikipedia.org/wiki/Cron) documents the format of CronJob schedule fields.

For instructions on creating and working with cron jobs, and for an example of CronJob manifest, see Running [automated tasks with cron jobs](https://kubernetes.io/docs/tasks/job/automated-tasks-with-cron-jobs).
