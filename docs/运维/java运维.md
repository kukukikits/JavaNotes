# Docker环境Java进程占用CPU过高/内存占用过高，问题排查
1. top 命令查看CPU占用情况
下面的数据显示CPU的占用不高，只有%Cpu(s): 23.7 us，所以一下的步骤只演示排查的步骤
```bash
top - 15:46:50 up 21 days, 21:56,  4 users,  load average: 1.95, 1.99, 1.62
Tasks: 195 total,   1 running, 194 sleeping,   0 stopped,   0 zombie
%Cpu(s): 23.7 us,  1.2 sy,  0.0 ni, 74.9 id,  0.1 wa,  0.0 hi,  0.1 si,  0.0 st
KiB Mem :  7997164 total,   154104 free,  5087112 used,  2755948 buff/cache
KiB Swap:  8388604 total,  7795708 free,   592896 used.  2278824 avail Mem 

  PID USER      PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND                                                                                                                                   
26599 root      20   0 3952376 584712  18260 S  89.0  7.3 257:36.46 java                                                                                                                                      
17972 root      20   0 1536256  96408   5280 S   3.0  1.2   1033:32 cadvisor                                                                                                                                  
61825 root      20   0 3914888 528872  23808 S   2.3  6.6   2:50.38 java                                                                                                                                      
 1500 root      20   0  903528  69696  12736 S   1.3  0.9 173:23.58 dockerd                                                                                                                                   
46014 root      20   0 3999392 678140  15032 S   1.3  8.5 385:20.33 java                                                                                                                                      
    9 root      20   0       0      0      0 S   0.3  0.0  18:00.69 rcu_sched                                                                                                                                 
 1499 influxdb  20   0  778660  78576   3612 S   0.3  1.0 137:08.82 influxd                                                                                                                                   
 1515 telegraf  20   0  323336  13592   3068 S   0.3  0.2  10:29.24 telegraf                                                                                                                                  
 8338 root      20   0 5640840  95996   5400 S   0.3  1.2  42:20.55 java                                                                                                                                      
 9866 root      20   0 3967236 564644  18216 S   0.3  7.1 393:17.53 java                                                                                                                                      
10581 root      20   0 3942564 589292  17848 S   0.3  7.4  87:41.74 java                                                                                                                                      
62610 root      20   0  164104   2300   1556 R   0.3  0.0   0:00.13 top                                                                                                                                       
66816 root      20   0 3998240 681652  16984 S   0.3  8.5 176:53.95 java                                                                                                                                      
    1 root      20   0   52652   4132   2160 S   0.0  0.1   6:30.48 systemd                                                                                                                                   
    2 root      20   0       0      0      0 S   0.0  0.0   0:00.41 kthreadd                                                                                                                                  
    4 root       0 -20       0      0      0 S   0.0  0.0   0:00.00 kworker/0:0H                                                                                                                              
    6 root      20   0       0      0      0 S   0.0  0.0   0:12.62 ksoftirqd/0                                                                                                                               
    7 root      rt   0       0      0      0 S   0.0  0.0   0:06.28 migration/0      
```

