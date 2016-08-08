/*
 * webtop-vfs is a WebTop Service developed by Sonicle S.r.l.
 * Copyright (C) 2014 Sonicle S.r.l.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation with the addition of the following permission
 * added to Section 15 as permitted in Section 7(a): FOR ANY PART OF THE COVERED
 * WORK IN WHICH THE COPYRIGHT IS OWNED BY SONICLE, SONICLE DISCLAIMS THE
 * WARRANTY OF NON INFRINGEMENT OF THIRD PARTY RIGHTS.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301 USA.
 *
 * You can contact Sonicle S.r.l. at email address sonicle@sonicle.com
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License version 3.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License
 * version 3, these Appropriate Legal Notices must retain the display of the
 * "Powered by Sonicle WebTop" logo. If the display of the logo is not reasonably
 * feasible for technical reasons, the Appropriate Legal Notices must display
 * the words "Powered by Sonicle WebTop".
 */
package com.sonicle.webtop.vfs.bol.js;

import com.sonicle.commons.PathUtils;
import com.sonicle.commons.time.DateTimeUtils;
import com.sonicle.webtop.vfs.Service;
import com.sonicle.webtop.vfs.Service.StoreNodeId;
import com.sonicle.webtop.vfs.bol.model.DownloadLink;
import com.sonicle.webtop.vfs.bol.model.Store;
import com.sonicle.webtop.vfs.bol.model.StoreShareFolder;
import com.sonicle.webtop.vfs.bol.model.UploadLink;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTimeZone;

/**
 *
 * @author malbinola
 */
public class JsGridSharingLink {
	public String linkId;
	public String userId;
	public String userDescription;
	public String linkType;
	public Integer storeId;
	public String storeIcon;
	public String storeName;
	public String fileName;
	public String fileExt;
	public String filePath;
	public String fileHash;
	public String fileId;
	public String parentFileId;
	public String parentFilePath;
	public String expireOn;
	public Boolean expired;
	public String authMode;
	
	public JsGridSharingLink() {}
	
	public JsGridSharingLink(DownloadLink dl, String userDescription, String storeName, String storeIcon, StoreNodeId baseFileId, DateTimeZone profileTz) {
		this.linkId = dl.getLinkId();
		this.userId = dl.getUserId();
		this.userDescription = userDescription;
		this.linkType = "D";
		this.storeId = dl.getStoreId();
		this.storeName = storeName;
		this.storeIcon = storeIcon;
		this.filePath = dl.getFilePath();
		this.fileName = PathUtils.getFileName(dl.getFilePath());
		this.fileExt = PathUtils.getFileExtension(dl.getFilePath());
		this.fileHash = dl.getFileHash();
		baseFileId.setPath(filePath);
		this.fileId = baseFileId.toString(true);
		this.parentFilePath = PathUtils.getFullParentPath(dl.getFilePath());
		baseFileId.setPath(parentFilePath);
		this.parentFileId = baseFileId.toString(true);
		this.expireOn = DateTimeUtils.printYmdHmsWithZone(dl.getExpiresOn(), profileTz);
		this.expired = dl.isExpired(DateTimeUtils.now());
		this.authMode = dl.getAuthMode();
	}
	
	public JsGridSharingLink(UploadLink ul, String userDescription, String storeName, String storeIcon, StoreNodeId baseFileId, DateTimeZone profileTz) {
		this.linkId = ul.getLinkId();
		this.userId = ul.getUserId();
		this.userDescription = userDescription;
		this.linkType = "U";
		this.storeId = ul.getStoreId();
		this.storeName = storeName;
		this.storeIcon = storeIcon;
		this.filePath = ul.getFilePath();
		this.fileName = PathUtils.getFileName(ul.getFilePath());
		this.fileExt = PathUtils.getFileExtension(ul.getFilePath());
		this.fileHash = ul.getFileHash();
		baseFileId.setPath(filePath);
		this.fileId = baseFileId.toString(true);
		this.parentFilePath = PathUtils.getFullParentPath(ul.getFilePath());
		baseFileId.setPath(parentFilePath);
		this.parentFileId = baseFileId.toString(true);
		this.expireOn = DateTimeUtils.printYmdHmsWithZone(ul.getExpiresOn(), profileTz);
		this.expired = ul.isExpired(DateTimeUtils.now());
		this.authMode = ul.getAuthMode();
	}
}
