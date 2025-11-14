package com.algangi.mongle.report.application.service;

import com.algangi.mongle.comment.domain.model.Comment;
import com.algangi.mongle.comment.domain.service.CommentFinder;
import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.global.util.ClientIpUtils;
import com.algangi.mongle.member.application.service.MemberFinder;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.post.application.helper.PostFinder;
import com.algangi.mongle.post.domain.model.Post;
import com.algangi.mongle.report.domain.model.Report;
import com.algangi.mongle.report.domain.model.ReportStatus;
import com.algangi.mongle.report.domain.model.ReportedTargetType;
import com.algangi.mongle.report.domain.repository.ReportRepository;
import com.algangi.mongle.report.exception.ReportErrorCode;
import com.algangi.mongle.report.presentation.dto.ReportCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportCommandService {

    private static final String IP_RATE_LIMIT_KEY_PREFIX = "report:ip-rate-limit:";
    private static final int MAX_IP_REPORTS_PER_HOUR = 10;
    private static final Duration IP_RATE_LIMIT_DURATION = Duration.ofHours(1);
    private static final String HASH_ERROR_STRING = "HASH_ERROR";

    private final ReportRepository reportRepository;
    private final MemberFinder memberFinder;
    private final PostFinder postFinder;
    private final CommentFinder commentFinder;
    private final RedisTemplate<String, String> redisTemplate;
    private final ClientIpUtils clientIpUtils;

    @Transactional
    public void createReport(String reporterId, ReportCreateRequest request) {
        String targetAuthorId = getTargetAuthorIdAndValidate(request.targetType(),
            request.targetId());

        if (targetAuthorId != null && isProtectedAccount(targetAuthorId)) {
            throw new ApplicationException(ReportErrorCode.CANNOT_REPORT_ADMIN_OR_BOOTH);
        }

        if (reporterId == null) {
            handleUnauthenticatedReport(request, targetAuthorId);
            return;
        }

        Member reporter = memberFinder.getMemberOrThrow(reporterId);

        if (targetAuthorId != null && Objects.equals(reporter.getMemberId(), targetAuthorId)) {
            throw new ApplicationException(ReportErrorCode.SELF_REPORT_NOT_ALLOWED);
        }

        if (reportRepository.existsByReporter_MemberIdAndTargetIdAndTargetType(reporterId,
            request.targetId(), request.targetType())) {
            throw new ApplicationException(ReportErrorCode.DUPLICATE_REPORT);
        }

        Report report = Report.builder()
            .reporter(reporter)
            .targetId(request.targetId())
            .targetType(request.targetType())
            .targetAuthorId(targetAuthorId)
            .reason(request.reason())
            .build();

        reportRepository.save(report);
        incrementReportCountAndBlockIfNeeded(request.targetType(), request.targetId());
    }

    private void handleUnauthenticatedReport(ReportCreateRequest request, String targetAuthorId) {
        checkIpRateLimit();

        Report report = Report.builder()
            .reporter(null)
            .targetId(request.targetId())
            .targetType(request.targetType())
            .targetAuthorId(targetAuthorId)
            .reason(request.reason())
            .build();

        reportRepository.save(report);

        log.debug("Unauthenticated report recorded. TargetType={}, TargetId={}, IP(hash)={}",
            request.targetType(), request.targetId(),
            clientIpUtils.getClientIpAddress().map(this::hashIPForLogging).orElse("UNKNOWN"));

        incrementReportCountAndBlockIfNeeded(request.targetType(), request.targetId());
    }


    private void checkIpRateLimit() {
        String clientIp = clientIpUtils.getClientIpAddress()
            .orElseThrow(() -> new IllegalStateException("Cannot determine client IP address"));

        String rateLimitKey = IP_RATE_LIMIT_KEY_PREFIX + clientIp;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        Long attempts = ops.increment(rateLimitKey);

        if (attempts == null) {
            throw new IllegalStateException(
                "Redis increment operation failed for key: " + rateLimitKey);
        }

        if (attempts == 1) {
            redisTemplate.expire(rateLimitKey, IP_RATE_LIMIT_DURATION);
        }

        if (attempts > MAX_IP_REPORTS_PER_HOUR) {
            log.warn("IP Rate Limit Exceeded: IP(hash)={}, Attempts={}", hashIPForLogging(clientIp),
                attempts);
            throw new ApplicationException(ReportErrorCode.REPORT_RATE_LIMIT_EXCEEDED);
        }
    }

    @Transactional
    public void updateReportStatus(String reportId, ReportStatus newStatus) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new ApplicationException(ReportErrorCode.REPORT_NOT_FOUND));
        report.updateStatus(newStatus);
    }

    private void incrementReportCountAndBlockIfNeeded(ReportedTargetType targetType,
        String targetId) {
        switch (targetType) {
            case POST -> {
                Post post = postFinder.getPostWithPessimisticLockOrThrow(targetId);
                post.incrementReportCountAndBlockIfNeeded();
            }
            case COMMENT -> {
                Comment comment = commentFinder.getCommentWithPessimisticLockOrThrow(targetId);
                comment.incrementReportCountAndBlockIfNeeded();
            }
        }
    }

    private String getTargetAuthorIdAndValidate(ReportedTargetType targetType, String targetId) {
        return switch (targetType) {
            case POST -> {
                Post post = postFinder.getPostOrThrow(targetId);
                yield post.getAuthorId();
            }
            case COMMENT -> {
                Comment comment = commentFinder.getCommentOrThrow(targetId);
                yield Optional.ofNullable(comment.getMember()).map(Member::getMemberId)
                    .orElse(null);
            }
        };
    }

    private boolean isProtectedAccount(String memberId) {
        try {
            Member member = memberFinder.getMemberOrThrow(memberId);
            return member.getMemberRole() == com.algangi.mongle.member.domain.model.MemberRole.ADMIN
                || member.getMemberRole() == com.algangi.mongle.member.domain.model.MemberRole.BOOTH;
        } catch (ApplicationException e) {
            // If member not found, it's not a protected account
            return false;
        }
    }

    private String hashIPForLogging(String ip) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ip.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return HASH_ERROR_STRING;
        }
    }
}
