/**
 * This class is generated by jOOQ
 */
package com.sonicle.webtop.vfs.jooq.tables.pojos;

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
public class SharingLinks implements java.io.Serializable {

	private static final long serialVersionUID = 558394116;

	private java.lang.String       sharingLinkId;
	private java.lang.String       domainId;
	private java.lang.String       userId;
	private java.lang.String       linkType;
	private java.lang.Integer      storeId;
	private java.lang.String       filePath;
	private java.lang.String       fileHash;
	private org.joda.time.DateTime createdOn;
	private org.joda.time.DateTime expiresOn;
	private java.lang.String       authMode;
	private java.lang.String       password;

	public SharingLinks() {}

	public SharingLinks(
		java.lang.String       sharingLinkId,
		java.lang.String       domainId,
		java.lang.String       userId,
		java.lang.String       linkType,
		java.lang.Integer      storeId,
		java.lang.String       filePath,
		java.lang.String       fileHash,
		org.joda.time.DateTime createdOn,
		org.joda.time.DateTime expiresOn,
		java.lang.String       authMode,
		java.lang.String       password
	) {
		this.sharingLinkId = sharingLinkId;
		this.domainId = domainId;
		this.userId = userId;
		this.linkType = linkType;
		this.storeId = storeId;
		this.filePath = filePath;
		this.fileHash = fileHash;
		this.createdOn = createdOn;
		this.expiresOn = expiresOn;
		this.authMode = authMode;
		this.password = password;
	}

	public java.lang.String getSharingLinkId() {
		return this.sharingLinkId;
	}

	public void setSharingLinkId(java.lang.String sharingLinkId) {
		this.sharingLinkId = sharingLinkId;
	}

	public java.lang.String getDomainId() {
		return this.domainId;
	}

	public void setDomainId(java.lang.String domainId) {
		this.domainId = domainId;
	}

	public java.lang.String getUserId() {
		return this.userId;
	}

	public void setUserId(java.lang.String userId) {
		this.userId = userId;
	}

	public java.lang.String getLinkType() {
		return this.linkType;
	}

	public void setLinkType(java.lang.String linkType) {
		this.linkType = linkType;
	}

	public java.lang.Integer getStoreId() {
		return this.storeId;
	}

	public void setStoreId(java.lang.Integer storeId) {
		this.storeId = storeId;
	}

	public java.lang.String getFilePath() {
		return this.filePath;
	}

	public void setFilePath(java.lang.String filePath) {
		this.filePath = filePath;
	}

	public java.lang.String getFileHash() {
		return this.fileHash;
	}

	public void setFileHash(java.lang.String fileHash) {
		this.fileHash = fileHash;
	}

	public org.joda.time.DateTime getCreatedOn() {
		return this.createdOn;
	}

	public void setCreatedOn(org.joda.time.DateTime createdOn) {
		this.createdOn = createdOn;
	}

	public org.joda.time.DateTime getExpiresOn() {
		return this.expiresOn;
	}

	public void setExpiresOn(org.joda.time.DateTime expiresOn) {
		this.expiresOn = expiresOn;
	}

	public java.lang.String getAuthMode() {
		return this.authMode;
	}

	public void setAuthMode(java.lang.String authMode) {
		this.authMode = authMode;
	}

	public java.lang.String getPassword() {
		return this.password;
	}

	public void setPassword(java.lang.String password) {
		this.password = password;
	}
}