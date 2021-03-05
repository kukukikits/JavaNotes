# SPAAM标定

本标定方法集成了EasyAR SDK，使用该SDK对AR眼镜进行单眼的标定，用于纠正眼睛看到的画面和摄像头看到的画面的视差。

下面是SPAAM标定的工具类，是https://github.com/krm104/AndroidSPAAM项目中的
```java
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import Jama.Matrix;

//Import needed for the Linear Algebra and Matrix related math functions//

/*******************************************************************************
 * This class provides an interface and related sub classes for recording
 * 2D - 3D   screen - world correspondence pairs needed for the SPAAM calibration.
 *
 * It also provides a class for storing a list of correspondence pairs and then performing
 * the SVD calculation on those pairs.
 *******************************************************************************/
public class SpaamUtil {

    /***************************************************************************
     * This class is the interface by which a list of correspondence pairs can be
     * recorded and the SVD calculations performed on those pairs. The list
     * corr_points stores all of the screen-world alignment pairs and the function
     * projectionDLTImpl( ) performs the SVD calculations producing the final
     * 3x4 projection Matrix.
     **************************************************************************/
    static public class SPAAM_SVD{

        //Default Constructor that does nothing//
        public SPAAM_SVD()
        {			}

        /********************************************************************
         * This class is used to easily store a 2D Pixel and 3D world point
         * correspondence pair. This makes it easy to keep related values
         * for each pair together.
         *******************************************************************/
        static public class Correspondence_Pair {
            public Correspondence_Pair( )
            {  }

            public Correspondence_Pair( double x1, double y1, double z1, double x2, double y2 )
            { worldPoint.set(0, 0, x1); worldPoint.set(0, 1, y1); worldPoint.set(0, 2, z1);
                screenPoint.set(0, 0, x2); screenPoint.set(0, 1, y2); }

            //Correspondence Points//
            public Matrix worldPoint = new Matrix(1, 3);
            public Matrix screenPoint = new Matrix(1, 2);

            @Override
            public String toString() {
                try(StringWriter stringWriter = new StringWriter();
                    PrintWriter writer = new PrintWriter(stringWriter, true)) {
                    writer.write(" worldPoint: ");
                    worldPoint.print(writer, 4, 4);
                    writer.write("\n screenPoint: ");
                    screenPoint.print(writer, 4, 4);
                    writer.flush();
                    return stringWriter.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return "Correspondence_Pair{" +
                        "worldPoint=" + worldPoint +
                        ", screenPoint=" + screenPoint +
                        '}';
            }
        }

        /////////////////////////////////////////////////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////////////////////////////
        ////Correspondence Points////
        public List<Correspondence_Pair> corr_points = new ArrayList<Correspondence_Pair>();

        ////Normalization Components for World Points////
        private Matrix fromShift = new Matrix(1, 3);
        private Matrix fromScale = new Matrix(1, 3);
        ////Normalization Components for Screen Points////
        private Matrix toShift = new Matrix(1, 2);
        private Matrix toScale = new Matrix(1, 2);
        ////Normalization Matrix for World Points////
        private Matrix modMatrixWorld = new Matrix(4, 4);
        ////Normalization Matrix for Screen Points////
        private Matrix modMatrixScreen = new Matrix(3, 3);

        ////Final 3 x 4 Projection Matrix////
        public Matrix Proj3x4 = new Matrix(3, 4);
        public double[] projMat4x4 = {0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0};

        ///////////////////////////////////////////////////////////////////////////////////////////////

        //A helper function to perform an element wise divide of 2 matrices (or vectors)
        private Matrix element_div(Matrix m1, Matrix m2)
        {
            Matrix result = null;
            if ( m1.getColumnDimension() == m1.getColumnDimension() )
                if ( m1.getRowDimension() == m2.getRowDimension() )
                {
                    result = new Matrix(m1.getRowDimension(), m1.getColumnDimension());
                    for ( int i = 0; i < m1.getRowDimension(); i++)
                        for( int j = 0; j < m1.getColumnDimension(); j++ )
                        {
                            result.set(i,  j, m1.get(i,  j)/m2.get(i,  j));
                        }
                }

            return result;
        }
        ///////////////////////////////////////////////////////////////////////////////////////////////

        //This is a normalization function that normalizes all of the 2D and 3D correpsondence values//
        //Normalization is required since the 2D and 3D point values come over different ranges of values//
        private void estimateNormalizationParameters( )
        {
            ////determine the number of points to be normalized////
            double n_pts = corr_points.size();

            ////start with all 0's////
            fromShift = new Matrix(1, 3);
            fromScale = new Matrix(1, 3);
            toShift = new Matrix(1, 2);
            toScale = new Matrix(1, 2);

            ////compute mean and mean of square////
            for ( int i = 0; i < corr_points.size(); ++i ){
                fromShift = fromShift.plus(corr_points.get(i).worldPoint);
                Matrix tempscale = new Matrix(1, 3);
                tempscale.set(0, 0, corr_points.get(i).worldPoint.get(0, 0)*corr_points.get(i).worldPoint.get(0, 0));
                tempscale.set(0, 1, corr_points.get(i).worldPoint.get(0, 1)*corr_points.get(i).worldPoint.get(0, 1));
                tempscale.set(0, 2, corr_points.get(i).worldPoint.get(0, 2)*corr_points.get(i).worldPoint.get(0, 2));
                fromScale = fromScale.plus(tempscale);

                toShift = toShift.plus(corr_points.get(i).screenPoint);
                Matrix temptscale = new Matrix(1, 2);
                temptscale.set(0, 0, corr_points.get(i).screenPoint.get(0, 0)*corr_points.get(i).screenPoint.get(0, 0));
                temptscale.set(0, 1, corr_points.get(i).screenPoint.get(0, 1)*corr_points.get(i).screenPoint.get(0, 1));
                toScale = toScale.plus(temptscale);
            }
            fromShift = fromShift.times(( 1.0 ) / n_pts);
            fromScale = fromScale.times(( 1.0 ) / n_pts);
            toShift = toShift.times(( 1.0 ) / n_pts);
            toScale = toScale.times(( 1.0 ) / n_pts);

            ////compute standard deviation////
            for ( int i = 0; i < 3; i++ ){
                fromScale.set(0, i, Math.sqrt( fromScale.get(0, i ) - ( fromShift.get(0, i ) * fromShift.get(0, i ) ) ));
            }
            for ( int i = 0; i < 2; i++ ){
                toScale.set(0, i, Math.sqrt( toScale.get(0, i ) - ( toShift.get(0, i ) * toShift.get(0, i )) ) );
            }
            ////end of function////
        }

        //This function produces the correction values needed to transform the result of the SVD function//
        //back into the proper range of values//
        private void generateNormalizationMatrix( )
        {
            ////compute correction matrix////
            ////Start with All 0's////
            modMatrixWorld = new Matrix(4, 4);
            modMatrixScreen = new Matrix(3, 3);

            ////create homogeneous matrix////
            modMatrixWorld.set( 3, 3, 1.0 );
            modMatrixScreen.set( 2, 2, 1.0 );

            ////honestly I'm not sure what this is for////
            //if ( true )
            {
                for ( int i = 0; i < 2; i++ )
                {
                    modMatrixScreen.set( i, i, toScale.get(0, i ));
                    modMatrixScreen.set( i, 2, toShift.get(0, i ));
                }
            }//else
            {
                for ( int i = 0; i < 3; i++ )
                {
                    modMatrixWorld.set( i, i, ( 1.0 ) / fromScale.get(0, i ));
                    modMatrixWorld.set( i, 3, -modMatrixWorld.get( i, i ) * fromShift.get(0, i ));
                }
            }
        }

        //This function should be called to perform the Singular Value Decomposition operation//
        //on the correspondence pairs in the corr_points list. The result is the 3x4 projection matrix//
        //stored in the Proj3x4 object.//
        public boolean projectionDLTImpl( )
        {
            ////minimum of 6 correspondence points required to solve////
            if( corr_points.size() < 6 )
                return false;

            // normalize input points
            estimateNormalizationParameters( );

            // construct equation system
            Matrix A = new Matrix(2*corr_points.size(), 12);

            for ( int i = 0; i < corr_points.size(); i++ )
            {
                Matrix to = element_div( corr_points.get(i).screenPoint.minus(toShift), toScale );
                Matrix from = element_div( corr_points.get(i).worldPoint.minus(fromShift), fromScale );

                A.set( i * 2,  0, 0 );
                A.set( i * 2, 1, 0 );
                A.set( i * 2, 2, 0 );
                A.set( i * 2, 3, 0 );
                A.set( i * 2,  4, -from.get(0, 0 ));
                A.set( i * 2,  5, -from.get(0, 1 ));
                A.set( i * 2,  6, -from.get(0, 2 ));
                A.set( i * 2,  7, -1);
                A.set( i * 2,  8, to.get(0, 1 ) * from.get(0, 0 ));
                A.set( i * 2,  9, to.get(0, 1 ) * from.get(0, 1 ));
                A.set( i * 2, 10, to.get(0, 1 ) * from.get(0, 2 ));
                A.set( i * 2, 11, to.get(0, 1 ));
                A.set( i * 2 + 1,  0, from.get(0, 0 ));
                A.set( i * 2 + 1,  1, from.get(0, 1 ));
                A.set( i * 2 + 1,  2, from.get(0, 2 ));
                A.set( i * 2 + 1,  3, 1);
                A.set( i * 2 + 1,  4, 0);
                A.set( i * 2 + 1, 5, 0 );
                A.set( i * 2 + 1, 6, 0 );
                A.set( i * 2 + 1, 7, 0);
                A.set( i * 2 + 1,  8, -to.get(0, 0 ) * from.get(0, 0 ));
                A.set( i * 2 + 1,  9, -to.get(0, 0 ) * from.get(0, 1 ));
                A.set( i * 2 + 1, 10, -to.get(0, 0 ) * from.get(0, 2 ));
                A.set( i * 2 + 1, 11, -to.get(0, 0 ));
            }

            // solve using SVD
            //Matrix s = new Matrix(1, 12);
            //Matrix U = new Matrix( 2 * corr_points.size(), 2 * corr_points.size() );
            Matrix Vt = new Matrix(12, 12);
            Vt = A.svd().getV().transpose();

            // copy result to 3x4 matrix
            Proj3x4.set( 0, 0, Vt.get( 11, 0 )); Proj3x4.set( 0, 1, Vt.get( 11, 1 )); Proj3x4.set( 0, 2, Vt.get( 11,  2 )); Proj3x4.set( 0, 3, Vt.get( 11,  3 ));
            Proj3x4.set( 1, 0, Vt.get( 11, 4 )); Proj3x4.set( 1, 1, Vt.get( 11, 5 )); Proj3x4.set( 1, 2, Vt.get( 11,  6 )); Proj3x4.set( 1, 3, Vt.get( 11,  7 ));
            Proj3x4.set( 2, 0, Vt.get( 11, 8 )); Proj3x4.set( 2, 1, Vt.get( 11, 9 )); Proj3x4.set( 2, 2, Vt.get( 11, 10 )); Proj3x4.set( 2, 3, Vt.get( 11, 11 ));

            // reverse normalization
            generateNormalizationMatrix( );
            Matrix toCorrect = new Matrix(( modMatrixScreen.getArray() ));
            Matrix Ptemp = new Matrix(3, 4); Ptemp = toCorrect.times(Proj3x4);
            Matrix fromCorrect = new Matrix(( modMatrixWorld.getArray() ));
            Proj3x4 = Ptemp.times(fromCorrect);

            // normalize result to have a viewing direction of length 1 (optional)
            double fViewDirLen = Math.sqrt( Proj3x4.get( 2, 0 ) * Proj3x4.get( 2, 0 ) + Proj3x4.get( 2, 1 ) * Proj3x4.get( 2, 1 ) + Proj3x4.get( 2, 2 ) * Proj3x4.get( 2, 2 ) );

            // if first point is projected onto a negative z value, negate matrix
            Matrix p1st = new Matrix(corr_points.get(0).worldPoint.getArray());
            if ( Proj3x4.get( 2, 0 ) * p1st.get(0, 0 ) + Proj3x4.get( 2, 1 ) * p1st.get(0, 1 ) + Proj3x4.get( 2, 2 ) * p1st.get(0, 2 ) + Proj3x4.get( 2, 3 ) < 0 )
                fViewDirLen = -fViewDirLen;

            Proj3x4 = Proj3x4.times(( 1.0 ) / fViewDirLen);

            Proj3x4.print(0,  3);

            return true;
        }

        //This function transforms the 3x4 projection matrix produced by the SVD operation into a//
        //4x4 matrix matrix usable by OpenGL. The parameters are the near, far clip planes, and screen resolution//
        public void BuildGLMatrix3x4(double ne, double fr, int right, int left, int top, int bottom){
            projMat4x4[0] = Proj3x4.get(0, 0); projMat4x4[1] = Proj3x4.get(0, 1); projMat4x4[2] = Proj3x4.get(0, 2); projMat4x4[3] = Proj3x4.get(0, 3);
            projMat4x4[4] = Proj3x4.get(1, 0); projMat4x4[5] = Proj3x4.get(1, 1); projMat4x4[6] = Proj3x4.get(1, 2); projMat4x4[7] = Proj3x4.get(1, 3);
            projMat4x4[8] = Proj3x4.get(2, 0); projMat4x4[9] = Proj3x4.get(2, 1); projMat4x4[10] = Proj3x4.get(2, 2); projMat4x4[11] = Proj3x4.get(2, 3);

            constructProjectionMatrix4x4_( ne, fr, right, left, top, bottom);
        }

        //This function creates an orthogonal matrix that is then multiplied by the 3x4 SPAAM result//
        //creating a 4x4 matrix (column major order) usable by OpenGL//
        private void constructProjectionMatrix4x4_( double ne, double fr, int right, int left, int top, int bottom)
        {
            double[] proj4x4 = new double[16];

            //Copy base 3x4 values//
            System.arraycopy(projMat4x4, 0, proj4x4, 0, 12);
            //Duplicate third row into the fourth//
            System.arraycopy(projMat4x4, 8, proj4x4, 12, 4);

            //calculate extra parameters//
            double norm = Math.sqrt(proj4x4[8] * proj4x4[8] + proj4x4[9] * proj4x4[9] + proj4x4[10] * proj4x4[10]);
            double add = fr*ne*norm;

            //Begin adjusting the 3x4 values for 4x4 use//
            proj4x4[8] *= (-fr - ne);
            proj4x4[9] *= (-fr - ne);
            proj4x4[10] *= (-fr - ne);
            proj4x4[11] *= (-fr - ne);
            proj4x4[11] += add;

            //Create Orthographic projection matrix//
            double[] ortho = new double[16];
            ortho[0] = 2.0f / (right - left);
            ortho[1] = 0.0f;
            ortho[2] = 0.0f;
            ortho[3] = (right + left)*1.0 / (left - right);
            ortho[4] = 0.0f;
            ortho[5] = 2.0f / (top - bottom);
            ortho[6] = 0.0f;
            ortho[7] = (top + bottom)*1.0 / (bottom - top);
            ortho[8] = 0.0f;
            ortho[9] = 0.0f;
            ortho[10] = 2.0f / (ne - fr);
            ortho[11] = (fr + ne) / (ne - fr);
            ortho[12] = 0.0f;
            ortho[13] = 0.0f;
            ortho[14] = 0.0f;
            ortho[15] = 1.0f;

            //Multiply the 4x4 projection by the orthographic projection//
            projMat4x4[0] = ortho[0]*proj4x4[0] + ortho[1]*proj4x4[4] + ortho[2]*proj4x4[8] + ortho[3]*proj4x4[12];
            projMat4x4[1] = ortho[0]*proj4x4[1] + ortho[1]*proj4x4[5] + ortho[2]*proj4x4[9] + ortho[3]*proj4x4[13];
            projMat4x4[2] = ortho[0]*proj4x4[2] + ortho[1]*proj4x4[6] + ortho[2]*proj4x4[10] + ortho[3]*proj4x4[14];
            projMat4x4[3] = ortho[0]*proj4x4[3] + ortho[1]*proj4x4[7] + ortho[2]*proj4x4[11] + ortho[3]*proj4x4[15];

            projMat4x4[4] = ortho[4]*proj4x4[0] + ortho[5]*proj4x4[4] + ortho[6]*proj4x4[8] + ortho[7]*proj4x4[12];
            projMat4x4[5] = ortho[4]*proj4x4[1] + ortho[5]*proj4x4[5] + ortho[6]*proj4x4[9] + ortho[7]*proj4x4[13];
            projMat4x4[6] = ortho[4]*proj4x4[2] + ortho[5]*proj4x4[6] + ortho[6]*proj4x4[10] + ortho[7]*proj4x4[14];
            projMat4x4[7] = ortho[4]*proj4x4[3] + ortho[5]*proj4x4[7] + ortho[6]*proj4x4[11] + ortho[7]*proj4x4[15];

            projMat4x4[8] = ortho[8]*proj4x4[0] + ortho[9]*proj4x4[4] + ortho[10]*proj4x4[8] + ortho[11]*proj4x4[12];
            projMat4x4[9] = ortho[8]*proj4x4[1] + ortho[9]*proj4x4[5] + ortho[10]*proj4x4[9] + ortho[11]*proj4x4[13];
            projMat4x4[10] = ortho[8]*proj4x4[2] + ortho[9]*proj4x4[6] + ortho[10]*proj4x4[10] + ortho[11]*proj4x4[14];
            projMat4x4[11] = ortho[8]*proj4x4[3] + ortho[9]*proj4x4[7] + ortho[10]*proj4x4[11] + ortho[11]*proj4x4[15];

            projMat4x4[12] = ortho[12]*proj4x4[0] + ortho[13]*proj4x4[4] + ortho[14]*proj4x4[8] + ortho[15]*proj4x4[12];
            projMat4x4[13] = ortho[12]*proj4x4[1] + ortho[13]*proj4x4[5] + ortho[14]*proj4x4[9] + ortho[15]*proj4x4[13];
            projMat4x4[14] = ortho[12]*proj4x4[2] + ortho[13]*proj4x4[6] + ortho[14]*proj4x4[10] + ortho[15]*proj4x4[14];
            projMat4x4[15] = ortho[12]*proj4x4[3] + ortho[13]*proj4x4[7] + ortho[14]*proj4x4[11] + ortho[15]*proj4x4[15];

            proj4x4[0] = projMat4x4[0]; proj4x4[1] = projMat4x4[4]; proj4x4[2] = projMat4x4[8]; proj4x4[3] = projMat4x4[12];
            proj4x4[4] = projMat4x4[1]; proj4x4[5] = projMat4x4[5]; proj4x4[6] = projMat4x4[9]; proj4x4[7] = projMat4x4[13];
            proj4x4[8] = projMat4x4[2]; proj4x4[9] = projMat4x4[6]; proj4x4[10] = projMat4x4[10]; proj4x4[11] = projMat4x4[14];
            proj4x4[12] = projMat4x4[3]; proj4x4[13] = projMat4x4[7]; proj4x4[14] = projMat4x4[11]; proj4x4[15] = projMat4x4[15];

            for (int i = 0; i < 16; i++)
            {
                projMat4x4[i] = proj4x4[i];
            }
        }
    }
}
```


