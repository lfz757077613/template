package cn.laifuzhi.template.controlller;

import cn.laifuzhi.template.Application;
import cn.laifuzhi.template.model.MyException;
import cn.laifuzhi.template.model.http.req.BaseReq;
import com.google.common.base.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.coyote.Constants;
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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping
public class NonRestfulController {
    private File healthCheckFile;

    @PostConstruct
    private void init() throws IOException {
        healthCheckFile = Paths.get(new ApplicationHome(getClass()).getDir().getCanonicalPath(), "healthCheck.tmp").toFile();
    }

    @GetMapping("healthCheck")
    public ResponseEntity<String> healthCheck() {
        if (!healthCheckFile.exists() || !Application.isStarted()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body("ok");
    }

    @PostMapping("download")
    public ResponseEntity<byte[]> download() throws IOException {
        String filePath = "xxx";
        String fileName = "xxx";
        Path file = Paths.get(filePath);
        byte[] bytes = FileUtils.readFileToByteArray(file.toFile());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(StringUtils.defaultString(Files.probeContentType(file), MediaType.APPLICATION_OCTET_STREAM_VALUE)));
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName, Charsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @GetMapping("streamingDownload")
    public ResponseEntity<StreamingResponseBody> streamingDownload() throws IOException {
        String filePath = "xxx";
        String fileName = "xxx";
        Path file = Paths.get(filePath);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(StringUtils.defaultString(Files.probeContentType(file), MediaType.APPLICATION_OCTET_STREAM_VALUE)));
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName, Charsets.UTF_8).build());
        StreamingResponseBody body = new StreamingResponseBody() {
            @Override
            public void writeTo(OutputStream outputStream) throws IOException {
                // StreamingResponseBody不阻塞servlet线程，这里的逻辑执行在默认的RequestMappingHandlerAdapter.taskExecutor线程池
                // 或者自己设置的线程池中，默认的线程池每有一个请求就新建一个线程
                // 同样受spring.mvc.async.request-timeout和servlet容器的默认超时控制
                // StreamingResponseBody该类比较鸡肋，在超时的场景下需要客户端配合断连，实际应用中不如使用servlet线程直接写入呢
                // 因为超时会抛出异常给客户端写回异常响应数据，此时客户端如果继续用这个连接发送请求tomcat会中断该链接，客户端会请求失败
                outputStream.write(FileUtils.readFileToByteArray(file.toFile()));
            }
        };
        return ResponseEntity.ok().headers(headers).body(body);
    }

    // https://tomcat.apache.org/tomcat-10.1-doc/aio.html
    @PostMapping("zeroCopyDownload")
    public void zeroCopyDownload(@Valid BaseReq baseReq, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String filePath = "xxx";
        String fileName = "xxx";
        if (!Boolean.parseBoolean(request.getAttribute(Constants.SENDFILE_SUPPORTED_ATTR).toString())) {
            throw new MyException("unsupported");
        }
        Path file = Paths.get(filePath);
        response.setContentType(StringUtils.defaultString(Files.probeContentType(file), MediaType.APPLICATION_OCTET_STREAM_VALUE));
        response.setContentLengthLong(file.toFile().length());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName, Charsets.UTF_8).build().toString());
        // 通过start/end可以实现零拷贝分片下载
        request.setAttribute(Constants.SENDFILE_FILENAME_ATTR, file.toFile().getCanonicalPath());
        request.setAttribute(Constants.SENDFILE_FILE_START_ATTR, 0L);
        request.setAttribute(Constants.SENDFILE_FILE_END_ATTR, file.toFile().length());
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

    @GetMapping("index")
    public ModelAndView index() {
        ModelAndView modelAndView = new ModelAndView("index");
        modelAndView.addObject("test", "hello");
        return modelAndView;
    }
}
