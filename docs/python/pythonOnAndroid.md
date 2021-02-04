# python 在 安卓平台上运行
本文只描述python代码没有第三方依赖包的情况下，在安卓上运行。
整体思路是：python --> cython --> c++ --> JNI调用
下面以例子的形式进行说明：

1. 使用cython语法编写python脚本
在embedded.pyx文件中输入如下代码
```python
# 可以在注释里定义cython的编译指令
# distutils: language=c++
# cython: language_level=3

# 引入C++类
from libcpp.vector cimport vector
from libcpp.string cimport string

# 引入python自带的模块
import json

# 定义C++公开方法，该方法会被cython编译并生成对应头文件里的方法
cdef public int say_hello_from_python() except -1:
    print("Hello from Python")
    return 20

# C++中的vector可以自动转为python中的List
cdef public string get_result(vector[float] key_points, int min_threshold):
    result = {}

    key_point_list = []
    i = 0

    for num in range(22):
        key_point_list.append([])
        key_point_list[num].append(key_points[i])
        key_point_list[num].append(key_points[i+1])
        i += 2

    result["key_points"] = key_point_list
    return json.dumps(result).encode('utf-8')

```

2. 编写脚本文件setup.py
```python
from distutils.core import setup
from distutils.extension import Extension
from Cython.Build import cythonize
import numpy
from Cython.Compiler import Options

# 在C++函数里调用cython编译embedded.pyx后的C++代码时，需要指定这个embed选项
Options.embed = "main"

# cythonize("embedded.pyx")： 编译embedded.pyx
# include_dirs = [numpy.get_include()]： 设置numpy第三方库的头文件目
# 录，这个在Android上目前还没找到对应CPU架构的numpy库的so文件，因此在
# Android上使用第三方库时会很麻烦，目前没有找到相应的方法
setup(
        ext_modules = cythonize("embedded.pyx"),
        include_dirs = [numpy.get_include()]
        )

```

3. 执行命令开始编译
执行如下编译命令：
```shell
python setup.py build_ext --inplace
```

4. 再执行
这个命令不知道有什么作用，暂且先记着。不知道是不是必须的
```shell
cython --embed -o embedded.cpp embedded.pyx
```