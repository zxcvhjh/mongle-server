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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailVerificationService, "rateLimitDurationMinutes", 5L);
        ReflectionTestUtils.setField(emailVerificationService, "maxRequests", 10);
        ReflectionTestUtils.setField(emailVerificationService, "disposableEmailDomains",
            List.of("yopmail.com"));
    }

    @Test
    @DisplayName("이메일 인증 코드 발송 시 HTML 템플릿과 함께 MailSender를 올바르게 호출한다")
    void sendVerificationCode_sendsHtmlEmailSuccessfully() {
        // given
        String testEmail = "test@example.com";

        // Mock 객체들의 동작을 정의
        doNothing().when(memberFinder).validateDuplicateEmail(anyString());
        when(clientIpUtils.getClientIpAddress()).thenReturn(Optional.of("127.0.0.1"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

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
}

