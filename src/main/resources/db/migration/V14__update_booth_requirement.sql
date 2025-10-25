ALTER TABLE post MODIFY COLUMN expired_at TIMESTAMP(6);

ALTER TABLE member MODIFY COLUMN member_role ENUM(
    'ADMIN',
    'USER',
    'BOOTH')
    NOT NULL;