create table apps
(
    id         uuid not null
        constraint apps_pk
            primary key,
    public_key text not null,
    name       text not null
);

create table users
(
    id                 text not null,
    app                uuid not null
        constraint users_apps_id_fk
            references apps
            on delete cascade,
    pub_enc_key        text not null,
    pub_sign_key       text not null,
    enc_priv_enc_key   text not null,
    enc_priv_sign_key  text not null,
    key_deriv_salt     text not null,
    signed_pub_enc_key text not null,
    group_id           text,
    constraint users_pk
        primary key (id, app)
);

create table documents
(
    id  uuid not null,
    app uuid not null
        constraint documents_apps_id_fk
            references apps
            on delete cascade,
    constraint documents_pk
        primary key (id, app)
);

create table document_keys
(
    document_id uuid not null,
    user_id     text not null,
    enc_sym_key text not null,
    app_id      uuid not null
        constraint document_keys_apps_id_fk
            references apps
            on delete cascade,
    constraint document_keys_pk
        primary key (document_id, user_id, app_id)
);
