-- Modified for H2
-- Assume id=1 is invalid
-- Assume id=2 is still valid token
CREATE VIEW IF NOT EXISTS fda.mdb_invalid_tokens AS
(SELECT `id`, `token_hash`, `creator`, `created`, `expires`, `last_used` FROM fda.`mdb_tokens` WHERE `id` = 1);