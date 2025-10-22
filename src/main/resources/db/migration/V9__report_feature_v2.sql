-- V9__report_feature_v2.sql

-- 1. Post 테이블에 신고 카운트 추가
ALTER TABLE post
    ADD COLUMN report_count BIGINT NOT NULL DEFAULT 0;

-- 2. Comment 테이블에 신고 카운트 추가
ALTER TABLE comment
    ADD COLUMN report_count BIGINT NOT NULL DEFAULT 0;

-- 3. report 테이블의 reporter_id를 nullable로 변경
-- 기존 FK 제약조건을 삭제하고 nullable로 변경 후, 새 FK를 추가합니다.
ALTER TABLE report
DROP FOREIGN KEY FK1uivt2jamt7slp3banldgnsef;

ALTER TABLE report
    MODIFY COLUMN reporter_id VARCHAR(255) NULL;

-- nullable로 변경된 후, 기존 FK 제약조건과 동일한 이름으로 다시 추가합니다.
ALTER TABLE report
    ADD CONSTRAINT FK1uivt2jamt7slp3banldgnsef FOREIGN KEY (reporter_id) REFERENCES member (member_id);
