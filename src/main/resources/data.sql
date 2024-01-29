CREATE TABLE IF NOT EXISTS migration
(
    id             bigserial primary key,
    entityName     varchar,
    total          bigint,
    exported       int,
    lastRequestUrl varchar,
    createdDate    timestamp,
    status         varchar
);
