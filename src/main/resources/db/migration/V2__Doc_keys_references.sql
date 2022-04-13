alter table document_keys
    add constraint document_keys_documents_id_app_fk
        foreign key (document_id, app_id) references documents (id, app) on delete cascade,
    add constraint document_keys_users_id_app_fk
        foreign key (user_id, app_id) references users (id, app) on delete cascade;
