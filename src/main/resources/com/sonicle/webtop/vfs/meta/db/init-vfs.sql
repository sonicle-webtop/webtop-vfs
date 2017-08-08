@DataSource[default@com.sonicle.webtop.vfs]

CREATE SCHEMA "vfs";

-- ----------------------------
-- Sequence structure for seq_stores
-- ----------------------------
DROP SEQUENCE IF EXISTS "vfs"."seq_stores";
CREATE SEQUENCE "vfs"."seq_stores";

-- ----------------------------
-- Table structure for sharing_links
-- ----------------------------
DROP TABLE IF EXISTS "vfs"."sharing_links";
CREATE TABLE "vfs"."sharing_links" (
"sharing_link_id" varchar(255) NOT NULL,
"domain_id" varchar(20) NOT NULL,
"user_id" varchar(100) NOT NULL,
"link_type" varchar(1) NOT NULL,
"store_id" int4 NOT NULL,
"file_path" text NOT NULL,
"file_hash" varchar(255) NOT NULL,
"created_on" timestamptz(6) NOT NULL,
"expires_on" timestamptz(6),
"auth_mode" varchar(1) NOT NULL,
"password" varchar(128),
"notify" bool DEFAULT true NOT NULL
)
WITH (OIDS=FALSE)

;

-- ----------------------------
-- Table structure for stores
-- ----------------------------
DROP TABLE IF EXISTS "vfs"."stores";
CREATE TABLE "vfs"."stores" (
"store_id" int4 DEFAULT nextval('"vfs".seq_stores'::regclass) NOT NULL,
"domain_id" varchar(20) NOT NULL,
"user_id" varchar(100) NOT NULL,
"built_in" int2 DEFAULT 0 NOT NULL,
"name" varchar(50) NOT NULL,
"provider" varchar(255) NOT NULL,
"uri" varchar(512),
"parameters" text
)
WITH (OIDS=FALSE)

;

-- ----------------------------
-- Alter Sequences Owned By 
-- ----------------------------

-- ----------------------------
-- Indexes structure for table sharing_links
-- ----------------------------
CREATE INDEX "sharing_links_ak2" ON "vfs"."sharing_links" USING btree ("domain_id", "user_id", "link_type", "store_id", "file_path");
CREATE INDEX "sharing_links_ak3" ON "vfs"."sharing_links" USING btree ("store_id", "file_path");
CREATE INDEX "sharing_links_ak1" ON "vfs"."sharing_links" USING btree ("sharing_link_id", "link_type");

-- ----------------------------
-- Primary Key structure for table sharing_links
-- ----------------------------
ALTER TABLE "vfs"."sharing_links" ADD PRIMARY KEY ("sharing_link_id");

-- ----------------------------
-- Indexes structure for table stores
-- ----------------------------
CREATE INDEX "stores_ak1" ON "vfs"."stores" USING btree ("domain_id", "user_id", "built_in");
CREATE INDEX "stores_ak2" ON "vfs"."stores" USING btree ("domain_id", "user_id", "name");

-- ----------------------------
-- Primary Key structure for table stores
-- ----------------------------
ALTER TABLE "vfs"."stores" ADD PRIMARY KEY ("store_id");

-- ----------------------------
-- Align service version
-- ----------------------------
@DataSource[default@com.sonicle.webtop.core]
DELETE FROM "core"."settings" WHERE ("settings"."service_id" = 'com.sonicle.webtop.vfs') AND ("settings"."key" = 'manifest.version');
INSERT INTO "core"."settings" ("service_id", "key", "value") VALUES ('com.sonicle.webtop.vfs', 'manifest.version', '5.0.6');
