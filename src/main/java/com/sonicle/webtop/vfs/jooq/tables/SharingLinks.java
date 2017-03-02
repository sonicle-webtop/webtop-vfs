/**
 * This class is generated by jOOQ
 */
package com.sonicle.webtop.vfs.jooq.tables;

/**
 * This class is generated by jOOQ.
 */
@javax.annotation.Generated(
	value = {
		"http://www.jooq.org",
		"jOOQ version:3.5.3"
	},
	comments = "This class is generated by jOOQ"
)
@java.lang.SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class SharingLinks extends org.jooq.impl.TableImpl<com.sonicle.webtop.vfs.jooq.tables.records.SharingLinksRecord> {

	private static final long serialVersionUID = 177176232;

	/**
	 * The reference instance of <code>vfs.sharing_links</code>
	 */
	public static final com.sonicle.webtop.vfs.jooq.tables.SharingLinks SHARING_LINKS = new com.sonicle.webtop.vfs.jooq.tables.SharingLinks();

	/**
	 * The class holding records for this type
	 */
	@Override
	public java.lang.Class<com.sonicle.webtop.vfs.jooq.tables.records.SharingLinksRecord> getRecordType() {
		return com.sonicle.webtop.vfs.jooq.tables.records.SharingLinksRecord.class;
	}

	/**
	 * The column <code>vfs.sharing_links.sharing_link_id</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.vfs.jooq.tables.records.SharingLinksRecord, java.lang.String> SHARING_LINK_ID = createField("sharing_link_id", org.jooq.impl.SQLDataType.VARCHAR.length(255).nullable(false), this, "");

	/**
	 * The column <code>vfs.sharing_links.domain_id</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.vfs.jooq.tables.records.SharingLinksRecord, java.lang.String> DOMAIN_ID = createField("domain_id", org.jooq.impl.SQLDataType.VARCHAR.length(20).nullable(false), this, "");

	/**
	 * The column <code>vfs.sharing_links.user_id</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.vfs.jooq.tables.records.SharingLinksRecord, java.lang.String> USER_ID = createField("user_id", org.jooq.impl.SQLDataType.VARCHAR.length(100).nullable(false), this, "");

	/**
	 * The column <code>vfs.sharing_links.link_type</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.vfs.jooq.tables.records.SharingLinksRecord, java.lang.String> LINK_TYPE = createField("link_type", org.jooq.impl.SQLDataType.VARCHAR.length(1).nullable(false), this, "");

	/**
	 * The column <code>vfs.sharing_links.store_id</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.vfs.jooq.tables.records.SharingLinksRecord, java.lang.Integer> STORE_ID = createField("store_id", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

	/**
	 * The column <code>vfs.sharing_links.file_path</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.vfs.jooq.tables.records.SharingLinksRecord, java.lang.String> FILE_PATH = createField("file_path", org.jooq.impl.SQLDataType.CLOB.nullable(false), this, "");

	/**
	 * The column <code>vfs.sharing_links.file_hash</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.vfs.jooq.tables.records.SharingLinksRecord, java.lang.String> FILE_HASH = createField("file_hash", org.jooq.impl.SQLDataType.VARCHAR.length(255).nullable(false), this, "");

	/**
	 * The column <code>vfs.sharing_links.created_on</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.vfs.jooq.tables.records.SharingLinksRecord, org.joda.time.DateTime> CREATED_ON = createField("created_on", org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false), this, "", new com.sonicle.webtop.core.jooq.DateTimeConverter());

	/**
	 * The column <code>vfs.sharing_links.expires_on</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.vfs.jooq.tables.records.SharingLinksRecord, org.joda.time.DateTime> EXPIRES_ON = createField("expires_on", org.jooq.impl.SQLDataType.TIMESTAMP, this, "", new com.sonicle.webtop.core.jooq.DateTimeConverter());

	/**
	 * The column <code>vfs.sharing_links.auth_mode</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.vfs.jooq.tables.records.SharingLinksRecord, java.lang.String> AUTH_MODE = createField("auth_mode", org.jooq.impl.SQLDataType.VARCHAR.length(1).nullable(false), this, "");

	/**
	 * The column <code>vfs.sharing_links.password</code>.
	 */
	public final org.jooq.TableField<com.sonicle.webtop.vfs.jooq.tables.records.SharingLinksRecord, java.lang.String> PASSWORD = createField("password", org.jooq.impl.SQLDataType.VARCHAR.length(128), this, "");

	/**
	 * Create a <code>vfs.sharing_links</code> table reference
	 */
	public SharingLinks() {
		this("sharing_links", null);
	}

	/**
	 * Create an aliased <code>vfs.sharing_links</code> table reference
	 */
	public SharingLinks(java.lang.String alias) {
		this(alias, com.sonicle.webtop.vfs.jooq.tables.SharingLinks.SHARING_LINKS);
	}

	private SharingLinks(java.lang.String alias, org.jooq.Table<com.sonicle.webtop.vfs.jooq.tables.records.SharingLinksRecord> aliased) {
		this(alias, aliased, null);
	}

	private SharingLinks(java.lang.String alias, org.jooq.Table<com.sonicle.webtop.vfs.jooq.tables.records.SharingLinksRecord> aliased, org.jooq.Field<?>[] parameters) {
		super(alias, com.sonicle.webtop.vfs.jooq.Vfs.VFS, aliased, parameters, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public org.jooq.UniqueKey<com.sonicle.webtop.vfs.jooq.tables.records.SharingLinksRecord> getPrimaryKey() {
		return com.sonicle.webtop.vfs.jooq.Keys.SHARING_LINKS_PKEY1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public java.util.List<org.jooq.UniqueKey<com.sonicle.webtop.vfs.jooq.tables.records.SharingLinksRecord>> getKeys() {
		return java.util.Arrays.<org.jooq.UniqueKey<com.sonicle.webtop.vfs.jooq.tables.records.SharingLinksRecord>>asList(com.sonicle.webtop.vfs.jooq.Keys.SHARING_LINKS_PKEY1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public com.sonicle.webtop.vfs.jooq.tables.SharingLinks as(java.lang.String alias) {
		return new com.sonicle.webtop.vfs.jooq.tables.SharingLinks(alias, this);
	}

	/**
	 * Rename this table
	 */
	public com.sonicle.webtop.vfs.jooq.tables.SharingLinks rename(java.lang.String name) {
		return new com.sonicle.webtop.vfs.jooq.tables.SharingLinks(name, null);
	}
}
