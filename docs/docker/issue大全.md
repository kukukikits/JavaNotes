# 如何设置proxy代理？
在windows系统的cmd命令行里设置HTTP_PROXY和HTTPS_PROXY环境变量
```sh
set HTTP_PROXY=http://127.0.0.1:8003
set HTTPS_PROXY=http://127.0.0.1:8003
```

在Linux系统中设置代理环境变量：
```sh
export http_proxy=http://127.0.0.1:8003
export https_proxy=http://127.0.0.1:8003
```
代理服务器从github下载cow