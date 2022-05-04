-- this is ok here as the message table is not usable before this migration,
-- and it prevents an error while adding not null columns if some test messages
-- were added before running this migration
delete from messages;

alter table messages
    add column public_ik text not null,
    add column public_ek text not null;
