-- V8__create_email_sanction_table.sql

-- 이메일 주소 제재 및 발송 실패 횟수를 기록하기 위한 테이블을 생성합니다.
CREATE TABLE email_sanction (
                                email VARCHAR(255) NOT NULL,
                                hard_bounce_count INT NOT NULL DEFAULT 0,
                                is_banned TINYINT(1) NOT NULL DEFAULT 0,
                                created_date DATETIME(6) NOT NULL,
                                updated_date DATETIME(6) NOT NULL,
                                PRIMARY KEY (email)
) ENGINE=InnoDB;