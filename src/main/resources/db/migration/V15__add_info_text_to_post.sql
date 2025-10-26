-- V15__add_info_text_to_post.sql
-- post 테이블에 info_text 컬럼 추가
ALTER TABLE post
    ADD COLUMN info_text VARCHAR(2000) NULL COMMENT '알갱이 상태 게시글에만 표시될 수 있는 관리자용 텍스트';