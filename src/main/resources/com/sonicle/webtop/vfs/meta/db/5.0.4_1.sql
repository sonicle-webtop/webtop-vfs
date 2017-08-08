@DataSource[default@com.sonicle.webtop.vfs]

-- ----------------------------
-- Update structure for stores
-- ----------------------------
ALTER TABLE "vfs"."stores" ADD COLUMN "provider" varchar(255);

-- ----------------------------
-- Fix data
-- ----------------------------
UPDATE "vfs"."stores" SET "provider" = substring(uri from '^[^:]*');
UPDATE "vfs"."stores" SET "provider" = 'ftp' WHERE provider IN ('sftp','ftps');

-- ----------------------------
-- Update structure for stores
-- ----------------------------
ALTER TABLE "vfs"."stores" ALTER COLUMN "provider" SET NOT NULL;
