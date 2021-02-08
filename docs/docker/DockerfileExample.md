


### Opencv Dockerfile Example
openjdk8-opencv文件
```docker
FROM alpine:latest

# 下载并编译opencv、下载openjdk8并设置环境变量
RUN apk update \
    && apk add --no-cache linux-headers g++ gcc make cmake git pkgconf ffmpeg freetype libdc1394 libgcc \
    libpng libstdc++ mesa-gl musl openexr openjpeg qt5-qtbase qt5-qtbase-x11 zlib openjdk8\
    && mkdir /git \
    && cd /git \
        && git clone -b 3.4 https://gitee.com/mirrors/opencv.git \
        && cd opencv \
        && mkdir build \
        && cd build \
        && cmake .. -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=/usr/local \
            -DBUILD_EXAMPLES=OFF -DBUILD_TESTS=OFF -DBUILD_PERF_TESTS=OFF -DBUILD_opencv_world=ON \
        && make -j8 \
        && make install \
        && rm -rf /git
ENV JAVA_HOME=/usr/lib/jvm/java-1.8-openjdk
ENV PATH=$JAVA_HOME/bin:$PATH
ENV LD_LIBRARY_PATH=/usr/lib:/usr/lib64:/usr/local:/usr/local/lib:/usr/local/lib64:/lib:/lib64:$LD_LIBRARY_PATH

```

### 另一个例子
```docker
FROM registry.com/xx/openjdk8-opencv:latest
# 不推荐将用户名和密码以参数形式传递进来
ARG gituser
ARG gitpassword

# 由于gitlab上docker in docker的问题，xx的依赖必须手动构建
# 1. 手动构建xx的依赖镜像, 构建镜像命令：
#    docker build --build-arg gituser=你的GIT账户 --build-arg gitpassword=你的GIT密码 --tag registry.com/xx/xx-dependencies:latest --file DockerfileXXDependencies .
#    如果密码中有“@”符号，使用“%40”替换
# 2. 构建镜像完成后，上传到registry.com镜像仓库：
#    docker push registry.com/xx/xx-dependencies:latest

RUN mkdir /git \
       && cd /git \
       && git clone https://${gituser}:${gitpassword}@git.x.com/xx/SeetaFace2.git \
       && cd SeetaFace2 \
       && mkdir build \
       && cd build \
       && cmake .. -DCMAKE_INSTALL_PREFIX=/usr/local -DCMAKE_BUILD_TYPE=Release -DBUILD_EXAMPLE=OFF \
       && cmake --build . --config Release \
       && cmake --build . --config Release --target install/strip

COPY /face-recognition /face-recognition
RUN cd /face-recognition/src/main/cpp \
    && rm -rf build \
    && mkdir build \
    && cd build \
    && cmake .. -DCMAKE_INSTALL_PREFIX=/usr/local -DCMAKE_BUILD_TYPE=Release \
    && make -j8 \
    && make install \
    && rm -rf /face-recognition \
    && rm -rf /git

```

### 另一个例子
```docker
FROM registry.d.com/ar-glass/ar-server-dependencies:latest
VOLUME ["/tmp", "/log", "/data", "/jniLibs"]
#ARG gituser
#ARG gitpassword
# 编译SeetaFace2动态链接库
# 使用gitlab自动化编译时出错：failed to set OOM Score on shim: write /proc/286/oom_score_adj: permission denied: unknown
# https://github.com/lxc/lxd/issues/2994 上的解释是：
# It's a hard kernel limit that unprivileged containers may not set an oom score higher than the current value.
# Otherwise any user could create a user namespace and make unkillable processes.
# 所以c++依赖库我们单独构建镜像
# RUN mkdir /git \
#        && cd /git \
#        && git clone https://${gituser}:${gitpassword}@git.d.com/ar-research/SeetaFace2.git \
#        && cd SeetaFace2 \
#        && mkdir build \
#        && cd build \
#        && cmake .. -DCMAKE_INSTALL_PREFIX=/usr/local -DCMAKE_BUILD_TYPE=Release -DBUILD_EXAMPLE=OFF \
#        && cmake --build . --config Release \
#        && cmake --build . --config Release --target install/strip

# 编译本项目动态链接库
# 同样地，本项目的C++模块也不能在gitlab的自动化构建中成功执行，所以每次修改代码后必须要手动先构建镜像并上传registry.d.com仓库
# 再更新FROM基础镜像
#COPY /face-recognition /face-recognition
#RUN cd /face-recognition/src/main/cpp \
#    && rm -rf build \
#    && mkdir build \
#    && cd build \
#    && cmake .. -DCMAKE_INSTALL_PREFIX=/usr/local -DCMAKE_BUILD_TYPE=Release \
#    && make -j8 \
#    && make install \
#    && rm -rf /face-recognition \
#    && rm -rf /git

COPY /ar-server/target/ar-server-*.jar app.jar

LABEL maintainer="geshengbin@cetiti.com" \
      name="CETITI AR-glass web server"
ENV FILE_STORAGE_PATH=${FILE_STORAGE_PATH:-/data}

# 人脸识别模型已经打包进registry.d.com/ar-glass/java-seetaface2:latest镜像的jniLibs文件夹下面了，可以直接拿来用
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", \
 "-jar","/app.jar", "--face.engine.recognize-model-file=/jniLibs/fr_2_10.dat", \
 "--face.engine.detect-model-file=/jniLibs/fd_2_00.dat", "--face.engine.marker-model-file=/jniLibs/pd_2_00_pts5.dat"]
```