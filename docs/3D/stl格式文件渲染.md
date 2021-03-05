# STL格式文件渲染

STLObject.java文件，实现了STL格式数据的解析，提供了OpenGL绘制方法
```java
package com.example.arcare.easyar.stl;

import ShaderHelper;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glVertexAttribPointer;


/**
 * 解析STL文件 (优化后的), 并使用OpenGL ES 2.0去渲染，渲染方法见{@link #render(GL10, float[])}
 * @author zhaowencong
 *
 */
public class STLObject {
    private static final String VERTEX_SHADER = "attribute vec4 coord;\n" +
            "attribute vec4 a_Color;        \n" +
            "uniform mat4 u_Matrix;         \n" +
            "varying vec4 v_Color;          \n" +
            "void main()                    \n" +
            "{                              \n" +
            "    v_Color = a_Color;         \n" +
            "    gl_Position = u_Matrix * coord;\n" +
            "    gl_PointSize = 10.0;       \n" +
            "}";
    private static final String FRAGMENT_SHADER = "precision mediump float;\n" +
            "varying vec4 v_Color;      \n" +
            "void main()                \n" +
            "{                          \n" +
            "    gl_FragColor = v_Color;\n" +
            "}";
    private byte[] stlBytes = null;
    private IFinishCallBack finishcallback;
    private ProgressDialog progressDialog;
    private Context mContext;
    private boolean loaded = false;

    private float maxX;
    private float maxY;
    private float maxZ;
    private float minX;
    private float minY;
    private float minZ;

    //优化使用的数组
    private  float[] normal_array=null;
    private  float[] vertex_array=null;
    private  float[] color_array= null;
    private  int vertext_size = 0;

    // opengl设置参数
    private FloatBuffer triangleBuffer;
    private FloatBuffer normalBuffer;
    private FloatBuffer colorBuffer;
    private int glProgram;
    private int aPositionLocation;
    private int uMatrixLocation;
    private int aColorLocation;
    private int gl_triangle_buffer;
    private int gl_normal_buffer;
    private int gl_color_buffer;
    private volatile float[] modelMatrix = new float[16];
    private volatile float[] postTransformMatrix = new float[16];

    public interface IFinishCallBack{
        void readFinish();
    }

    public STLObject200(byte[] stlBytes, Context context , IFinishCallBack finishcallback) {
        this.stlBytes = stlBytes;
        this.finishcallback = finishcallback;
        processSTL(stlBytes, context);
        Matrix.setIdentityM(postTransformMatrix, 0);
    }

    private void prepareProgressDialog(Context context) {
        this.mContext = context;
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("STL加载进度");
        progressDialog.setMax(0);
        progressDialog.setMessage("请等一会.");
        progressDialog.setIndeterminate(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void adjustMaxMin(float x, float y, float z) {
        if (x > maxX) {
            maxX = x;
        }
        if (y > maxY) {
            maxY = y;
        }
        if (z > maxZ) {
            maxZ = z;
        }
        if (x < minX) {
            minX = x;
        }
        if (y < minY) {
            minY = y;
        }
        if (z < minZ) {
            minZ = z;
        }
    }

    private int getIntWithLittleEndian(byte[] bytes, int offset) {
        return (0xff & stlBytes[offset]) | ((0xff & stlBytes[offset + 1]) << 8) | ((0xff & stlBytes[offset + 2]) << 16) | ((0xff & stlBytes[offset + 3]) << 24);
    }

    /**
     * checks 'text' in ASCII code
     */
    private boolean isText(byte[] bytes) {
        for (byte b : bytes) {
            if (b == 0x0a || b == 0x0d || b == 0x09) {
                // white spaces
                continue;
            }
            if (b < 0x20 || (0xff & b) >= 0x80) {
                // control codes
                return false;
            }
        }
        return true;
    }

    /**
     * FIXME 'STL format error detection' depends exceptions.
     */
    private boolean processSTL(byte[] stlBytes, final Context context) {
        maxX = Float.MIN_VALUE;
        maxY = Float.MIN_VALUE;
        maxZ = Float.MIN_VALUE;
        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        minZ = Float.MAX_VALUE;

        prepareProgressDialog(context);
        try {
            task.execute(stlBytes);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * 矫正坐标  坐标圆心移动
     */
    private void adjust_coordinate(float[] vertex_array , int postion, float adjust){
        vertex_array[postion]-=adjust;
    }

    /**
     * 渲染前准备OpenGL程序
     */
    public void prepareGL() {
        // 初始化着色器程序
        int vertexShader = ShaderHelper.compileVertexShader(VERTEX_SHADER);
        int fragmentShader = ShaderHelper.compileFragmentShader(FRAGMENT_SHADER);
        glProgram = ShaderHelper.linkProgram(vertexShader, fragmentShader);
        ShaderHelper.validateProgram(glProgram);
        GLES20.glUseProgram(glProgram);

        aPositionLocation = glGetAttribLocation(glProgram, "coord");
        uMatrixLocation = glGetUniformLocation(glProgram, "u_Matrix");
        aColorLocation = glGetAttribLocation(glProgram, "a_Color");
    }
    /**
     * 使用OpenGL渲染
     */
    public void render(GL10 gl, float[] uMatrix) {
        if (triangleBuffer == null) {
            return;
        }
        GLES20.glUseProgram(glProgram);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);  // 要开启深度测试，否则模型背面显示有问题
        GLES20.glClearDepthf(1f);               // 设置深度缓冲区的值，1最大
        GLES20.glDisable(GLES20.GL_CULL_FACE);  // 关闭正反面剔除

        // 计算三维模型的 matrix = uMatrix * modelMatrix，其中modelMatrix是模型到世界坐标系的转换矩阵，
        // uMatrix = viewMatrix * projectMatrix，uMatrix是投影矩阵和视口转换矩阵的乘积
        float[] matrix = new float[16];
        Matrix.setIdentityM(matrix, 0);
        Matrix.multiplyMM(matrix, 0, uMatrix, 0, modelMatrix, 0);
        uMatrix = new float[16];
        Matrix.multiplyMM(uMatrix, 0, postTransformMatrix, 0, matrix, 0);
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, uMatrix, 0);

        // 渲染stl模型的点
        triangleBuffer.position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, gl_triangle_buffer);
        glVertexAttribPointer(aPositionLocation, 3, GL_FLOAT, false, 0, triangleBuffer);
        glEnableVertexAttribArray(aPositionLocation);

        // 将stl模型面的法向量作为颜色进行渲染
        colorBuffer.position(0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, gl_color_buffer);
        glVertexAttribPointer(aColorLocation, 3, GL_FLOAT, false, 0, colorBuffer);
        glEnableVertexAttribArray(aColorLocation);

        glDrawArrays(GL_TRIANGLES, 0, vertext_size * 3);
    }

    public boolean isLoaded() {
        return loaded;
    }

    private int generateOneBuffer() {
        int[] buffer = {0};
        GLES20.glGenBuffers(1, buffer, 0);
        return buffer[0];
    }

    @SuppressLint("StaticFieldLeak")
    private final AsyncTask<byte[], Integer, float[]> task = new AsyncTask<byte[], Integer, float[]>() {

        float[] processText(String stlText) throws Exception {
            String[] stlLines = stlText.split("\n");
            vertext_size=(stlLines.length-2)/7;
            vertex_array=new float[vertext_size*9];
            normal_array=new float[vertext_size*9];
            color_array =new float[vertext_size*9];
            progressDialog.setMax(stlLines.length);

            int normal_num=0;
            int vertex_num=0;
            int color_num =0;
            for (int i = 0; i < stlLines.length; i++) {
                String string = stlLines[i].trim();
                if (string.startsWith("facet normal ")) {
                    string = string.replaceFirst("facet normal ", "");
                    String[] normalValue = string.split(" ");
                    for(int n=0;n<3;n++){
                        normal_array[normal_num++]= Float.parseFloat(normalValue[0]);
                        normal_array[normal_num++]= Float.parseFloat(normalValue[1]);
                        normal_array[normal_num++]= Float.parseFloat(normalValue[2]);

                        color_array[color_num++] = Math.abs(normal_array[normal_num-3]);
                        color_array[color_num++] = Math.abs(normal_array[normal_num-2]);
                        color_array[color_num++] = Math.abs(normal_array[normal_num-1]);
                    }
                }
                if (string.startsWith("vertex ")) {
                    string = string.replaceFirst("vertex ", "");
                    String[] vertexValue = string.split(" ");
                    float x = Float.parseFloat(vertexValue[0]);
                    float y = Float.parseFloat(vertexValue[1]);
                    float z = Float.parseFloat(vertexValue[2]);
                    adjustMaxMin(x, y, z);
                    vertex_array[vertex_num++]=x;
                    vertex_array[vertex_num++]=y;
                    vertex_array[vertex_num++]=z;
                }

                if (i % (stlLines.length / 50) == 0) {
                    publishProgress(i);
                }
            }
            vertext_size=vertex_array.length;
            return vertex_array;
        }

        float[] processBinary(byte[] stlBytes) throws Exception {

            vertext_size=getIntWithLittleEndian(stlBytes, 80);;
            vertex_array=new float[vertext_size*9];
            normal_array=new float[vertext_size*9];
            color_array =new float[vertext_size*9];
            progressDialog.setMax(vertext_size);
            for (int i = 0; i < vertext_size; i++) {
                for(int n=0;n<3;n++){
                    float x = Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50));
                    normal_array[i*9+n*3]= x;
                    color_array [i*9+n*3]= Math.abs(x);
                    float y = Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 4));
                    normal_array[i*9+n*3+1]= y;
                    color_array[i*9+n*3+1]= Math.abs(y);
                    float z = Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 8));
                    normal_array[i*9+n*3+2]= z;
                    color_array[i*9+n*3+2]= Math.abs(z);
                }
                float x = Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 12));
                float y = Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 16));
                float z = Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 20));
                adjustMaxMin(x, y, z);
                vertex_array[i*9]=x;
                vertex_array[i*9+1]=y;
                vertex_array[i*9+2]=z;

                x = Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 24));
                y = Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 28));
                z = Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 32));
                adjustMaxMin(x, y, z);
                vertex_array[i*9+3]=x;
                vertex_array[i*9+4]=y;
                vertex_array[i*9+5]=z;

                x = Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 36));
                y = Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 40));
                z = Float.intBitsToFloat(getIntWithLittleEndian(stlBytes, 84 + i * 50 + 44));
                adjustMaxMin(x, y, z);
                vertex_array[i*9+6]=x;
                vertex_array[i*9+7]=y;
                vertex_array[i*9+8]=z;

                if (i % (vertext_size / 50) == 0) {
                    publishProgress(i);
                }
            }

            return vertex_array;
        }

        @Override
        protected float[] doInBackground(byte[]... stlBytes) {
            float[]processResult = null;
            try {
                if (isText(stlBytes[0])) {
                    // Log.i("trying text...");
                    processResult = processText(new String(stlBytes[0]));
                } else {
                    //	Log.i("trying binary...");
                    processResult = processBinary(stlBytes[0]);
                }
            } catch (Exception e) {
            }
            if (processResult != null && processResult.length > 0 && normal_array != null && normal_array.length > 0) {
                return processResult;
            }

            return processResult;
        }

        @Override
        public void onProgressUpdate(Integer... values) {
            progressDialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(float[] vertexList) {
            if (normal_array.length < 1 || vertex_array.length < 1) {
                Toast.makeText(mContext, "STL加载失败", Toast.LENGTH_LONG).show();

                progressDialog.dismiss();
                return;
            }

            ByteBuffer color_buffer = ByteBuffer.allocateDirect(normal_array.length * 4);
            color_buffer.order(ByteOrder.nativeOrder());
            colorBuffer = color_buffer.asFloatBuffer();
            colorBuffer.put(color_array);
            colorBuffer.position(0);

            ByteBuffer normal = ByteBuffer.allocateDirect(normal_array.length * 4);
            normal.order(ByteOrder.nativeOrder());
            normalBuffer = normal.asFloatBuffer();
            normalBuffer.put(normal_array);
            normalBuffer.position(0);

            //=================矫正中心店坐标========================
            float center_x=(maxX+minX)/2;
            float center_y=(maxY+minY)/2;
            float center_z=(maxZ+minZ)/2;

            for(int i=0;i<vertext_size*3;i++){
                adjust_coordinate(vertex_array,i*3,center_x);
                adjust_coordinate(vertex_array,i*3+1,center_y);
                adjust_coordinate(vertex_array,i*3+2,center_z);
            }


            ByteBuffer vbb = ByteBuffer.allocateDirect(vertex_array.length * 4);
            vbb.order(ByteOrder.nativeOrder());
            triangleBuffer = vbb.asFloatBuffer();
            triangleBuffer.put(vertex_array);
            triangleBuffer.position(0);

            gl_triangle_buffer = generateOneBuffer();
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, gl_triangle_buffer);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertex_array.length * 4, triangleBuffer, GLES20.GL_DYNAMIC_DRAW);

            gl_normal_buffer = generateOneBuffer();
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, gl_normal_buffer);
            normalBuffer.position(0);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, normal_array.length * 4, normalBuffer, GLES20.GL_DYNAMIC_DRAW);

            gl_color_buffer = generateOneBuffer();
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, gl_color_buffer);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, color_array.length * 4, colorBuffer, GLES20.GL_DYNAMIC_DRAW);

            // 计算模型转换矩阵：缩放到归一化设备坐标系
            float scaleX = maxX - minX;
            float scaleY = maxY - minY;
            float scaleZ = maxZ - minZ;

            float scale = 0.5f / Math.max(Math.max(scaleX, scaleY), scaleZ);
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.scaleM(modelMatrix, 0, scale, scale, scale);

            finishcallback.readFinish();

            progressDialog.dismiss();
            stlBytes = null;
            loaded = true;
        }
    };

}

```


