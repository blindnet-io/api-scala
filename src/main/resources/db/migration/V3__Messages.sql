create table messages
(
    id bigint not null,
    app_id uuid not null
        constraint messages_apps_id_fk
            references apps
            on delete cascade,
    sender_id text not null,
    recipient_id text not null,
    data text not null,
    time_sent timestamptz,
    constraint messages_pk
        primary key (id, app_id),
    constraint messages_sender_users_id_app_fk
        foreign key (sender_id, app_id) references users (id, app) on delete cascade,
    constraint messages_recipient_users_id_app_fk
        foreign key (recipient_id, app_id) references users (id, app) on delete cascade
);
