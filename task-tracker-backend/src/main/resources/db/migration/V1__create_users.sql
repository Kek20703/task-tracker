create table users
(
    id         bigint generated always as identity primary key,
    email      varchar(254) not null,
    password   varchar(100) not null,
    created_at timestamptz  not null default now(),
    constraint uq_users_email unique (email)
);
