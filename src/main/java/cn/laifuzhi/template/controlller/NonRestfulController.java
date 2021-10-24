package cn.laifuzhi.template.controlller;

import cn.laifuzhi.template.Application;
import cn.laifuzhi.template.model.http.req.BaseReq;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

@Controller
@RequestMapping("template")
public class NonRestfulController {
    private File healthCheckFile;
    @PostConstruct
    private void init() throws IOException {
        healthCheckFile = Paths.get(new ApplicationHome(getClass()).getDir().getCanonicalPath(), "healthCheck").toFile();
    }

    @GetMapping("healthCheck")
    public ResponseEntity<String> healthCheck() {
        if (!healthCheckFile.exists() || !Application.isStarted()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body("ok");
    }

    @PostMapping("ico")
    public ResponseEntity<byte[]> ico() throws IOException {
        byte[] bytes = Files.asByteSource(new File("/Users/lfz/favicon.ico")).read();
        HttpHeaders headers = new HttpHeaders();
        //设置MIME类型
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.builder(FileUpload.ATTACHMENT).filename("汉字.ico", Charsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    // 原封不动返回上传的文件，因为接收两个参数，都要加RequestPart，RequestPart是专门给multipart/form-data请求用的，参数名就是前端传来的参数名
    // 如果file参数传了多个文件，只会受到第一个文件。可以用数组或者list接收
    @PostMapping("upload")
    public ResponseEntity<byte[]> upload(@Valid @RequestPart BaseReq baseReq, @RequestPart MultipartFile file) throws IOException {
         HttpHeaders headers = new HttpHeaders();
        //设置MIME类型
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.builder(FileUpload.ATTACHMENT).filename(StringUtils.isEmpty(file.getOriginalFilename()) ? "下载文件" : file.getOriginalFilename(), Charsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(file.getBytes());
    }
}
