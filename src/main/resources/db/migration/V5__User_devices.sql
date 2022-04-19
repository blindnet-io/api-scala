create table user_devices
(
    user_id      text not null,
    app_id       uuid not null
        constraint user_devices_apps_id_fk
            references apps
            on delete cascade,
    id           text not null,
    pub_sign_key text not null,
    pub_ik_id    text not null,
    pub_ik       text not null,
    pub_spk_id   text not null,
    pub_spk      text not null,
    pk_sig       text not null,
    constraint user_devices_pk
        primary key (user_id, app_id, id),
    constraint user_devices_users_id_app_fk
        foreign key (user_id, app_id) references users (id, app) on delete cascade
);

create table one_time_keys
(
    user_id      text not null,
    app_id       uuid not null,
    device_id    text not null,
    id           text not null,
    key          text not null,
    constraint one_time_keys_pk
        primary key (user_id, app_id, device_id, id),
    constraint one_time_keys_devices_fk
        foreign key (user_id, app_id, device_id) references user_devices (user_id, app_id, id) on delete cascade
);
