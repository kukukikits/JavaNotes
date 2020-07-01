# 使用Webuploader上传文件

## 前端

看官方文档：

## 后端

```java

public class UploadDTO {
    private String type;

    private Long size;

    private Integer chunk;

    private String name;

    private String md5;

    private Integer chunks;

    // Getter、Setter方法略
}

import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
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

        long seek = chunkSize * chunk;
        String destFilePath = UPLOAD_PATH + File.separator + fileName;
        File destFile = new File(destFilePath);
        if (!file.isEmpty()) {
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(destFile, "rw")) {
                randomAccessFile.getChannel()
                        .write(ByteBuffer.wrap(file.getBytes()), seek);
            }
        }
    }

}
```