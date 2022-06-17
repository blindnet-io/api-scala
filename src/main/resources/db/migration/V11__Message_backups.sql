create table message_backups
(
    app_id  uuid not null,
    user_id text not null,
    id      text not null,
    salt    text not null,
    constraint message_backups_pk
        primary key (app_id, user_id),
    constraint message_backups_users_app_id_fk
        foreign key (app_id, user_id) references users (app, id)
            on delete cascade
);