标定数据点采样和标定任务执行程序：
```java
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.arcare.Application;

import com.example.arcare.easyar.EasyAR;
import com.example.arcare.event.AddCorrespondingPoint;
import com.example.arcare.event.ClearCorrespondingPoints;
import com.example.arcare.event.DoSpaamCalibrate;
import com.example.arcare.glass.SPAAMConstant;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.opengles.GL10;

import cn.easyar.ImageTarget;

/**
 * SPAAM 单点主动对齐标定方法的实现。需要结合EasyAR使用
 */
public class SpaamCalibrator implements EasyAR.ImageTrackerResultListener{
    private final static String MESSAGE_LOST_TRACKING = "Target Lost";
    private final static String MESSAGE_TARGET_TRACKING = "Target Tracking";
    private final float[][] screenPoints;
    private final DisplayMetrics dm;
    private final Context mContext;
    private Display display;
    private TextView mTextView;
    private String previousMessage="";
    private SpaamUtil.SPAAM_SVD svd = new SpaamUtil.SPAAM_SVD();
    private ImageView mCrossImage;
    private int crossWidth;
    private int crossHeight;
    private boolean calibrated;
    private Handler mainHandler;
    private boolean addCorrespondingPoint;
    private int prePoint = -1;
    private int curPoint = 0;

    public SpaamCalibrator(Context context, Display display, ImageView crossImage, TextView messageView) {
        this.mContext = context;        
        this.display = display;         // AR眼镜屏幕Display
        this.mCrossImage = crossImage;  // 十字图案，在AR眼镜屏幕上显示的，用来标定用的
        this.mTextView = messageView;   // AR眼镜屏幕上的提示字体
        this.dm = new DisplayMetrics();
        display.getMetrics(this.dm);
        this.mainHandler = new Handler(context.getMainLooper());

        float unitX = dm.widthPixels/6f;
        float unitY = dm.heightPixels/6f;
        float halfX = dm.widthPixels/2f;
        float halfY = dm.heightPixels/2f;

        // 十字图案中心在屏幕上的25个位置。坐标原点为图片左上角。
        this.screenPoints = new float[][]{
                // 第一行
                {halfX - 2*unitX, halfY - 2*unitY}, {halfX - unitX, halfY - 2*unitY}, {halfX, halfY - 2*unitY}, {halfX + unitX, halfY - 2*unitY}, {halfX + 2*unitX, halfY - 2*unitY},
                // 第二行
                {halfX - 2*unitX, halfY - unitY}, {halfX - unitX, halfY - unitY}, {halfX, halfY - unitY}, {halfX + unitX, halfY - unitY}, {halfX + 2*unitX, halfY - unitY},
                // 第三行
                {halfX - 2*unitX, halfY}, {halfX - unitX, halfY}, {halfX, halfY}, {halfX + unitX, halfY}, {halfX + 2*unitX, halfY},
                // 第四行
                {halfX - 2*unitX, halfY + unitY}, {halfX - unitX, halfY + unitY}, {halfX, halfY + unitY}, {halfX + unitX, halfY + unitY}, {halfX + 2*unitX, halfY + unitY},
                // 第五行
                {halfX - 2*unitX, halfY + 2*unitY}, {halfX - unitX, halfY + 2*unitY}, {halfX, halfY + 2*unitY}, {halfX + unitX, halfY + 2*unitY}, {halfX + 2*unitX, halfY + 2*unitY},
        };

        // 这里给左眼进行标定，整体左移一下，否则带上眼镜后左眼看不清十字图案
        for (int i = 0; i < this.screenPoints.length; i++) {
            this.screenPoints[i][0] -= unitX/2.0;
        }
        //width = dm.widthPixels;         // 屏幕宽度（像素）
        //height = dm.heightPixels;       // 屏幕高度（像素）
        EventBus.getDefault().register(this);
    }

    @Subscribe
    public void doCalibrate(DoSpaamCalibrate doSpaamCalibrate) {
        this.doCalibrate(); // 这是采样完成后计算标定结果的方法
    }

    @Subscribe
    public void addPoint(AddCorrespondingPoint add) {
        addCorrespondingPoint = true; // 这是采样过程中添加采样点的标志信号
    }

    @Subscribe
    public synchronized void clearPoints(ClearCorrespondingPoints event) {
        // 清空采样点
        resetState();
    }

    private void setCrossLinePosition(float x, float y) {
        if (crossWidth == 0) {
            crossWidth = mCrossImage.getWidth();
        }
        if (crossHeight == 0) {
            crossHeight = mCrossImage.getHeight();
        }
        x -= crossWidth/2.0;
        y -= crossHeight/2.0;

        mCrossImage.setX(x);
        mCrossImage.setY(y);
    }

    private void resetState() {
        calibrated = false;
        // 清空采样点
        addCorrespondingPoint = false;
        svd.corr_points.clear();
        curPoint = 0;
        prePoint = -1;
    }

    /**
     * glPoseMatrix: 列优先存储的模型坐标系到相机坐标系的姿态转换矩阵
     */
    private void addCorrespondingPoints(float[] glPoseMatrix) {

        // x,y coordinate of the cross image's center point
        // origin is top left corner of image
        double x = mCrossImage.getX() + crossWidth/2.;
        double y = mCrossImage.getY() + crossHeight/2.;

        // change origin to left bottom of image
        y = dm.heightPixels - y;
        // 模型中原点（0,0,0）转换到相机坐标系后的点
        double[] point = new double[] {
                glPoseMatrix[12], glPoseMatrix[13], glPoseMatrix[14]
        };
        SpaamUtil.SPAAM_SVD.Correspondence_Pair pair = new SpaamUtil.SPAAM_SVD.Correspondence_Pair(point[0], point[1], point[2], x, y);
        svd.corr_points.add(pair);
    }

    public void doCalibrate() {
        if (!canCalibrate()) {
            runOnUIThread(()->{
                Toast.makeText(mContext, "要采集至少6个点才能进行标定", Toast.LENGTH_LONG).show();
            });
            return;
        }
        new SpaamCalibrationAsyncTask().execute();
    }

    private boolean canCalibrate() {
        return svd.corr_points.size() >= 6;
    }

    /**
     * 标定点数据采集完成后执行，计算转换矩阵.
     * 这个方法要放到异步线程去求解，因为有可能出现解不出来解，卡死的情况
     */
    private void calibrate() {
        //Call the SVD function, a minimum of 6 points is required//
        if ( canCalibrate() && svd.projectionDLTImpl() ) {
            //Build the OpenGL 4x4 projection matrix with a near clip plane of .1 and far clip plane of 100//
            svd.BuildGLMatrix3x4(0.01, 1000., dm.widthPixels, 0, dm.heightPixels, 0);
            //write the calibration results to the proper file//
            //Log.i(TAG, Arrays.toString(svd.projMat4x4));
            calibrated = true;
            float[] gMat3x4 = new float[12];
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 4; j++) {
                    gMat3x4[4*i+j] = (float) svd.Proj3x4.get(i, j);
                }
            }
            SPAAMConstant.G_Matrix_3x4 = gMat3x4;
        }
    }

    public boolean isCalibrated() {
        return calibrated;
    }

    @Override
    public synchronized void imageTrackerResult(GL10 gl, ImageTarget imagetarget, float[] glPoseMatrix, float[] glProjectionMatrix) {
        if (!Application.spaamCalibrating) {
            return;
        }
        String message = MESSAGE_TARGET_TRACKING + "\n已添加" + svd.corr_points.size() + "组样本点";
        if (!previousMessage.contentEquals(message)) {
            previousMessage = message;
            mainHandler.post(()->{
                mTextView.setText(message);
                mTextView.setTextColor(Color.parseColor("#00ff00"));
            });
        }

        if (curPoint != prePoint && curPoint<25) {
            prePoint = curPoint;

            float[] screenPoint = screenPoints[curPoint];
            runOnUIThread(()->{
                this.setCrossLinePosition(screenPoint[0], screenPoint[1]);
            });
        }

        if (addCorrespondingPoint && curPoint<25) {
            curPoint++;
            addCorrespondingPoints(glPoseMatrix);
        }
        addCorrespondingPoint = false;
    }

    @Override
    public void imageTrackingLost(GL10 gl) {
        if (previousMessage.contentEquals(MESSAGE_LOST_TRACKING)) {
            return;
        }
        previousMessage = MESSAGE_LOST_TRACKING;
        mainHandler.post(()->{
            mTextView.setText(MESSAGE_LOST_TRACKING);
            mTextView.setTextColor(Color.parseColor("#ff0000"));
        });
    }

    private void runOnUIThread(Runnable runnable) {
        mainHandler.post(runnable);
    }

    /**
     * 执行最终SPAAM标定，计算矩阵G的类
     */
    class SpaamCalibrationAsyncTask extends AsyncTask<Void, Void, Void> {

        private AsyncTask<Void, Void, Boolean> spaamCalibration = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                SpaamCalibrator.this.calibrate();
                return true;
            }
        };
        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(Void... arg0) {
            spaamCalibration.executeOnExecutor(SpaamCalibrationAsyncTask.THREAD_POOL_EXECUTOR);
            try {
                Boolean aBoolean = spaamCalibration.get(5, TimeUnit.SECONDS);
                if (Boolean.TRUE.equals(aBoolean)) {
                    // 保存到SharedPreferences中
                    CalibrationResult.save(mContext, Application.userName, svd.Proj3x4);
                }
            } catch (Exception e) {
                spaamCalibration.cancel(true);
                while (!spaamCalibration.isCancelled()) {
                    try {
                        spaamCalibration.wait(100);
                    } catch (InterruptedException ex) {
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            boolean calibrated = SpaamCalibrator.this.isCalibrated();
            String message = "标定失败";
            if (calibrated) {
                message = "标定成功";
            }

            (Toast.makeText(SpaamCalibrator.this.mContext, message, Toast.LENGTH_LONG)).show();

            SpaamCalibrator.this.resetState();
        }
    }
}

```


