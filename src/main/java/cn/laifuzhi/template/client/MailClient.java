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
        } catch (Exception e) {
            log.error("send email error, content:{}", content, e);
        }
    }
}
