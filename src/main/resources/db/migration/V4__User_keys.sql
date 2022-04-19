create table user_keys
(
    user_id text not null,
    app_id uuid not null
        constraint user_keys_apps_id_fk
            references apps
            on delete cascade,
    pub_enc_key        text not null,
    pub_sign_key       text not null,
    enc_priv_enc_key   text not null,
    enc_priv_sign_key  text not null,
    key_deriv_salt     text not null,
    signed_pub_enc_key text not null,
    constraint user_keys_pk
        primary key (user_id, app_id),
    constraint user_keys_users_id_app_fk
        foreign key (user_id, app_id) references users (id, app) on delete cascade
);

insert into user_keys (user_id, app_id, pub_enc_key, pub_sign_key, enc_priv_enc_key, enc_priv_sign_key, key_deriv_salt, signed_pub_enc_key)
select id, app, pub_enc_key, pub_sign_key, enc_priv_enc_key, enc_priv_sign_key, key_deriv_salt, signed_pub_enc_key from users;

alter table users
    drop column pub_enc_key,
    drop column pub_sign_key,
    drop column enc_priv_enc_key,
    drop column enc_priv_sign_key,
    drop column key_deriv_salt,
    drop column signed_pub_enc_key;