工具类CalibrationResult.java
```java
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.common.base.Strings;

import Jama.Matrix;

/**
 * 将SPAAM标定好的矩阵G保存到手机配置里，每次用户登录的时候自动获取
 * */
public abstract class CalibrationResult {
    private static final String SHARED_PREF_FILE = "SPAAM::CalibrationResult::Pref";
    private static final String TAG = "SPAAM::CalibrationResult";

    public static void save(Context context, String userName, Matrix G_3x4) {
        SharedPreferences sharedPref = context.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                builder.append(G_3x4.get(i, j))
                        .append(i==2&&j==3 ? "]" : ",");
            }
        }
        String matrixStr = builder.toString();
        editor.putString("CalibrationResult_G_3x4_Matrix:" + userName, matrixStr);
        editor.apply();
        Log.i(TAG, "Saved SPAAM calibration result - G_3x4 Matrix: " + matrixStr);
    }

    public static float[] tryLoad(Context context, String userName) {
        try {
            SharedPreferences sharedPref = context.getSharedPreferences(SHARED_PREF_FILE, Context.MODE_PRIVATE);
            String matrix = sharedPref.getString("CalibrationResult_G_3x4_Matrix:" + userName, null);
            if (Strings.isNullOrEmpty(matrix)) {
                return null;
            }
            float[] G_3x4 = new float[16];
            String[] split = matrix.substring(1, matrix.length() - 1).split(",");
            for (int i = 0; i < split.length; i++) {
                G_3x4[i] = Float.parseFloat(split[i].trim());
            }
            return G_3x4;
        } catch (Exception e) {
            Log.i(TAG, "Failed to retrieve Calibration Result", e);
        }
        return null;
    }
}

```


