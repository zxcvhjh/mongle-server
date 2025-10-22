-- V10__add_version_column_to_post.sql (수정)

-- post 테이블에 version 컬럼이 누락되어 있으므로 추가합니다.
ALTER TABLE post
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- comment 테이블은 이미 version 컬럼이 있으므로 속성을 수정합니다.
ALTER TABLE comment
    MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;