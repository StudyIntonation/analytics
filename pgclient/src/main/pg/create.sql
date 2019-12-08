CREATE TABLE IF NOT EXISTS "user"
(
    id             BIGSERIAL PRIMARY KEY,
    gender         VARCHAR(6)  NOT NULL,
    age            INTEGER NOT NULL,
    first_language CHAR(5) NOT NULL
);

CREATE TABLE IF NOT EXISTS attempt_report
(
    id              BIGSERIAL PRIMARY KEY,
    uid             BIGINT      NOT NULL,
    cid             VARCHAR(32) NOT NULL,
    lid             VARCHAR(32) NOT NULL,
    tid             VARCHAR(32) NOT NULL,
    raw_pitch       INTEGER[]     NOT NULL,
    raw_sample_rate INTEGER     NOT NULL,
    dtw             FLOAT       NOT NULL,

    FOREIGN KEY (uid) REFERENCES "user" (id)
);
