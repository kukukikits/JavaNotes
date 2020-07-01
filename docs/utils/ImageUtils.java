import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;

/**
 * 图像处理工具类
 * Author: geshengbin
 * Email: kukukiki19940703@163.com
 */
public class ImageUtils {

    /**
     * 创建图片缩略图，缩略图的长宽比和原图相同，并不会完全按照w和h参数指定的大小去压缩
     * @param imgStream 图片流
     * @param w 缩略图的宽
     * @param h 缩略图的高
     * @param out 缩略图的输出流, 方法执行后自动关闭
     * @throws IOException 缩略图创建失败抛出异常
     */
    public static void thumbnailImage(InputStream imgStream, int w, int h, OutputStream out) throws IOException {
        // ImageIO 支持的图片类型 : [BMP, bmp, jpg, JPG, wbmp, jpeg, png, PNG, JPEG, WBMP, GIF, gif]
        try (InputStream in = imgStream;
             ImageInputStream imageInputStream = ImageIO.createImageInputStream(in);
             OutputStream closableOut = out){
            // Find all image readers that recognize the image format
            Iterator iter = ImageIO.getImageReaders(imageInputStream);
            while (iter.hasNext()) {
                try {
                    ImageReader reader = (ImageReader) iter.next();
                    reader.setInput(imageInputStream, false);

                    int minIndex = reader.getMinIndex();
                    BufferedImage read;

                    float aspectRatio = reader.getAspectRatio(minIndex);
                    int height = reader.getHeight(minIndex);
                    int width = reader.getWidth(minIndex);
                    float requiredAspectRatio = w * 1f / h;

                    if (requiredAspectRatio > aspectRatio) {
                        w = (int)Math.ceil(aspectRatio * h);
                    } else {
                        h = (int)Math.ceil(w / aspectRatio);
                    }

                    ImageReadParam params = reader.getDefaultReadParam();
                    //re-sample
                    params.setSourceSubsampling(width / w, height / h, 0, 0);
                    read = reader.read(minIndex, params);
                    ImageIO.write(read, reader.getFormatName(), closableOut);
                    return;
                } catch (Exception e) {
                    //retry
                }
            }
            // No reader found
            throw new IOException("Create thumbnail failed!");
        }
    }

    public static void thumbnailImage(File imgFile, int w, int h, OutputStream out) throws IOException {
        thumbnailImage(new FileInputStream(imgFile), w, h, out);
    }
}