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
import com.sonicle.webtop.vfs.bol.model.SharingLink;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;

/**
 *
 * @author malbinola
 */
public class JsSharingLink {
	public String linkId;
	public String type; // Read-only
	public String filePath; // Read-only
	public String fileName; // Read-only
	public String expirationDate;
	public String authMode;
	public String password;
	
	
	public JsSharingLink() {}
	
	public JsSharingLink(SharingLink o, DateTimeZone profileTz) {
		this.linkId = o.getLinkId();
		this.type = o.getType();
		this.filePath = o.getFilePath();
		this.fileName = PathUtils.getFileName(o.getFilePath());
		this.expirationDate = DateTimeUtils.printYmdHmsWithZone(o.getExpiresOn(), profileTz);
		this.authMode = o.getAuthMode();
		this.password = o.getPassword();
	}
	
	public static SharingLink createSharingLink(JsSharingLink js, DateTimeZone profileTz) {
		DateTimeFormatter ymdHmsFmt = DateTimeUtils.createYmdHmsFormatter(profileTz);
		
		SharingLink bean = new SharingLink();
		bean.setLinkId(js.linkId);
		bean.setType(js.type);
		if(!StringUtils.isBlank(js.expirationDate)) {
			DateTime dt = ymdHmsFmt.parseDateTime(js.expirationDate);
			bean.setExpiresOn(DateTimeUtils.withTimeAtEndOfDay(dt));
		}
		bean.setAuthMode(js.authMode);
		bean.setPassword(js.password);
		return bean;
	}
}
