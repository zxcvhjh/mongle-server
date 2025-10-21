package com.algangi.mongle.auth.application.service.email;

import java.util.Map;
import jakarta.mail.MessagingException;

public interface MailSender {

    /**
     * 템플릿을 사용하여 이메일을 발송합니다.
     *
     * @param to               수신자 이메일 주소
     * @param subject          이메일 제목
     * @param templateName     사용할 템플릿의 이름 (예: "email-verification")
     * @param contextVariables 템플릿에 전달할 동적 데이터 맵 (예: {"verificationCode", "123456"})
     */

    void send(String to, String subject, String templateName, Map<String, Object> contextVariables)
        throws MessagingException;
}