-- this is ok here as the message table is not usable before this migration,
-- and it prevents an error while adding not null columns if some test messages
-- were added before running this migration
delete from messages;

alter table messages
    add column sender_device_id text not null,
    add column recipient_device_id text not null,
    add column time_delivered timestamptz,
    add column time_read timestamptz,
    add column dh_key text not null,
    add constraint messages_sender_devices_fk
        foreign key (sender_id, app_id, sender_device_id) references user_devices (user_id, app_id, id) on delete cascade,
    add constraint messages_recipient_devices_fk
        foreign key (recipient_id, app_id, recipient_device_id) references user_devices (user_id, app_id, id) on delete cascade;
