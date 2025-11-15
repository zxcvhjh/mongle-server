package com.algangi.mongle.global.constants;

/**
 * 사용자에게 표시되는 메시지 관련 상수 정의
 */
public final class MessageConstants {

    private MessageConstants() {
        // Utility class - prevent instantiation
    }

    /**
     * 게시글 생성 시 알림 봇 메시지 템플릿
     * 파라미터: (현재 게시글 수, 최대 게시글 수)
     */
    public static final String POST_CREATION_NOTIFICATION_TEMPLATE =
        "⚙️ 게시글 (%d/%d) · 5개 초과시 가장 오래된 게시글 삭제" +
        "\n⚙️ 24시간 후 게시글 자동 삭제";
}