工具类SPAAMConstant，用来和OpenGL集成时计算投影矩阵
```java
public class SPAAMConstant {

    /**
     * 3行4列，行优先存储矩阵。该矩阵为SPAAM论文中标定后得到的矩阵G。矩阵G要和OpenGL集成还需要使用
     * 方法{@link #constructProjectionMatrix4x4_(float, float, int, int, int, int)}来计算投影矩阵进行集成。
     * 下面这个矩阵是某个人已经标定好的左眼的数据。
     */
    public static volatile float[] G_Matrix_3x4 = new float[] {
            3262.960907012f, 20.932897929f, -1364.343369176f, 934.207927547f,
            -3.005901481f, 3326.802757571f, -174.203868122f, 3293.483800532f,
            -0.022753432f, -0.026573106f, -0.999387888f, 1.354207745f
    };

    private static float[] createOrthographicProjection(float ne, float fr, int right, int left, int top, int bottom) {
        float[] ortho = new float[16];
        ortho[0] = 2.0f / (right - left);
        ortho[1] = 0.0f;
        ortho[2] = 0.0f;
        ortho[3] = (right + left)*1f / (left - right);
        ortho[4] = 0.0f;
        ortho[5] = 2.0f / (top - bottom);
        ortho[6] = 0.0f;
        ortho[7] = (top + bottom)*1f / (bottom - top);
        ortho[8] = 0.0f;
        ortho[9] = 0.0f;
        ortho[10] = 2.0f / (ne - fr);
        ortho[11] = (fr + ne) / (ne - fr);
        ortho[12] = 0.0f;
        ortho[13] = 0.0f;
        ortho[14] = 0.0f;
        ortho[15] = 1.0f;
        return ortho;
    }
    //This function creates an orthogonal matrix that is then multiplied by the 3x4 SPAAM result//
    //creating a 4x4 matrix (column major order) usable by OpenGL//

    /*
     * |                                                |
     * |  | 1  0     0      |        | 0 0 0 0        | |    | 2/(right-left)      0          0    -(right+left)/(right-left)  |
     * |  | 0  1     0      | * G +  | 0 0 0 0        | |  * |        0       2/(top-bottom)  0    -(top+bottom)/(top-bottom)  |
     * |  | 0  0  -far-near |        | 0 0 0 far*near | |    |        0            0   -2/(far-near)    -(far+near)/(far-near) |
     * |  | 0  0     1      |        | 0 0 0 0        | |    |        0            0          0                 1              |
     * |                                                |
     *
     * @return 4x4 opengl_projectionMatrix 列优先投影矩阵
     */
    public static float[] constructProjectionMatrix4x4_( float ne, float fr, int right, int left, int top, int bottom)
    {
        float[] projMat4x4 = new float[16];
        projMat4x4[0] = G_Matrix_3x4[0]; projMat4x4[1] = G_Matrix_3x4[1]; projMat4x4[2] = G_Matrix_3x4[2]; projMat4x4[3] = G_Matrix_3x4[3];
        projMat4x4[4] = G_Matrix_3x4[4]; projMat4x4[5] = G_Matrix_3x4[5]; projMat4x4[6] = G_Matrix_3x4[6]; projMat4x4[7] = G_Matrix_3x4[7];
        projMat4x4[8] = G_Matrix_3x4[8]; projMat4x4[9] = G_Matrix_3x4[9]; projMat4x4[10] = G_Matrix_3x4[10]; projMat4x4[11] = G_Matrix_3x4[11];

        float[] proj4x4 = new float[16];
        //Copy base 3x4 values//
        System.arraycopy(projMat4x4, 0, proj4x4, 0, 12);
        //Duplicate third row into the fourth//
        System.arraycopy(projMat4x4, 8, proj4x4, 12, 4);

        //calculate extra parameters//
        double norm = Math.sqrt(proj4x4[8] * proj4x4[8] + proj4x4[9] * proj4x4[9] + proj4x4[10] * proj4x4[10]);
        double add = fr*ne*norm;

        //Begin adjusting the 3x4 values for 4x4 use//
        proj4x4[8] *= (-fr - ne);
        proj4x4[9] *= (-fr - ne);
        proj4x4[10] *= (-fr - ne);
        proj4x4[11] *= (-fr - ne);
        proj4x4[11] += add;

        //Create Orthographic projection matrix//
        float[] ortho = createOrthographicProjection(ne, fr, right, left, top, bottom);

        //Multiply the 4x4 projection by the orthographic projection//
        projMat4x4[0] = ortho[0]*proj4x4[0] + ortho[1]*proj4x4[4] + ortho[2]*proj4x4[8] + ortho[3]*proj4x4[12];
        projMat4x4[1] = ortho[0]*proj4x4[1] + ortho[1]*proj4x4[5] + ortho[2]*proj4x4[9] + ortho[3]*proj4x4[13];
        projMat4x4[2] = ortho[0]*proj4x4[2] + ortho[1]*proj4x4[6] + ortho[2]*proj4x4[10] + ortho[3]*proj4x4[14];
        projMat4x4[3] = ortho[0]*proj4x4[3] + ortho[1]*proj4x4[7] + ortho[2]*proj4x4[11] + ortho[3]*proj4x4[15];

        projMat4x4[4] = ortho[4]*proj4x4[0] + ortho[5]*proj4x4[4] + ortho[6]*proj4x4[8] + ortho[7]*proj4x4[12];
        projMat4x4[5] = ortho[4]*proj4x4[1] + ortho[5]*proj4x4[5] + ortho[6]*proj4x4[9] + ortho[7]*proj4x4[13];
        projMat4x4[6] = ortho[4]*proj4x4[2] + ortho[5]*proj4x4[6] + ortho[6]*proj4x4[10] + ortho[7]*proj4x4[14];
        projMat4x4[7] = ortho[4]*proj4x4[3] + ortho[5]*proj4x4[7] + ortho[6]*proj4x4[11] + ortho[7]*proj4x4[15];

        projMat4x4[8] = ortho[8]*proj4x4[0] + ortho[9]*proj4x4[4] + ortho[10]*proj4x4[8] + ortho[11]*proj4x4[12];
        projMat4x4[9] = ortho[8]*proj4x4[1] + ortho[9]*proj4x4[5] + ortho[10]*proj4x4[9] + ortho[11]*proj4x4[13];
        projMat4x4[10] = ortho[8]*proj4x4[2] + ortho[9]*proj4x4[6] + ortho[10]*proj4x4[10] + ortho[11]*proj4x4[14];
        projMat4x4[11] = ortho[8]*proj4x4[3] + ortho[9]*proj4x4[7] + ortho[10]*proj4x4[11] + ortho[11]*proj4x4[15];

        projMat4x4[12] = ortho[12]*proj4x4[0] + ortho[13]*proj4x4[4] + ortho[14]*proj4x4[8] + ortho[15]*proj4x4[12];
        projMat4x4[13] = ortho[12]*proj4x4[1] + ortho[13]*proj4x4[5] + ortho[14]*proj4x4[9] + ortho[15]*proj4x4[13];
        projMat4x4[14] = ortho[12]*proj4x4[2] + ortho[13]*proj4x4[6] + ortho[14]*proj4x4[10] + ortho[15]*proj4x4[14];
        projMat4x4[15] = ortho[12]*proj4x4[3] + ortho[13]*proj4x4[7] + ortho[14]*proj4x4[11] + ortho[15]*proj4x4[15];

        proj4x4[0] = projMat4x4[0]; proj4x4[1] = projMat4x4[4]; proj4x4[2] = projMat4x4[8]; proj4x4[3] = projMat4x4[12];
        proj4x4[4] = projMat4x4[1]; proj4x4[5] = projMat4x4[5]; proj4x4[6] = projMat4x4[9]; proj4x4[7] = projMat4x4[13];
        proj4x4[8] = projMat4x4[2]; proj4x4[9] = projMat4x4[6]; proj4x4[10] = projMat4x4[10]; proj4x4[11] = projMat4x4[14];
        proj4x4[12] = projMat4x4[3]; proj4x4[13] = projMat4x4[7]; proj4x4[14] = projMat4x4[11]; proj4x4[15] = projMat4x4[15];

        return proj4x4;
    }
}

```


