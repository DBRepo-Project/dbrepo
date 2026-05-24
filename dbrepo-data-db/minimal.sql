create table md_data_source
(
    id_datasource                         varchar(255) not null
        primary key,
    datasource_identifier                 varchar(255) null,
    full_citation                         text         null,
    publication_year                      varchar(255) null,
    publication_type                      varchar(255) null,
    publication_type_comments             varchar(255) null,
    data_owner_contact_organisation       varchar(255) null,
    data_owner_name                       varchar(255) null,
    data_owner_contact                    varchar(255) null,
    licensing_type                        varchar(255) null,
    licensing_comments                    varchar(255) null,
    data_supplier_name                    varchar(255) null,
    comments                              varchar(255) null,
    created_at                            varchar(255) null,
    created_by                            varchar(255) null,
    data_owner_website                    varchar(255) null,
    data_license                          text         null,
    data_license_url                      varchar(255) null,
    publication_possible                  varchar(255) null,
    sampling_sites_anonymization_required varchar(255) null
)
    with system versioning;

create table wb_single_measurements
(
    id_single_meas                 varchar(255) not null
        primary key,
    sample_identifier              varchar(255) null,
    name_determinand               varchar(255) null,
    observed_value                 varchar(255) null,
    unit_of_measure                varchar(255) null,
    loq                            varchar(255) null,
    value_below_loq                varchar(255) null,
    lod                            varchar(255) null,
    value_below_lod                varchar(255) null,
    analysed_matrix                varchar(255) null,
    analysed_matrix_comments       varchar(255) null,
    cen_iso_code_analytical_method varchar(255) null,
    analysis_method                varchar(255) null,
    analysis_method_accredited     varchar(255) null,
    datasource_identifier          varchar(255) null,
    comments                       varchar(255) null,
    created_at                     varchar(255) null,
    created_by                     varchar(255) null,
    name_lab                       varchar(255) null,
    lab_analysis_original_id       varchar(255) null,
    sample_preparation_method      varchar(255) null
)
    with system versioning;

