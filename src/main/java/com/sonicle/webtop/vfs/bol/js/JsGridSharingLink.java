/* 
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
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301 USA.
 *
 * You can contact Sonicle S.r.l. at email address sonicle[at]sonicle[dot]com
 *
 * The interactive user interfaces in modified source and object code versions
 * of this program must display Appropriate Legal Notices, as required under
 * Section 5 of the GNU Affero General Public License version 3.
 *
 * In accordance with Section 7(b) of the GNU Affero General Public License
 * version 3, these Appropriate Legal Notices must retain the display of the
 * Sonicle logo and Sonicle copyright notice. If the display of the logo is not
 * reasonably feasible for technical reasons, the Appropriate Legal Notices must
 * display the words "Copyright (C) 2014 Sonicle S.r.l.".
 */
package com.sonicle.webtop.vfs.bol.js;

import com.sonicle.commons.EnumUtils;
import com.sonicle.commons.PathUtils;
import com.sonicle.commons.time.DateTimeUtils;
import com.sonicle.webtop.vfs.Service.StoreNodeId;
import com.sonicle.webtop.vfs.model.SharingLink;
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
	
	public JsGridSharingLink(SharingLink sl, String userDescription, String storeName, String storeIcon, StoreNodeId baseFileId, DateTimeZone profileTz) {
		this.linkId = sl.getLinkId();
		this.userId = sl.getUserId();
		this.userDescription = userDescription;
		this.linkType = EnumUtils.toSerializedName(sl.getLinkType());
		this.storeId = sl.getStoreId();
		this.storeName = storeName;
		this.storeIcon = storeIcon;
		this.filePath = sl.getFilePath();
		this.fileName = PathUtils.getFileName(sl.getFilePath());
		this.fileExt = PathUtils.getFileExtension(sl.getFilePath());
		this.fileHash = sl.getFileHash();
		baseFileId.setPath(filePath);
		this.fileId = baseFileId.toString(true);
		this.parentFilePath = PathUtils.getFullParentPath(sl.getFilePath());
		baseFileId.setPath(parentFilePath);
		this.parentFileId = baseFileId.toString(true);
		this.expireOn = DateTimeUtils.printYmdHmsWithZone(sl.getExpiresOn(), profileTz);
		this.expired = sl.isExpired(DateTimeUtils.now());
		this.authMode = EnumUtils.toSerializedName(sl.getAuthMode());
	}
}
