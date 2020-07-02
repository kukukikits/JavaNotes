# 使用Webuploader上传文件

## 前端

看官方文档：http://fex.baidu.com/webuploader/demo.html

## 后端

```java

public class UploadDTO {
    private String type;
    /**
     * 整个文件的大小
     */
    private Long size;
    /**
     * 分片的编号，从0开始，到chunks-1
     */
    private Integer chunk;

    private String name;

    private String md5;
    /**
     * 分片的总数
     */
    private Integer chunks;

    // Getter、Setter方法略
}

import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;
import java.util.UUID;
public class UploadService {

    private String UPLOAD_PATH = "F:/files";

    public void upload(UploadDTO uploadDTO, MultipartFile multipartFile) throws IOException {
        try {

            if (Objects.nonNull(uploadDTO.getChunk())) {
                chunkUpload(uploadDTO.getName(), uploadDTO.getMd5(), uploadDTO.getChunk(),
                        uploadDTO.getChunks(), uploadDTO.getSize(), multipartFile);
            } else {
                uploadMultiPart(multipartFile, multipartFile.getOriginalFilename());
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public void upload(InputStream inputStream, String fileName) throws IOException {
        File baseFile = new File(UPLOAD_PATH);
        File targetFile = new File(baseFile, Objects.requireNonNull(fileName));

        if (!baseFile.exists()) {
            boolean mkdirs = baseFile.mkdirs();
            logger.info("Create file storage path: [{}], result [{}]", baseFile.getPath(), mkdirs);
        }

        try (InputStream in = inputStream) {
            Files.copy(in, targetFile.toPath());
        } catch (IOException e) {
            logger.error("Upload failed", e);
            throw e;
        }
    }

    /**
     * 处理MultipartFile
     */
    private void uploadMultiPart(@NotNull MultipartFile file, @NotBlank String fileName) throws IOException {
        File baseFile = new File(UPLOAD_PATH);

        String prefix = UUID.randomUUID().toString();
        fileName = prefix + "-" + fileName;
        File targetFile = new File(baseFile, fileName);

        if (!baseFile.exists()) {
            boolean mkdirs = baseFile.mkdirs();
            logger.info("Create file storage path: [{}], result [{}]", baseFile.getPath(), mkdirs);
        }

        //保存
        file.transferTo(targetFile);
    }

    /**
     * 分片上传
     */
    private void chunkUpload(@NotBlank String fileName, @NotBlank String fileMd5,
                               @NotNull Integer chunk, @NotNull Integer chunks, @NotNull Long chunkSize,
                               @NotNull MultipartFile file) throws IOException {
       // 分片文件上传
        boolean lastChunk = chunk == chunks - 1;
        String destFilePath = UPLOAD_PATH + File.separator + fileName;
        File destFile = new File(destFilePath);
        if (!file.isEmpty()) {
            long seek = lastChunk ? fileSize - file.getSize(): file.getSize() * chunk;
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(destFile, "rw");
                 FileChannel outChannel = randomAccessFile.getChannel();
                 ReadableByteChannel inputChannel = file.getResource().readableChannel()){

                outChannel.position(seek);
                ByteBuffer allocate = ByteBuffer.allocate(1024 * 1024);
                while (inputChannel.read(allocate) != -1) {
                    allocate.flip();
                    outChannel.write(allocate);
                    allocate.clear();
                }
            }
        }
    }

}
```