-- V5__add_post_view_log.sql

-- Create Table
CREATE TABLE post_view_log (
    member_id VARCHAR(255) NOT NULL,
    post_id VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    id VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- Add Unique Constraint
ALTER TABLE post_view_log ADD CONSTRAINT uk_post_view_log_user_post UNIQUE (member_id, post_id);

-- Add Foreign Key
ALTER TABLE post_view_log ADD CONSTRAINT fk_post_view_log_member FOREIGN KEY (member_id) REFERENCES member (member_id);
ALTER TABLE post_view_log ADD CONSTRAINT fk_post_view_log_post FOREIGN KEY (post_id) REFERENCES post(id);

-- Create Index
CREATE INDEX idx_post_view_log_member_id_created_date ON post_view_log (member_id, created_date);