package com.algangi.mongle.report.presentation.dto;

import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.report.domain.model.Report;
import com.algangi.mongle.report.domain.model.ReportReason;
import com.algangi.mongle.report.domain.model.ReportStatus;
import com.algangi.mongle.report.domain.model.ReportedTargetType;
import java.util.Optional;

import java.time.Instant;

public record ReportAdminResponse(
    String reportId,
    ReporterInfo reporter,
    String targetId,
    ReportedTargetType targetType,
    TargetAuthorInfo targetAuthor,
    ReportReason reason,
    ReportStatus reportStatus,
    Instant createdAt
) {

    public static ReportAdminResponse from(Report report) {
        return new ReportAdminResponse(
            report.getId(),
            Optional.ofNullable(report.getReporter()).map(ReporterInfo::from)
                .orElseGet(ReporterInfo::anonymous),
            report.getTargetId(),
            report.getTargetType(),
            new TargetAuthorInfo(report.getTargetAuthorId()),
            report.getReason(),
            report.getReportStatus(),
            report.getCreatedDate()
        );
    }

    public record ReporterInfo(String memberId, String nickname) {

        public static ReporterInfo from(Member member) {
            if (member == null) {
                return anonymous();
            }
            return new ReporterInfo(member.getMemberId(), member.getNickname());
        }

        public static ReporterInfo anonymous() {
            return new ReporterInfo(null, "(비인증 사용자)");
        }
    }

    public record TargetAuthorInfo(String memberId) {

    }
}