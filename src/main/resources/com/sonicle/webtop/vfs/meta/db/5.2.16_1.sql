@DataSource[default@com.sonicle.webtop.vfs]

-- ----------------------------
-- Fix URI for mydocs built-in records
-- ----------------------------

UPDATE "vfs"."stores" SET
"uri" = 'mydocs://' || user_id || '%40' || domain_id || '@localhost'
WHERE provider = 'mydocs' AND built_in = 100;
