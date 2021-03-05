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

5. JNI相关接口编写

相关Java文件
```java
import android.content.Context;
import android.util.Log;
import com.example.arcare.python.AssetExtractor;
import java.io.File;
import java.nio.charset.StandardCharsets;

public class HandTracking {
    static {
         //加载python3.5m.so文件。该文件需要从crystax-ndk-10.3.2（下载地址：https://www.crystax.net/en/download）
         //里面解压出来，拷贝到工程里
        System.loadLibrary("python3.5m"); 
    }

    public HandTracking(Context mContext) {
        loadPy(mContext);
    }

    //cle调用python代码
    private void loadPy(Context mContext){
        // Extract python files from assets
        AssetExtractor assetExtractor = new AssetExtractor(mContext);
        assetExtractor.removeAssets("python");
        assetExtractor.copyAssets("python");

        // Get the extracted assets directory
        String pyPath = assetExtractor.getAssetsDataDir() + "python";

        // 初始化python时需要stdlib.zip的一个文件
        String stdlibPath = pyPath + File.separator + "stdlib.zip";

        Log.i("CallPython", stdlibPath);

        initPythonInterpreter(stdlibPath);
    }
    public String getGestureResult(int[] key_points, int threshold) {
        return new String(getGesture(key_points, threshold), StandardCharsets.UTF_8);
    }

    private native static int initPythonInterpreter(String stdlibPath);
    private native static int releasePythonInterpreter();
    public native static int gestureRecognize() ;

    private native byte[] getGesture(int[] key_points, int threshold);
}

```

相关CPP文件
```cpp
#include <jni.h>
#include "embedded.h"
int cleanup(wchar_t* program) {
    PyMem_RawFree(program);
    Py_Finalize();
    return -1;
}

jbyteArray to_jbyteArray(JNIEnv *env, std::string result) {
    int byteCount = static_cast<int>(result.length());
    const jbyte *pNativeMessage = reinterpret_cast<const jbyte *>(result.c_str());
    jbyteArray bytes = env->NewByteArray(byteCount);
    env->SetByteArrayRegion(bytes, 0, byteCount, pNativeMessage);
    return bytes;
}
const wchar_t *GetWC(const char *c)
{
    const size_t cSize = strlen(c)+1;
    wchar_t* wc = new wchar_t[cSize];
    mbstowcs (wc, c, cSize);

    return wc;
}

wchar_t* program;

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_example_arcare_detect_HandTracking_getGesture(JNIEnv *env, jclass clazz,
                                                              jintArray key_points,
                                                              jint threshold) {
    jsize size = env->GetArrayLength( key_points );
    std::vector<int> input( size );

    env->GetIntArrayRegion( key_points, 0, size, &input[0] );
    string result = "{}";
    //result = to_string(say_hello_from_python());

    // get_result为embedded.h中的方法
    result = get_result(input, threshold);
    //__android_log_write(ANDROID_LOG_ERROR, "DEMO", result.data());
    return to_jbyteArray(env, result);
}extern "C"
JNIEXPORT jint JNICALL
Java_com_example_arcare_detect_HandTracking_initPythonInterpreter(JNIEnv *env,
                                                                         jclass clazz,
                                                                         jstring stdlib_path) {
    jboolean isCopy;	// 返回JNI_TRUE表示原字符串的拷贝，返回JNI_FALSE表示返回原字符串的指针
    const char *setPath;
    setPath = env->GetStringUTFChars(stdlib_path, &isCopy);

    const wchar_t *sp;
    sp = GetWC(setPath);

    Py_SetPath(sp);

    if (isCopy == JNI_TRUE) {
        env->ReleaseStringUTFChars(stdlib_path, setPath);
    }

    // ---------------
    std::string result = "python init success";

    /*Py_Initialize ();
    Py_Finalize ();*/
    program = Py_DecodeLocale("", NULL);
    if (program == NULL) {
        result = "Py_DecodeLocale failed";
    }

    /* Add a built-in module, before Py_Initialize */
    if (PyImport_AppendInittab("embedded", PyInit_embedded) == -1) {
        result = "Error: could not extend in-built modules table";
    }

    /* Pass argv[0] to the Python interpreter */
    Py_SetProgramName(program);

    /* Initialize the Python interpreter.  Required.
       If this step fails, it will be a fatal error. */
    Py_Initialize();

    /* Optionally import the module; alternatively,
       import can be deferred until the embedded script
       imports it. */
    PyObject* pmodule;
    pmodule = PyImport_ImportModule("embedded");
    if (!pmodule) {
        result = "Error: could not import module 'gesture2'";
        cleanup(program);
        return -1;
    }
    return 0;
}extern "C"
JNIEXPORT jint JNICALL
Java_com_example_arcare_detect_HandTracking_releasePythonInterpreter(JNIEnv *env,
                                                                            jclass clazz) {
    cleanup(program);
    free(program);
    return 0;
}
```