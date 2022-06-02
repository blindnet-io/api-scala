create table storage_objects
(
    app_id   uuid not null
        constraint storage_objects_apps_id_fk
            references apps
            on delete cascade,
    id       uuid not null,
    user_id  text,
    token_id text,
    meta     text,
    constraint storage_objects_pk
        primary key (app_id, id),
    constraint storage_objects_users_app_id_fk
        foreign key (app_id, user_id) references users (app, id)
            on delete cascade,
    constraint storage_objects_either_ids_check
        check (num_nonnulls(user_id, token_id) = 1)
);
