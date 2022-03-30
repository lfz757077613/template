package cn.laifuzhi.template.client;

import com.google.common.collect.Maps;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import javax.annotation.Resource;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@Slf4j
@Component
public final class MailClient {
    @Resource
    private JavaMailSender sender;
    @Resource
    private MailProperties mailProperties;
    @Resource
    private FreeMarkerConfigurer freeMarkerConfigurer;

    public void sendText(String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.getUsername());
            message.setTo("757077613@qq.com");
//            主题
            message.setSubject("新用户注册：" + content);
            message.setText("欢迎：" + content);
            // 发送邮件，JavaMailSender类似http1.0，都是短连接
            sender.send(message);
        } catch (Exception e) {
            log.error("send email error, content:{}", content, e);
        }
    }

    public void sendHtml(File file, File picture) {
        try {
            HashMap<String, String> paramMap = Maps.newHashMap();
            paramMap.put("test", "hello");
            Template indexTemplate = freeMarkerConfigurer.getConfiguration().getTemplate("index.ftlh", StandardCharsets.UTF_8.name());
            MimeMessageHelper helper = new MimeMessageHelper(sender.createMimeMessage(), true);
            helper.setFrom(mailProperties.getUsername());
            helper.setTo("757077613@qq.com");
            helper.setSubject("新用户注册");
            helper.setText(FreeMarkerTemplateUtils.processTemplateIntoString(indexTemplate, paramMap), true);
            helper.addAttachment(file.getName(), file);
//            html中通过contentId使用<img src='cid:contentId'/>
            helper.addInline(picture.getName(), picture);
            sender.send(helper.getMimeMessage());
        } catch (Exception e) {
            log.error("sendHtml email error", e);
        }
    }
// 自定义javaMailSender
//    public void sendDiv(String content) {
//        try {
//            JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
//            javaMailSender.setHost();
////            默认-1，使用协议的默认port
//            javaMailSender.setPort();
//            javaMailSender.setUsername();
//            javaMailSender.setPassword();
//            javaMailSender.setProtocol("smtp");
//            javaMailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());
//            javaMailSender.setJavaMailProperties();
//
//            SimpleMailMessage message = new SimpleMailMessage();
//            message.setFrom("757077613@qq.com");
//            message.setTo("757077613@qq.com");
//            message.setSubject("新用户注册：" + content);
//            message.setText("欢迎：" + content);
//            //发送邮件
//            javaMailSender.send(message);
//        } catch (Exception e) {
//            log.error("send email error, content:{}", content, e);
//        }
//    }
}