1. docker stats 命令查看服务资源占用情况
```bash
CONTAINER ID        NAME                                            CPU %               MEM USAGE / LIMIT     MEM %               NET I/O             BLOCK I/O           PIDS
bc2bd7b62025        lamp-ops                                        0.70%               511.7MiB / 800MiB     63.96%              737kB / 8.3MB       24.3MB / 8.19kB     114
c848ac2a8a9d        lamp-sb                                         95.96%              566.9MiB / 800MiB     70.86%              375MB / 687MB       232MB / 32.4MB      129
9bc2861389a7        lamp-park                                       0.40%               567.5MiB / 800MiB     70.93%              197MB / 301MB       37.8MB / 55.3kB     121
e825b791680c        lamp-wm                                         0.43%               657.4MiB / 800MiB     82.18%              818MB / 1.01GB      50.5MB / 8.47MB     152
975b0f9d5963        cadvisor                                        2.75%               91.03MiB / 7.627GiB   1.17%               133MB / 11.4GB      174MB / 24.6kB      19
51b017a677e8        lamp-common                                     13.57%              658.3MiB / 800MiB     82.29%              346MB / 526MB       15.7MB / 0B         147
104154682c7b        lamp-web-gis-test.5.jomsd227udfz6otvkkcoo9drs   0.10%               52.87MiB / 7.627GiB   0.68%               6.08MB / 8.27MB     877MB / 0B          50
61b3129abe41        my-test.3.0mdrpqdz4b0m7nzio9x2knuwv             0.00%               1.395MiB / 7.627GiB   0.02%               6.37kB / 3.97kB     4.86MB / 0B         2
96112348527a        my-test.1.p20g6bn9ynzppgt96enpjisoy             0.00%               1.395MiB / 7.627GiB   0.02%               8.09kB / 6.07kB     0B / 0B             2
4d51c5f94451        my-test.5.z2ogobo2mlpr6y6i8fmx082yk             0.00%               1.391MiB / 7.627GiB   0.02%               8.81kB / 6.33kB     10.5MB / 0B         2
bb6558397585        my-test.2.017w385xe8gf48yk9z2klemu9             0.00%               1.395MiB / 7.627GiB   0.02%               8.87kB / 6.88kB     0B / 0B             2
5fa372bea1d4        my-test.4.zcb8it4xlnsoj6m83l7g9f2ft             0.00%               1.395MiB / 7.627GiB   0.02%               9.61kB / 6.95kB     0B / 0B             2
7067b2bd3acd        lamp-web-pdu                                    0.40%               541.7MiB / 800MiB     67.71%              1.37GB / 1.74GB     322MB / 983kB       141
42a03ea5713f        prtainer-test                                   0.00%               18.05MiB / 7.627GiB   0.23%               202MB / 26.7MB      137MB / 416MB       13

```

3. docker top [CONTAINER] 命令查看docker容器CPU占用情况
```bash
[root@swarm142 home]# docker top lamp-sb
UID                 PID                 PPID                C                   STIME               TTY                 TIME                CMD
root                26554               26534               0                   11:18               ?                   00:00:04            /usr/bin/coreutils --coreutils-prog-shebang=tail /usr/bin/tail -F /usr/local/apache-tomcat-8.5.39/logs/catalina.out
root                26599               26554               95                  11:18               ?                   04:22:39            /usr/local/jdk1.8.0_191/bin/java -Djava.util.logging.config.file=/usr/local/apache-tomcat-8.5.39/conf/logging.properties -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager -Djdk.tls.ephemeralDHKeySize=2048 -Djava.protocol.handler.pkgs=org.apache.catalina.webresources -Dorg.apache.catalina.security.SecurityListener.UMASK=0027 -Dignore.endorsed.dirs= -classpath /usr/local/apache-tomcat-8.5.39/bin/bootstrap.jar:/usr/local/apache-tomcat-8.5.39/bin/tomcat-juli.jar -Dcatalina.base=/usr/local/apache-tomcat-8.5.39 -Dcatalina.home=/usr/local/apache-tomcat-8.5.39 -Djava.io.tmpdir=/usr/local/apache-tomcat-8.5.39/temp org.apache.catalina.startup.Bootstrap start
root                58726               26534               0                   15:19               pts/0               00:00:00            /bin/bash
[root@swarm142 home]# 

```

发现进程id为26599的进程占用CPU的比例较高

