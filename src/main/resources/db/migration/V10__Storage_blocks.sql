create table storage_blocks
(
    app_id    uuid not null,
    object_id uuid not null,
    id        uuid not null,
    constraint storage_blocks_pk
        primary key (app_id, object_id, id),
    constraint storage_blocks_objects_fk
        foreign key (app_id, object_id) references storage_objects (app_id, id)
            on delete cascade
);
