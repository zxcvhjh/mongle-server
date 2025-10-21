package com.algangi.mongle.auth.application.service.email;

import java.util.Map;

/**
 * 이메일 발송 기능을 위한 인터페이스입니다. 이 인터페이스를 구현하는 클래스는 특정 기술(예: Thymeleaf, Freemarker)을 사용하여 템플릿 기반의 이메일을
 * 발송하는 로직을 담당합니다.
 */
public interface MailSender {

    /**
     * 템플릿을 사용하여 이메일을 발송합니다.
     *
     * @param to               수신자 이메일 주소
     * @param subject          이메일 제목
     * @param templateName     사용할 템플릿의 이름 (예: "email-verification")
     * @param contextVariables 템플릿에 전달할 동적 데이터 맵 (예: {"verificationCode", "123456"})
     */
    void send(String to, String subject, String templateName, Map<String, Object> contextVariables);
}

