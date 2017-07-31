@DataSource[default@com.sonicle.webtop.vfs]

ALTER TABLE "vfs"."stores" ADD COLUMN "provider" varchar(255);

UPDATE "vfs"."stores" SET "provider" = substring(uri from '^[^:]*');
UPDATE "vfs"."stores" SET "provider" = 'ftp' WHERE provider IN ('sftp','ftps');

ALTER TABLE "vfs"."stores" ALTER COLUMN "provider" SET NOT NULL;
