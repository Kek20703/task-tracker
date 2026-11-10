create table users
(
    id         bigint generated always as identity primary key,
    email      varchar(254) not null,
    password   varchar(100) not null,
    created_at timestamptz  not null default now()
);

-- Уникальность без учёта регистра гарантирует сама база, а не только код приложения.
create unique index uq_users_email_lower on users (lower(email));
