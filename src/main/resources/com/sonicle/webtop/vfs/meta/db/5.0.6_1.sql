@DataSource[default@com.sonicle.webtop.vfs]

-- ----------------------------
-- Update structure for sharing_links
-- ----------------------------
ALTER TABLE "vfs"."sharing_links" ADD COLUMN "notify" bool DEFAULT true NOT NULL;
