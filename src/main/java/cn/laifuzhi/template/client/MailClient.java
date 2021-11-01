package cn.laifuzhi.template.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public final class MailClient {
    @Resource
    private JavaMailSender sender;

    public void send(String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("757077613@qq.com");
            message.setTo("757077613@qq.com");
            message.setSubject("新用户注册：" + content);
            message.setText("欢迎：" + content);
            //发送邮件
            sender.send(message);
//            MimeMessage mimeMessage = sender.createMimeMessage();
//            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
//            mimeMessageHelper.setFrom();
        } catch (Exception e) {
            log.error("send email error, content:{}", content, e);
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
