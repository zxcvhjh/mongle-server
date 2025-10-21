-- V7__add_is_anonymous.sql

ALTER TABLE post
    ADD COLUMN is_anonymous TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE comment
    ADD COLUMN is_anonymous TINYINT(1) NOT NULL DEFAULT 0;