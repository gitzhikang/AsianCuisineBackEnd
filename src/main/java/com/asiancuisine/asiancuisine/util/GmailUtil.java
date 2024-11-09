package com.asiancuisine.asiancuisine.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.util.Properties;

public class GmailUtil {

    private final Logger logger = LoggerFactory.getLogger(GmailUtil.class);

    private final String senderEmail;

    private final String senderPassword;

    public GmailUtil(String senderEmail, String senderPassword, String toUser) {
        this.senderEmail = senderEmail;
        this.senderPassword = senderPassword;
        this.toUser = toUser;
    }

    private final String toUser;

    private static final String subject = "Code review finished!";

    public void publishMessage(String logUrl,String content){
        // 设置 Gmail 的 SMTP 属性
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        // 邮箱认证
        final String myAccountEmail = senderEmail;  // 你的 Gmail 地址
        final String password = senderPassword;  // Gmail 应用程序密码

        // 创建会话
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(myAccountEmail, password);
            }
        });

        try {
            // 创建带有 HTML 内容的邮件
            Message message = prepareMessage(session, myAccountEmail, toUser, subject, content);

            // 发送邮件
            Transport.send(message);
            logger.info("HTML 邮件已发送成功！");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    private static Message prepareMessage(Session session, String myAccountEmail, String recipient, String subject, String htmlContent) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(myAccountEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            message.setSubject(subject);

            // 设置邮件内容为 HTML
            message.setContent(htmlContent, "text/html;charset=utf-8");

            return message;
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