4. docker exec -it [CONTAINER] /bin/bash 命令进入容器查看
```bash
[root@swarm142 home]# docker exec -it lamp-sb /bin/bash
```
5. 容器内使用top命令
```bash
top - 15:57:26 up 21 days, 22:06,  0 users,  load average: 1.76, 1.91, 1.75
Tasks:   5 total,   1 running,   4 sleeping,   0 stopped,   0 zombie
%Cpu(s): 25.8 us,  0.9 sy,  0.0 ni, 73.2 id,  0.0 wa,  0.0 hi,  0.2 si,  0.0 st
MiB Mem :   7809.7 total,    129.0 free,   4980.7 used,   2700.1 buff/cache
MiB Swap:   8192.0 total,   7613.2 free,    578.8 used.   2212.6 avail Mem 

  PID USER      PR  NI    VIRT    RES    SHR S  %CPU  %MEM     TIME+ COMMAND    
   15 root      20   0 3952376 584664  18212 S  90.7   7.3 267:03.34 java       
    1 root      20   0   23012    748    708 S   0.0   0.0   0:04.72 tail       
  449 root      20   0   12124   2220   1664 S   0.0   0.0   0:00.03 bash       
  477 root      20   0   12020   2176   1636 S   0.0   0.0   0:00.02 bash       
  493 root      20   0   48384   2104   1508 R   0.0   0.0   0:00.00 top
```
发现PID为15的java进程占用的CPU比例较高

6. ps -mp [PID] -o THREAD,tid,time 命令获取到[PID]这个进程下面所有线程
```bash
[root@c848ac2a8a9d local]# ps -mp 15 -o THREAD,tid,time
USER     %CPU PRI SCNT WCHAN  USER SYSTEM   TID     TIME
root     95.6   -    - -         -      -     - 04:29:00
root      0.0  19    - futex_    -      -    15 00:00:00
root      0.0  19    - poll_s    -      -    16 00:00:00
root     76.9  19    - -         -      -    17 03:36:15
root      0.0  19    - futex_    -      -    18 00:00:00
root      0.0  19    - futex_    -      -    19 00:00:00
root      0.0  19    - futex_    -      -    20 00:00:00
root      0.3  19    - futex_    -      -    21 00:01:05
root      0.3  19    - futex_    -      -    22 00:01:05
root      0.0  19    - futex_    -      -    23 00:00:14
root      0.0  19    - futex_    -      -    24 00:00:00
root      0.0  19    - futex_    -      -    25 00:00:09
root      0.0  19    - futex_    -      -    26 00:00:00
root      0.0  19    - futex_    -      -    27 00:00:00
root      0.0  19    - ep_pol    -      -    28 00:00:07
...
```
发现TID为17的线程占用CPU较高

7. printf ‘%x\n’ [TID] 命令转换成对应的16进制TID
```bash
[root@c848ac2a8a9d local]# printf '%x\n' 17
11
[root@c848ac2a8a9d local]# 
```
8. jstack [PID] | grep 0x11 -A 30 命令查看异常信息, 其中0x11是上面步骤得到的16进制线程ID
```bash
[root@c848ac2a8a9d local]# jstack 15|grep 0x11 -A 30
"VM Thread" os_prio=0 tid=0x00007f1de806f000 nid=0x11 runnable 

"VM Periodic Task Thread" os_prio=0 tid=0x00007f1de80c3000 nid=0x19 waiting on condition 

JNI global references: 664
```
发现nid=0x11的日志对应VM Thread，说明这个Java程序运行没有什么异常

9. jstack [PID]>stack.dump 命令导出java进程的线程栈信息
10. 线程栈无明显异常，怀疑是内存问题
11. 从第一步中得知，空闲的内存只有154104/1024=150MB，所以怀疑是内存不够导致java程序响应慢
12. jstat -gcutil 15 5s 每隔5s打印java进程的GC状态
```bash
  S0     S1     E      O      M     CCS    YGC     YGCT    FGC    FGCT     GCT   
  0.00 100.00 100.00 100.00  95.94  93.27    638    4.991 32191 27831.346 27836.337
  0.00 100.00 100.00 100.00  95.94  93.27    638    4.991 32193 27833.270 27838.261
  0.00 100.00 100.00 100.00  95.94  93.27    638    4.991 32196 27836.116 27841.107
```
从上面的信息中分析发现，S1（Survivor）、E(Eden)、O(Old)、M(Meta space)分区几乎都是满的，程序一直在执行FGC。所以导致程序响应慢的原因是内存不够了。
