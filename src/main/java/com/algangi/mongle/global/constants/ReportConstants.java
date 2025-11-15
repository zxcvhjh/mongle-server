package com.algangi.mongle.global.constants;

/**
 * 신고 관련 상수 정의
 */
public final class ReportConstants {

    private ReportConstants() {
        // Utility class - prevent instantiation
    }

    /**
     * 신고 횟수 기준 - 이 횟수 이상 신고되면 자동으로 차단됩니다.
     */
    public static final int REPORT_BLOCK_THRESHOLD = 5;
}
