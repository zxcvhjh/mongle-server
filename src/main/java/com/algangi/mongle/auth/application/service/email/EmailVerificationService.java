package com.algangi.mongle.auth.application.service.email;

import com.algangi.mongle.auth.exception.AuthErrorCode;
import com.algangi.mongle.auth.exception.DisposableEmailException;
import com.algangi.mongle.auth.exception.RateLimitExceededException;
import com.algangi.mongle.auth.presentation.dto.VerifyEmailRequest;
import com.algangi.mongle.auth.presentation.dto.VerifyEmailResponse;
import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.global.util.ClientIpUtils;
import com.algangi.mongle.member.application.service.MemberFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final MemberFinder memberFinder;
    private final MailSender mailSender;
    private final EmailVerificationCodeManager emailVerificationCodeManager;
    private final VerificationTokenManager verificationTokenManager;
    private final ClientIpUtils clientIpUtils;

    @Value("${app.security.rate-limit.duration-minutes}")
    private long rateLimitDurationMinutes;

    @Value("${app.security.rate-limit.max-requests}")
    private int maxRequests;

    @Value("${app.security.disposable-email-domains}")
    private List<String> disposableEmailDomains;

    public void sendVerificationCode(String email) {
        checkPolicies(email);
        memberFinder.validateDuplicateEmail(email);

        String code = generateRandomCode();
        emailVerificationCodeManager.save(email, code);

        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("verificationCode", code);

        mailSender.send(
            email,
            "Mongle 회원가입 인증 코드입니다.",
            "email-verification",
            templateVariables
        );
    }

    private void checkPolicies(String email) {
        checkIpRateLimit();
        checkDisposableEmail(email);
    }

    private void checkIpRateLimit() {
        String clientIp = clientIpUtils.getClientIpAddress()
            .orElseThrow(() -> new IllegalStateException("Cannot determine client IP address"));

        String rateLimitKey = "email-verification:ip-rate-limit:" + clientIp;

        Long attempts = redisTemplate.opsForValue().increment(rateLimitKey);

        if (attempts == null) {
            throw new IllegalStateException(
                "Redis increment operation failed for key: " + rateLimitKey);
        }

        if (attempts == 1) {
            redisTemplate.expire(rateLimitKey, Duration.ofMinutes(rateLimitDurationMinutes));
        }

        if (attempts > maxRequests) {
            throw new RateLimitExceededException();
        }
    }

    private void checkDisposableEmail(String email) {
        String domain = email.substring(email.indexOf("@") + 1);
        if (disposableEmailDomains.contains(domain)) {
            throw new DisposableEmailException();
        }
    }

    public VerifyEmailResponse verifyEmail(VerifyEmailRequest request) {
        String email = request.email();
        String verificationCode = request.verificationCode();

        if (!StringUtils.hasText(verificationCode)) {
            throw new IllegalArgumentException("이메일 인증코드는 빈 값일 수 없습니다.");
        }

        String savedCode = emailVerificationCodeManager.getCode(email);
        if (savedCode == null) {
            throw new ApplicationException(AuthErrorCode.VERIFICATION_CODE_EXPIRED);
        }
        if (!savedCode.equals(verificationCode)) {
            throw new ApplicationException(AuthErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        emailVerificationCodeManager.deleteCode(email);

        String verificationToken = verificationTokenManager.generate(email);
        return new VerifyEmailResponse(verificationToken);
    }

    private String generateRandomCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }
}
