-- V6__increase_post_content_length.sql
ALTER TABLE post MODIFY COLUMN content VARCHAR(2000) NOT NULL;