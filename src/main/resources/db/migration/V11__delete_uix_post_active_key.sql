-- 1. 인덱스 삭제
DROP INDEX uix_posts_active_key ON post;

-- 2. 생성 컬럼 삭제
ALTER TABLE post DROP COLUMN active_key;