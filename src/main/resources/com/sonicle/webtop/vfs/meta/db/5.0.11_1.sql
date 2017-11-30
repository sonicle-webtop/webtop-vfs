@DataSource[default@com.sonicle.webtop.vfs]

-- ----------------------------
-- Fix data
-- ----------------------------
UPDATE "vfs"."stores" SET "uri" = replace("uri", 'webdav:', 'webdavs:') WHERE provider IN ('nextcloud');
