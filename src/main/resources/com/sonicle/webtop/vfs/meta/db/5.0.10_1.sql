@DataSource[default@com.sonicle.webtop.vfs]

-- ----------------------------
-- Update structure for stores
-- ----------------------------
ALTER TABLE "vfs"."stores" ALTER COLUMN "provider" TYPE varchar(20);
