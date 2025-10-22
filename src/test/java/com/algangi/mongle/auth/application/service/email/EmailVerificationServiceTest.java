package com.algangi.mongle.auth.application.service.email;

import com.algangi.mongle.global.util.ClientIpUtils;
import com.algangi.mongle.member.application.service.MemberFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algangi.mongle.auth.exception.AuthErrorCode;
import com.algangi.mongle.global.exception.ApplicationException;

import jakarta.mail.MessagingException;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Mock
    private MailSender mailSender;
    @Mock

    private MemberFinder memberFinder;
    @Mock
    private ClientIpUtils clientIpUtils;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private EmailVerificationCodeManager emailVerificationCodeManager;
    @Mock
    private VerificationTokenManager verificationTokenManager;
    @Mock
    private ValueOperations<String, String>
        valueOperations;

    @Mock
    private EmailSanctionManager emailSanctionManager;

    // 재사용을 위한 공통 Mock 설정
    private void mockSuccessfulRateLimitCheck() {
        // IP 주소는 항상 성공적으로 획득
        when(clientIpUtils.getClientIpAddress()).thenReturn(Optional.of("127.0.0.1"));
        // Redis opsForValue() 설정
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // Rate limit increment는 항상 1 (첫 시도)을 반환하여 통과
        when(valueOperations.increment(anyString())).thenReturn(1L);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailVerificationService, "rateLimitDurationMinutes", 5L);
        ReflectionTestUtils.setField(emailVerificationService, "maxRequests", 10);
    }

    @Test

    @DisplayName("이메일 인증 코드 발송 시 HTML 템플릿과 함께 MailSender를 올바르게 호출한다")
    void sendVerificationCode_sendsHtmlEmailSuccessfully()
        throws MessagingException {
        // given
        String testEmail = "test@example.com";

        mockSuccessfulRateLimitCheck(); // IP 및 Rate Limit 통과 Mocking

        // 1. 중복 이메일 검사 통과
        doNothing().when(memberFinder).validateDuplicateEmail(anyString());
        // 2. 벤 상태 아님
        when(emailSanctionManager.isBanned(testEmail)).thenReturn(false);
        // 3. MailSender는 성공적으로 실행
        doNothing().when(mailSender).send(anyString(), anyString(), anyString(), anyMap());

        // when
        emailVerificationService.sendVerificationCode(testEmail);

        // then
        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> templateNameCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Map> contextVariablesCaptor = ArgumentCaptor.forClass(Map.class);

        verify(mailSender,
            org.mockito.Mockito.description("메일 발송 메소드가 올바른 인자들로 호출되어야 합니다.")

        ).send(toCaptor.capture(), subjectCaptor.capture(), templateNameCaptor.capture(),
            contextVariablesCaptor.capture());

        assertThat(toCaptor.getValue()).isEqualTo(testEmail);
        assertThat(subjectCaptor.getValue()).isEqualTo("Mongle 회원가입 인증 코드입니다.");
        assertThat(templateNameCaptor.getValue()).isEqualTo("email-verification");

        Map<String, Object> capturedVariables = contextVariablesCaptor.getValue();
        assertThat(capturedVariables).containsKey("verificationCode");
        assertThat(capturedVariables.get("verificationCode").toString()).hasSize(6);
    }

    @Test
    @DisplayName("이메일 주소가 벤 상태일 경우 발송을 시도하지 않고 예외 발생")
    void sendVerificationCode_WhenEmailIsBanned_ThrowsException() throws MessagingException {
        // Given
        String bannedEmail = "banned@email.com";
        when(emailSanctionManager.isBanned(bannedEmail)).thenReturn(true);

        // IP 제한 검사를 시도하므로, 이 부분이 실패하지 않도록 설정 (IP 획득)
        when(clientIpUtils.getClientIpAddress()).thenReturn(Optional.of("127.0.0.1"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // When & Then
        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            emailVerificationService.sendVerificationCode(bannedEmail);
        });

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.EMAIL_IS_BANNED);
        verify(mailSender, never()).send(anyString(), anyString(), anyString(), anyMap());
        // verify(clientIpUtils, times(1)).getClientIpAddress(); // IP 검사 호출은 되어야 함
    }

    @Test
    @DisplayName("MailSender에서 MessagingException 발생 시 서비스 계층에서 실패 예외로 변환")
    void sendVerificationCode_WhenMessagingExceptionOccurs_ThrowsSendFailedException()
        throws MessagingException {
        // Given
        String testEmail = "error@email.com";

        mockSuccessfulRateLimitCheck(); // IP 및 Rate Limit 통과 Mocking

        // 1. 벤 상태 아님
        when(emailSanctionManager.isBanned(testEmail)).thenReturn(false);

        // 2. MailSender가 MessagingException을 던지도록 설정 (MailSender 내부에서 하드 바운스 기록됨)
        doThrow(MessagingException.class).when(mailSender)
            .send(anyString(), anyString(), anyString(), anyMap());

        // When & Then
        ApplicationException exception = assertThrows(ApplicationException.class, () -> {
            emailVerificationService.sendVerificationCode(testEmail);
        });

        assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.VERIFICATION_CODE_SEND_FAILED);
    }
}