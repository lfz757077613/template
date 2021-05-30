package cn.laifuzhi.template.controlller;

import cn.laifuzhi.template.Application;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.FileUpload;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;


@Controller("template")
public class NonRestfulController {
    private final File healthCheckFile = Paths.get(new ApplicationHome(getClass()).getDir().getPath(), "healthCheck").toFile();

    @GetMapping("healthCheck")
    private ResponseEntity<String> healthCheck() {
        if (!healthCheckFile.exists() || !Application.isStarted()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body("ok");
    }

    @PostMapping("ico")
    private ResponseEntity<byte[]> ico() throws IOException {
        byte[] bytes = Files.asByteSource(new File("/Users/lfz/favicon.ico")).read();
        HttpHeaders headers=new HttpHeaders();
        //设置MIME类型
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.builder(FileUpload.ATTACHMENT).filename("汉字.ico", Charsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    // 原封不动返回上传的文件
    @PostMapping("upload")
    private ResponseEntity<byte[]> upload(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.ok().build();
        }
        HttpHeaders headers=new HttpHeaders();
        //设置MIME类型
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.builder(FileUpload.ATTACHMENT).filename(StringUtils.isEmpty(file.getOriginalFilename()) ? "下载文件" : file.getOriginalFilename(), Charsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(file.getBytes());
    }
}
