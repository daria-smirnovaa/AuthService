create table user_credentials(
    id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY UNIQUE,
    username VARCHAR(30) NOT NULL UNIQUE,
    password VARCHAR(80) NOT NULL,
    email    VARCHAR(50) UNIQUE
);

create table refresh_tokens(
    id      bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY UNIQUE,
    user_id bigint NOT NULL,
    token   text   NOT NULL
);

INSERT INTO user_credentials (username, password, email)
VALUES ('user', '$2a$12$MTbrzkxTGAnaeKoL.1QSPO.LsJm7NqnK.GhDjqD8dfPqrQCVxEzjy', 'user@exaple.com'),
       ('admin', '$2a$12$iTcsff1KeAnxWXuPjefetOLdrbJ8nttIJ16FS0Avg1cIiTcS2Phpe',
        'admin@example.com');