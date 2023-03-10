INSERT INTO `qs_queries` (`created`, `executed`, `created_by`, `query`, `query_normalized`, `is_persisted`,
                          `query_hash`, `result_hash`, `result_number`)
VALUES ('2022-12-24 18:00:00', '2022-12-24 18:00:00', 'sclause', 'SELECT `present` FROM `bag`',
        'SELECT `present` FROM `bag`', false, 'e8aff3ca4caeb228b314e88f00be767407bc45656a96da208a4cea00b75cc8d8',
        '5a9977bb0b8653f18a6542f098b72e696a3584433db156ceb26047ee4f6f7e2b', 3),
       ('2022-12-24 18:00:01', '2022-12-24 18:00:01', 'sclause', 'SELECT `type`, `present` FROM `bag`',
        'SELECT `type`, `present` FROM `bag`', true, 'e8aff3ca4caeb228b314e88f00be767407bc45656a96da208a4cea00b75cc8d7',
        '5a9977bb0b8653f18a6542f098b72e696a3584433db156ceb26047ee4f6f7e2a', 3),
       (NOW(), NOW(), 'sclause', 'SELECT `id`, `present` FROM `bag`',
        'SELECT `id`, `present` FROM `bag`', false, 'e8aff3ca4caeb228b314e88f00be767407bc45656a96da208a4cea00b75cc8d9',
        '5a9977bb0b8653f18a6542f098b72e696a3584433db156ceb26047ee4f6f7e2c', 3);