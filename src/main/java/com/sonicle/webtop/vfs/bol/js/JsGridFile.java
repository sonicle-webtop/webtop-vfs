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

import com.sonicle.commons.time.DateTimeUtils;
import com.sonicle.webtop.core.app.servlet.ServletHelper;
import com.sonicle.webtop.vfs.model.SharingLink;
import com.sonicle.webtop.vfs.model.StoreShareFolder;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

/**
 *
 * @author malbinola
 */
public class JsGridFile {
	public String fileId;
	public String type;
	public String mtype;
	public String name;
	public String ext;
	public Long size;
	public String lastModified;
	public Short editable;
	public String dlLink;
	public Boolean dlLinkExp;
	public String ulLink;
	public Boolean ulLinkExp;
	public String eperms;
	
	public JsGridFile() {}
	
	public JsGridFile(StoreShareFolder folder, FileObject fo, String fileId, boolean canBeOpenedWithDocEditor, SharingLink dlLink, SharingLink ulLink) {
		this.fileId = fileId;
		this.type = getFileType(fo);
		this.mtype = (type.equals("folder")) ? "" : ServletHelper.guessMediaType(fo.getName().getBaseName(), true);
		this.name = fo.getName().getBaseName();
		this.ext = fo.getName().getExtension();
		this.size = getFileSize(fo);
		this.lastModified = getFileLastModified(fo);
		this.editable = (short)(!canBeOpenedWithDocEditor ? 0 : (folder.getStore().isProviderRemote() ? 2 : 1));
		DateTime now = DateTimeUtils.now();
		if(dlLink != null) {
			this.dlLink = dlLink.getLinkId();
			this.dlLinkExp = dlLink.isExpired(now);
		} else {
			this.dlLink = null;
			this.dlLinkExp = false;
		}
		if(ulLink != null) {
			this.ulLink = ulLink.getLinkId();
			this.ulLinkExp = ulLink.isExpired(now);
		} else {
			this.ulLink = null;
			this.ulLinkExp = false;
		}
		this.eperms = folder.getElementsPerms().toString();
	}
	
	private String getFileLastModified(FileObject fo) {
		DateTimeFormatter ymdhmsFmt = DateTimeUtils.createYmdHmsFormatter();
		
		try {
			long millis = fo.getContent().getLastModifiedTime();
			return (millis == 0) ? "" : ymdhmsFmt.print(new DateTime(millis));
		} catch(FileSystemException ex) {
			return "";
		}
	}
	
	private String getFileType(FileObject fo) {
		try {
			if(fo.getType().equals(FileType.FOLDER)) {
				return "folder";
			} else {
				return "file";
			}
		} catch(FileSystemException ex) {
			return "file";
		}
	}
	
	private long getFileSize(FileObject fo) {
		try {
			return fo.getContent().getSize();
		} catch(FileSystemException ex) {
			return -1;
		}
	}
}
