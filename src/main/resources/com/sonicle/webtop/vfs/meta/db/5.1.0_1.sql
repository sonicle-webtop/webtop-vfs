@DataSource[default@com.sonicle.webtop.core]

-- ----------------------------
-- Fix data
-- ----------------------------
UPDATE "core"."user_settings"
SET "key" = 'files.showhidden'
WHERE "service_id" = 'com.sonicle.webtop.vfs' AND "key" = 'hiddenfiles.show';
