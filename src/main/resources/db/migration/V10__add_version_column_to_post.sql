-- V10__add_version_column_to_post.sql

-- Post 엔티티에 @Version 필드가 추가되었으므로, 해당 컬럼을 DB에 추가하고 NOT NULL DEFAULT 0 제약 조건을 설정합니다.
ALTER TABLE post
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0; -- <<< 수정: NOT NULL DEFAULT 0

-- Comment 엔티티에도 @Version 필드가 있으므로, comment 테이블의 version 컬럼도 수정합니다.
ALTER TABLE comment
    MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0; -- <<< 추가: Comment 테이블 수정