最后是和EasyAR SDK集成。下面的代码不全，但是能看懂大概意思就行
```java
import android.content.Context;
import android.opengl.GLES20;
import android.os.SystemClock;
import android.util.Log;

import com.example.arcare.Application;
import com.example.arcare.event.ArGlassCameraEvent;
import com.example.arcare.glass.SPAAMConstant;
import com.lgh.uvccamera.callback.PreviewCallback;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import cn.easyar.Buffer;
import cn.easyar.CameraParameters;
import cn.easyar.DelayedCallbackScheduler;
import cn.easyar.FrameFilterResult;
import cn.easyar.FunctorOfVoidFromTargetAndBool;
import cn.easyar.Image;
import cn.easyar.ImageTarget;
import cn.easyar.ImageTracker;
import cn.easyar.ImageTrackerMode;
import cn.easyar.ImageTrackerResult;
import cn.easyar.InputFrame;
import cn.easyar.InputFrameSink;
import cn.easyar.InputFrameThrottler;
import cn.easyar.InputFrameToFeedbackFrameAdapter;
import cn.easyar.Matrix44F;
import cn.easyar.OutputFrame;
import cn.easyar.OutputFrameBuffer;
import cn.easyar.OutputFrameFork;
import cn.easyar.StorageType;
import cn.easyar.Target;
import cn.easyar.TargetInstance;
import cn.easyar.TargetStatus;
import cn.easyar.Vec2F;

public class EasyAR {

    private final Context mContext;
    private DelayedCallbackScheduler scheduler;
    private ISampleCamera cameraDevice;
    private ArrayList<ImageTracker> trackers;
    private BGRenderer bgRenderer;
    private BoxRenderer boxRenderer;
    private InputFrameThrottler throttler;
    private InputFrameToFeedbackFrameAdapter i2FAdapter;
    private OutputFrameBuffer outputFrameBuffer;
    private InputFrameSink sink;
    private OutputFrameFork outputFrameFork;
    private int previousInputFrameIndex = -1;
    private byte[] imageBytes = null;
    private Boolean imageTrackingLost = null;
    private float[] projectionMatrix;
    private List<ImageTrackerResultListener> imageTrackerResultListeners;
    private volatile boolean showBackground = false;
    private boolean projectionMatrixSaved = false;
    public static float[] getGLMatrix(Matrix44F m) {
        float[] d = m.data;
        return new float[]{d[0], d[4], d[8], d[12], d[1], d[5], d[9], d[13], d[2], d[6], d[10], d[14], d[3], d[7], d[11], d[15]};
    }

    public EasyAR(Context context, ISampleCamera cameraDevice) {
        this.mContext = context;
        scheduler = new DelayedCallbackScheduler();
        trackers = new ArrayList<ImageTracker>();
        this.cameraDevice = cameraDevice;
    }

    private void loadFromImage(ImageTracker tracker, String path, String name) {
        ImageTarget target = ImageTarget.createFromImageFile(path, StorageType.Assets, name, "", "", 1.0f);
        if (target == null) {
            Log.e("HelloAR", "target create failed or key is not correct");
            return;
        }
        tracker.loadTarget(target, scheduler, new FunctorOfVoidFromTargetAndBool() {
            @Override
            public void invoke(Target target, boolean status) {
                Log.i("HelloAR", String.format("load target (%b): %s (%d)", status, target.name(), target.runtimeID()));
            }
        });
    }

    public void recreate_context() {
        if (bgRenderer != null) {
            bgRenderer.dispose();
            bgRenderer = null;
        }
        if (boxRenderer != null) {
            boxRenderer.dispose();
            boxRenderer = null;
        }
        previousInputFrameIndex = -1;
        bgRenderer = new BGRenderer();
        boxRenderer = new BoxRenderer();
    }

    public void initialize() {
        recreate_context();

        cameraDevice.open();
        throttler = InputFrameThrottler.create();
        i2FAdapter = InputFrameToFeedbackFrameAdapter.create();
        outputFrameBuffer = OutputFrameBuffer.create();
        outputFrameFork = OutputFrameFork.create(2);
        ImageTracker tracker = ImageTracker.createWithMode(ImageTrackerMode.PreferQuality);
        tracker.setSimultaneousNum(2);
        loadFromImage(tracker, "idback.jpg", "idback");
        loadFromImage(tracker, "namecard.jpg", "namecard");
        loadFromImage(tracker, "marker2.jpg", "marker2");
        trackers.add(tracker);
        sink = throttler.input();
        throttler.output().connect(i2FAdapter.input());
        i2FAdapter.output().connect(tracker.feedbackFrameSink());
        tracker.outputFrameSource().connect(outputFrameFork.input());
        outputFrameFork.output(0).connect(outputFrameBuffer.input());
        outputFrameFork.output(1).connect(i2FAdapter.sideInput());
        outputFrameBuffer.signalOutput().connect(throttler.signalInput());
    }

    public void dispose() {
        for (ImageTracker tracker : trackers) {
            tracker.dispose();
        }
        trackers.clear();
        if (bgRenderer != null) {
            bgRenderer.dispose();
            bgRenderer = null;
        }
        if (boxRenderer != null) {
            boxRenderer.dispose();
            boxRenderer = null;
        }
        cameraDevice = null;
        if (scheduler != null) {
            scheduler.dispose();
            scheduler = null;
        }
    }

    public boolean start() {
        EventBus.getDefault().register(this);
        boolean status = true;
        if (cameraDevice != null) {
            //status &= cameraDevice.start(new CameraCallback());
            status &= cameraDevice.start(new CameraByteBufferCallback());
        } else {
            status = false;
        }
        for (ImageTracker tracker : trackers) {
            status &= tracker.start();
        }
        return status;
    }

    public void stop() {
        EventBus.getDefault().unregister(this);
        if (cameraDevice != null) {
            cameraDevice.stop();
        }
        dispose();
    }

    @Subscribe
    public void onArGlassCameraEvent(ArGlassCameraEvent event) {
        if (ArGlassCameraEvent.EnablePreviewOnGlass.equals(event)) {
            showBackground = true;
        } else {
            showBackground = false;
        }
    }
    public void render(GL10 gl, int width, int height, int screenRotation) {
        while (scheduler.runOne()) {
        }

        GLES20.glViewport(0, 0, width, height);
        GLES20.glClearColor(0.f, 0.f, 0.f, 1.f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        OutputFrame oframe = outputFrameBuffer.peek();
        if (oframe == null) {
            return;
        }
        InputFrame iframe = oframe.inputFrame();
        if (iframe == null) {
            oframe.dispose();
            return;
        }
        CameraParameters cameraParameters = iframe.cameraParameters();
        if (cameraParameters == null) {
            oframe.dispose();
            iframe.dispose();
            return;
        }

        float viewport_aspect_ratio = (float) width / (float) height;
        Matrix44F imageProjection = cameraParameters.imageProjection(viewport_aspect_ratio, screenRotation, true, false);
        Image image = iframe.image();

        try {
            if (iframe.index() != previousInputFrameIndex) {
                Buffer buffer = image.buffer();
                try {
                    if ((imageBytes == null) || (imageBytes.length != buffer.size())) {
                        imageBytes = new byte[buffer.size()];
                    }
                    buffer.copyToByteArray(imageBytes);
                    bgRenderer.upload(image.format(), image.width(), image.height(), ByteBuffer.wrap(imageBytes));
                } finally {
                    buffer.dispose();
                }
                previousInputFrameIndex = iframe.index();
            }

            if (showBackground) {
                bgRenderer.render(imageProjection);
            }

            // 在这里集成SPAAM标定的结果
            if (!Application.useSpaamResult) {
                // 方案A. 眼镜未经过标定时使用的投影矩阵
                Matrix44F projectionMatrix = cameraParameters.projection(0.01f, 1000.f, viewport_aspect_ratio, screenRotation, true, false);
                this.projectionMatrix = getGLMatrix(projectionMatrix);
            } else {
                // 方案B. 使用SPAAM标定好眼镜后的数据计算投影矩阵, 并替换方案A。
                // AR眼镜 width=1920  height=1080
                float[] floats = SPAAMConstant.constructProjectionMatrix4x4_(0.01f, 1000f, width, 0, height, 0);
                this.projectionMatrix = floats;
            }

            // oframe.results()一定不为null
            if (imageTrackingLost != null) {
                imageTrackingLost = true;
            }
            for (FrameFilterResult oResult : oframe.results()) {
                ImageTrackerResult result = (ImageTrackerResult) oResult;
                if (result != null) {
                    ArrayList<TargetInstance> targetInstances = result.targetInstances();
                    for (TargetInstance targetInstance : targetInstances) {
                        int status = targetInstance.status();
                        if (status == TargetStatus.Tracked) {
                            imageTrackingLost = false;
                            Target target = targetInstance.target();
                            ImageTarget imagetarget = target instanceof ImageTarget ? (ImageTarget) (target) : null;
                            if (imagetarget == null) {
                                continue;
                            }
                            ArrayList<Image> images = ((ImageTarget) target).images();
                            Image targetImg = images.get(0);
                            float targetScale = imagetarget.scale();
                            Vec2F scale = new Vec2F(targetScale, targetScale * targetImg.height() / targetImg.width());
                            boxRenderer.render(projectionMatrix, targetInstance.pose(), scale);

                            if (this.imageTrackerResultListeners != null) {
                                float[] glPoseMatrix = getGLMatrix(targetInstance.pose());
                                try {
                                    this.imageTrackerResultListeners.forEach(action->action.imageTrackerResult(gl, imagetarget, glPoseMatrix, this.projectionMatrix));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            for (Image img : images) {
                                img.dispose();
                            }
                        }
                    }
                    result.dispose();
                }
            }

            // 1. 刚开始加载，没有识别出图片的时候，imageTrackingLost = null，因此下面的方法不调用。只有
            //    当第一次识别出图片的时候imageTrackingLost = false初始化。
            // 2. 当图片识别丢失（即上一帧识别出来了，但是这一帧又没识别出来）后，imageTrackingLost = true
            //    之后的每一帧都会掉用下面的方法。
            if (imageTrackingLost != null && imageTrackingLost && this.imageTrackerResultListeners != null) {
                try {
                    this.imageTrackerResultListeners.forEach((e)-> e.imageTrackingLost(gl));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            iframe.dispose();
            oframe.dispose();
            if (cameraParameters != null) {
                cameraParameters.dispose();
            }
            image.dispose();
        }
    }

    public void addImageTrackerResultListener(ImageTrackerResultListener listener) {
        if (this.imageTrackerResultListeners == null) {
            this.imageTrackerResultListeners = new ArrayList<>();
        }
        this.imageTrackerResultListeners.add(listener);
    }

    public interface ImageTrackerResultListener {
        void imageTrackerResult(GL10 gl, ImageTarget imagetarget, float[] glPoseMatrix, float[] glProjectionMatrix);
        void imageTrackingLost(GL10 gl);
    }

    class CameraByteBufferCallback implements IPreviewFrameCallback {

        @Override
        public void onPreviewFrame(ByteBuffer yuv) {
            Buffer buf = null;
            Image img = null;
            InputFrame frame = null;
            try {
                CameraParameters cameraParameters = cameraDevice.getmCameraParameters();
                buf = Buffer.wrapBuffer(yuv);

                CameraFrame cameraFrame = new CameraFrame(yuv, cameraDevice.getPixelFormat(),
                        cameraParameters.size().data[0], cameraParameters.size().data[1], cameraParameters.cameraOrientation());
                EventBus.getDefault().post(cameraFrame);

                img = new Image(buf,
                        cameraDevice.getPixelFormat(),
                        cameraParameters.size().data[0],
                        cameraParameters.size().data[1]);

                frame = InputFrame.createWithImageAndCameraParametersAndTemporal(img, cameraParameters, SystemClock.elapsedRealtimeNanos() * 1e-9);
                sink.handle(frame);
            } finally {
                if (buf != null) buf.dispose();
                if (img != null) img.dispose();
                if (frame != null) frame.dispose();
            }
        }
    }
}

```

Camera接口
```java

import android.graphics.SurfaceTexture;
import com.lgh.uvccamera.callback.PreviewCallback;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import cn.easyar.CameraParameters;

public interface ISampleCamera {
    boolean open();
    @Deprecated
    boolean start(PreviewCallback callback);
    boolean start(IPreviewFrameCallback callback);
    void stop();
    CameraParameters getmCameraParameters();
    int getPixelFormat();
    void setOnCameraStartedListener(OnCameraStartedListener listener);
    // 设置屏幕的旋转
    void setDisplayOrientation(int displayOrientation);

    void setZoom(int zoom);
    interface OnCameraStartedListener {
        void onCameraStarted(SurfaceTexture surfaceTexture, CameraParameters cameraParameters, int pixelFormat);
    }
}

// PreviewCallback.java
public interface PreviewCallback {
    /**
     * 预览流回调
     *
     * @param yuv yuv格式的数据流
     */
    void onPreviewFrame(byte[] yuv);
}


// IPreviewFrameCallback.java
import java.nio.ByteBuffer;

public interface IPreviewFrameCallback {
    /**
     * 预览流回调
     *
     * @param yuv yuv格式的数据流
     */
    void onPreviewFrame(ByteBuffer yuv);
}
```