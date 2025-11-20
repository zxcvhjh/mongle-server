-- 챗봇 질문-답변 로그 테이블 생성
CREATE TABLE chatbot_query_log (
    id VARCHAR(26) PRIMARY KEY COMMENT 'ULID',
    question VARCHAR(2000) NOT NULL COMMENT '사용자 질문',
    answer TEXT COMMENT 'AI 답변',
    has_answer BOOLEAN NOT NULL DEFAULT FALSE COMMENT '답변 존재 여부',
    has_images BOOLEAN NOT NULL DEFAULT FALSE COMMENT '이미지 존재 여부',
    reference_url VARCHAR(500) COMMENT '참고 URL',
    response_time_ms BIGINT NOT NULL COMMENT '응답 시간(밀리초)',
    is_success BOOLEAN NOT NULL DEFAULT TRUE COMMENT '성공 여부',
    error_message VARCHAR(1000) COMMENT '에러 메시지',
    created_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시간',
    updated_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시간',
    INDEX idx_created_date (created_date),
    INDEX idx_is_success (is_success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='챗봇 질문-답변 로그';
