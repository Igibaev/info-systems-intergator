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

CREATE TABLE IF NOT EXISTS stat_period
(
    id       bigint  not null,
    text     varchar not null
);
COMMENT ON COLUMN stat_period.id IS 'Уникальный идентификатор периода для статистики, нужен для выборки';
COMMENT ON COLUMN stat_period.text IS 'Текстовое значение периода';

CREATE TABLE IF NOT EXISTS stats_periods
(
    stat_period_id       bigint  not null,
    stat_id             bigint not null
);
COMMENT ON COLUMN stats_periods.stat_period_id IS 'Связывает ИД периода с ИД статистики';
COMMENT ON COLUMN stats_periods.stat_id IS 'Связывает ИД статистики с ИД периода';

CREATE TABLE IF NOT EXISTS stat
(
    id       bigint  not null,
    name     varchar not null,
    code     varchar,
    info     varchar
);
COMMENT ON COLUMN stat.id IS 'ИД статистики';
COMMENT ON COLUMN stat.name IS 'Наименование стат';
COMMENT ON COLUMN stat.code IS 'Связывает ИД статистики с ИД периода';
COMMENT ON COLUMN stat.info IS 'Связывает ИД статистики с ИД периода';

CREATE TABLE IF NOT EXISTS stat_info
(
    id                          bigint  not null,
    name_path                    varchar,
    name                        varchar,
    short_name                   varchar,
    full_code                    varchar,
    cg_params                    varchar,
    term_Names                   varchar,
    measure_id                   varchar,
    measure_kfc                  varchar,
    measure_sign                 varchar,
    measure_name                 varchar,
    preferred_measure_id          varchar,
    preferred_measure_name        varchar,
    preferred_measure_kfc         varchar,
    preferred_measure_sign        varchar
);

CREATE TABLE IF NOT EXISTS stat_info_passport
(
    stat_id   bigint  not null,
    title     varchar not null,
    value     varchar
);

CREATE TABLE IF NOT EXISTS stat_measure
(
    stat_measure_id     varchar  not null,
    id     varchar  not null,
    text   varchar not null,
    kfc    bigint,
    sign   varchar,
    leaf   boolean,
    expand boolean
);

CREATE TABLE IF NOT EXISTS stat_segment
(
    dic_id         varchar,
    dic_class_id    varchar,
    names         varchar,
    full_names     varchar,
    term_ids       varchar,
    term_names     varchar,
    dic_count      int,
    idx           int,
    id            int,
    segment_order int,
    dec_format     int,
    keyword_dic    varchar,
    max_date       varchar,
    stat_period_id  bigint,
    stat_id        bigint
);

CREATE TABLE IF NOT EXISTS stat_combinations
(
    dic_count        int,
    full_text        varchar,
    id               varchar,
    text             varchar,
    stat_period_id   bigint,
    stat_id          bigint
);

CREATE TABLE IF NOT EXISTS stat_filters
(
    id                    bigint,
    parent_id             bigint,
    text                  varchar,
    stat_Period_Id        bigint,
    stat_Id               bigint,
    dic_ids varchar,
    dic varchar
);
CREATE UNIQUE INDEX IF NOT EXISTS stat_filters_unique ON stat_filters (id, stat_period_id, stat_id, dic_ids, dic);

CREATE TABLE IF NOT EXISTS stat_data
(
    id                    varchar,
    rownum                varchar,
    text                  varchar,
    leaf                  boolean,
    expanded              boolean,
    measure_Name           varchar,
    stat_id               bigint,
    stat_period_id        bigint,
    stat_measure_id       varchar,
    stat_segment_term_ids varchar,
    stat_segment_dic_ids  varchar,
    stat_filter_id        bigint,
    stat_filter_term_ids  varchar,
    parent_id             varchar,
    date_Data_Map         varchar
);

CREATE UNIQUE INDEX IF NOT EXISTS stat_data_unique ON stat_data
    (id, rownum, text, leaf, expanded, measure_name, stat_id, stat_period_id, stat_measure_id, stat_segment_term_ids,
    stat_segment_dic_ids, stat_filter_id, stat_filter_term_ids, parent_id, date_data_map);

CREATE TABLE IF NOT EXISTS stat_migration_status
(
    period_id bigint,
    stat_id bigint,
    total bigint
);

CREATE TABLE IF NOT EXISTS stat_migration_busy
(
    period_id bigint,
    stat_id bigint
);