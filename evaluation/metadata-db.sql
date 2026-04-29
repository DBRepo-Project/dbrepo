INSERT INTO mdb_tables (tdbid, tname, internal_name, queue_name, routing_key, tdescription, owned_by)
VALUES ('1ab235a4-8be3-46a2-b3e9-99dc66e514a1', 'wb_single_measurements', 'wb_single_measurements', 'dbrepo', 'dbrepo.#', null, 'mweise');

INSERT INTO mdb_columns (tid, cname, internal_name, datatype, length, ordinal_position, description,
                         is_null_allowed)
VALUES ('c8d008a4-5db6-4df6-851c-992b854a0158', 'id_single_meas', 'id_single_meas', 'INT', 11, 0, null, false),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'sample_identifier', 'sample_identifier', 'TEXT', null, 1, null, false),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'name_determinand', 'name_determinand', 'TEXT', null, 2, null, false),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'observed_value', 'observed_value', 'DOUBLE', null, 3, null, true),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'unit_of_measure', 'unit_of_measure', 'TEXT', null, 4, null, false),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'loq', 'loq', 'DOUBLE', null, 6, null, true),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'value_below_loq', 'value_below_loq', 'BOOL', null, 7, null, true),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'lod', 'lod', 'DOUBLE', null, 8, null, true),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'value_below_lod', 'value_below_lod', 'BOOL', null, 9, null, true),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'analysed_matrix', 'analysed_matrix', 'TEXT', null, 10, null, false),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'analysed_matrix_comments', 'analysed_matrix_comments', 'TEXT', null, 11, null, true),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'cen_iso_code_analytical_method','cen_iso_code_analytical_method', 'TEXT', null, 12, null, false),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'analysis_method','analysis_method', 'TEXT', null, 13, null, false),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'analysis_method_accredited','analysis_method_accredited', 'TEXT', null, 14, null, true),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'datasource_identifier','datasource_identifier', 'TEXT', null, 15, null, false),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'comments','comments', 'TEXT', null, 16, null, true),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'created_at','created_at', 'TIMESTAMP', null, 17, null, true),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'created_by','created_by', 'TEXT', null, 18, null, false),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'name_lab','name_lab', 'TEXT', null, 19, null, false),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'lab_analysis_original_id','lab_analysis_original_id', 'TEXT', null, 20, null, true),
       ('c8d008a4-5db6-4df6-851c-992b854a0158', 'sample_preparation_method','sample_preparation_method', 'TEXT', null, 21, null, false)