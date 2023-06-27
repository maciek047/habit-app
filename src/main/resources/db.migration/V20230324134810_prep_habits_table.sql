CREATE TABLE IF NOT EXISTS habits (
                                      id
                                      uuid
                                      PRIMARY
                                      KEY,
                                      name
                                      VARCHAR
(
                                      255
) NOT NULL,
                        description VARCHAR(255) NOT NULL
    -- Add other columns here
);

ALTER TABLE habits ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

UPDATE habits SET created_at = CURRENT_TIMESTAMP;

ALTER TABLE habits ALTER COLUMN created_at SET NOT NULL;