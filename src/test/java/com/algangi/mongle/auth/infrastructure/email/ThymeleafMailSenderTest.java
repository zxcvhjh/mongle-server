package com.algangi.mongle.auth.infrastructure.email;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.algangi.mongle.auth.application.service.email.EmailSanctionManager;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class ThymeleafMailSenderTest {

    private static final String TEST_EMAIL = "test@example.com";
    private static final String FROM_ADDRESS = "no-reply@mongle.com";

    private ThymeleafMailSender thymeleafMailSender;

    @Mock
    private JavaMailSender javaMailSender;
    @Mock
    private TemplateEngine templateEngine;
    @Mock
    private EmailSanctionManager emailSanctionManager;

    @BeforeEach
    void setUp() throws MessagingException {
        this.thymeleafMailSender = new ThymeleafMailSender(
            javaMailSender,
            templateEngine,
            FROM_ADDRESS,
            emailSanctionManager
        );

        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(javaMailSender.createMimeMessage()).thenReturn(mockMimeMessage);

        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(
            "<html>...</html>");
    }

    @Test
    @DisplayName("정상 발송 시 MailException이나 MessagingException이 발생하지 않아야 한다")
    void send_Success_NoExceptionThrown() throws MessagingException {
        // Given
        doNothing().when(javaMailSender).send(any(MimeMessage.class));

        // When & Then (No exception expected)
        thymeleafMailSender.send(TEST_EMAIL, "제목", "템플릿", Collections.emptyMap());

        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
        verify(emailSanctionManager, never()).recordHardBounceAndSanction(anyString());
    }

    @Test
    @DisplayName("영구적 오류 발생 시 (Invalid Address) 하드 바운스를 기록하고 예외를 다시 던진다")
    void send_Failure_RecordsHardBounceAndRethrows_OnPermanentError() {
        // Given

        MailSendException hardBounceException = new MailSendException("메일 발송 실패",
            new MessagingException("550 Invalid Address for " + TEST_EMAIL));
        doThrow(hardBounceException).when(javaMailSender).send(any(MimeMessage.class));

        // When & Then
        assertThrows(MessagingException.class, () -> {
            thymeleafMailSender.send(TEST_EMAIL, "제목", "템플릿", Collections.emptyMap());
        });

        // 1. EmailSanctionManager.recordHardBounceAndSanction()이 호출되었는지 검증 (필터 통과)
        verify(emailSanctionManager, times(1)).recordHardBounceAndSanction(eq(TEST_EMAIL));

        // 2. javaMailSender는 호출되었음
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("일시적 오류 발생 시 (Temporary Failure) 하드 바운스를 기록하지 않고 예외만 던진다")
    void send_Failure_DoesNotRecordHardBounce_OnTemporaryError() {
        // Given
        MailSendException temporaryError = new MailSendException("메일 발송 실패: Connection timed out");
        doThrow(temporaryError).when(javaMailSender).send(any(MimeMessage.class));

        // When & Then
        assertThrows(MessagingException.class, () -> {
            thymeleafMailSender.send(TEST_EMAIL, "제목", "템플릿", Collections.emptyMap());
        });

        // 1. EmailSanctionManager.recordHardBounceAndSanction()이 호출되지 않았는지 검증 (필터 미통과)
        verify(emailSanctionManager, never()).recordHardBounceAndSanction(anyString());

        // 2. javaMailSender는 호출되었음
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }
}