-- V10__add_version_column_to_post.sql

-- Post 엔티티에 @Version 필드가 추가되었으므로, 해당 컬럼을 DB에 추가합니다.
ALTER TABLE post
    ADD COLUMN version BIGINT NULL;
