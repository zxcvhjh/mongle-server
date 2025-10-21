package com.algangi.mongle.auth.infrastructure.email;

import com.algangi.mongle.auth.application.service.email.MailSender;
import com.algangi.mongle.auth.exception.AuthErrorCode;
import com.algangi.mongle.global.exception.ApplicationException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Thymeleaf 템플릿 엔진을 사용하여 HTML 이메일을 발송하는 MailSender 구현체입니다.
 *
 * @Primary 어노테이션을 통해 기본 MailSender 구현으로 사용됩니다.
 */
@Component
@Primary // 이 구현체를 MailSender의 기본 구현으로 설정합니다.
public class ThymeleafMailSender implements MailSender {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final String fromAddress;

    public ThymeleafMailSender(
        JavaMailSender javaMailSender,
        TemplateEngine templateEngine,
        @Value("${app.mail.from-address}") String fromAddress
    ) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
        this.fromAddress = fromAddress;
    }

    /**
     * Thymeleaf 템플릿을 사용하여 HTML 이메일을 발송합니다.
     *
     * @param to               수신자 이메일 주소
     * @param subject          이메일 제목
     * @param templateName     사용할 HTML 템플릿의 이름 (예: "email-verification")
     * @param contextVariables 템플릿에 전달할 동적 데이터 맵 (예: {"verificationCode", "123456"})
     */
    @Override
    public void send(String to, String subject, String templateName,
        Map<String, Object> contextVariables) {
        Context context = new Context();
        context.setVariables(contextVariables);

        try {
            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);

        } catch (MessagingException e) {
            throw new ApplicationException(AuthErrorCode.VERIFICATION_CODE_SEND_FAILED, e);
        }
    }
}