STLRender.java文件，GLTextureView.Renderer的实现类
```java
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.example.arcare.easyar.GLTextureView;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.opengles.GL10;

public class STLRender implements GLTextureView.Renderer {
    private STLObject200 stlObject;
    private float[] glPoseMatrix;       // TODO: 构造合适的View Transformation矩阵
    private float[] glProjectionMatrix; // TODO：构造合适的Projection Transformation矩阵
    private float[] modelAndViewMatrix = new float[16];
    private EGL10 egl;
    public STLRender200(STLObject200 stlObject) {
        this.stlObject = stlObject;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.egl = (EGL10)EGLContext.getEGL();
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        this.egl = (EGL10)EGLContext.getEGL();
        this.stlObject.prepareGL();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        renderStlObject(gl);
    }

    private void renderStlObject(GL10 gl) {
        Matrix.multiplyMM(this.modelAndViewMatrix, 0, this.glProjectionMatrix, 0, this.glPoseMatrix, 0);
        stlObject.render(gl, this.modelAndViewMatrix);
    }
}

```

最后是ShaderHelper工具类
```java
/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
***/

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_VALIDATE_STATUS;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glValidateProgram;
import android.util.Log;

public class ShaderHelper {
    private static final String TAG = "ShaderHelper";

    /**
     * Loads and compiles a vertex shader, returning the OpenGL object ID.
     */
    public static int compileVertexShader(String shaderCode) {
        return compileShader(GL_VERTEX_SHADER, shaderCode);
    }

    /**
     * Loads and compiles a fragment shader, returning the OpenGL object ID.
     */
    public static int compileFragmentShader(String shaderCode) {
        return compileShader(GL_FRAGMENT_SHADER, shaderCode);
    }

    /**
     * Compiles a shader, returning the OpenGL object ID.
     */
    private static int compileShader(int type, String shaderCode) {        
        // Create a new shader object.
        final int shaderObjectId = glCreateShader(type);

        if (shaderObjectId == 0) {
            if (LoggerConfig.ON) {
                Log.w(TAG, "Could not create new shader.");
            }

            return 0;
        }       
        
        // Pass in the shader source.
        glShaderSource(shaderObjectId, shaderCode);
        
        // Compile the shader.
        glCompileShader(shaderObjectId);
        
        // Get the compilation status.
        final int[] compileStatus = new int[1];
        glGetShaderiv(shaderObjectId, GL_COMPILE_STATUS,
            compileStatus, 0);

        if (LoggerConfig.ON) {
            // Print the shader info log to the Android log output.
            Log.v(TAG, "Results of compiling source:" 
                + "\n" + shaderCode + "\n:" 
                + glGetShaderInfoLog(shaderObjectId));
        }
        
        // Verify the compile status.
        if (compileStatus[0] == 0) {
            // If it failed, delete the shader object.
            glDeleteShader(shaderObjectId);

            if (LoggerConfig.ON) {
                Log.w(TAG, "Compilation of shader failed.");
            }

            return 0;
        }
        
        // Return the shader object ID.
        return shaderObjectId;
    }
 
    /**
     * Links a vertex shader and a fragment shader together into an OpenGL
     * program. Returns the OpenGL program object ID, or 0 if linking failed.
     */
    public static int linkProgram(int vertexShaderId, int fragmentShaderId) {        
        // Create a new program object.
        final int programObjectId = glCreateProgram();

        if (programObjectId == 0) {
            if (LoggerConfig.ON) {
                Log.w(TAG, "Could not create new program");
            }

            return 0;
        }
        
        // Attach the vertex shader to the program.
        glAttachShader(programObjectId, vertexShaderId);

        // Attach the fragment shader to the program.
        glAttachShader(programObjectId, fragmentShaderId);        
        
        // Link the two shaders together into a program.
        glLinkProgram(programObjectId);
        
        // Get the link status.
        final int[] linkStatus = new int[1];
        glGetProgramiv(programObjectId, GL_LINK_STATUS,
            linkStatus, 0);

        if (LoggerConfig.ON) {
            // Print the program info log to the Android log output.
            Log.v(TAG, "Results of linking program:\n"
                + glGetProgramInfoLog(programObjectId));         
        }
        
        // Verify the link status.
        if (linkStatus[0] == 0) {
            // If it failed, delete the program object.
            glDeleteProgram(programObjectId);

            if (LoggerConfig.ON) {
                Log.w(TAG, "Linking of program failed.");
            }

            return 0;
        }
        
        // Return the program object ID.
        return programObjectId;
    }

    /**
     * Validates an OpenGL program. Should only be called when developing the
     * application.
     */
    public static boolean validateProgram(int programObjectId) {
        glValidateProgram(programObjectId);
        final int[] validateStatus = new int[1];
        glGetProgramiv(programObjectId, GL_VALIDATE_STATUS,
            validateStatus, 0);
        Log.v(TAG, "Results of validating program: " + validateStatus[0]
            + "\nLog:" + glGetProgramInfoLog(programObjectId));

        return validateStatus[0] != 0;
    }
}
```