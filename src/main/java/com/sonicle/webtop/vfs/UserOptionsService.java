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
package com.sonicle.webtop.vfs;

import com.sonicle.commons.db.DbUtils;
import com.sonicle.commons.web.Crud;
import com.sonicle.commons.web.ServletUtils;
import com.sonicle.commons.web.json.JsonResult;
import com.sonicle.commons.web.json.MapItem;
import com.sonicle.commons.web.json.Payload;
import com.sonicle.webtop.core.app.WT;
import com.sonicle.webtop.core.sdk.BaseUserOptionsService;
import com.sonicle.webtop.vfs.bol.js.JsUserOptions;
import java.io.PrintWriter;
import java.sql.Connection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;

/**
 *
 * @author malbinola
 */
public class UserOptionsService extends BaseUserOptionsService {
	public static final Logger logger = WT.getLogger(UserOptionsService.class);
	
	@Override
	public void processUserOptions(HttpServletRequest request, HttpServletResponse response, PrintWriter out, String payload) {
		Connection con = null;
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			VfsUserSettings us = new VfsUserSettings(SERVICE_ID, getTargetProfileId());
			
			if(crud.equals(Crud.READ)) {
				JsUserOptions jso = new JsUserOptions(getTargetProfileId().toString());
				
				// Main
				jso.privateUploadMaxFileSize = us.getPrivateUploadMaxFileSize();
				jso.publicUploadMaxFileSize = us.getPublicUploadMaxFileSize();
				jso.showHiddenFiles = us.getShowHiddenFiles();
				
				new JsonResult(jso).printTo(out);
				
			} else if(crud.equals(Crud.UPDATE)) {
				Payload<MapItem, JsUserOptions> pl = ServletUtils.getPayload(payload, JsUserOptions.class);
				
				// Main
				if(pl.map.has("privateUploadMaxFileSize")) us.setPrivateUploadMaxFileSize(pl.data.privateUploadMaxFileSize);
				if(pl.map.has("publicUploadMaxFileSize")) us.setPublicUploadMaxFileSize(pl.data.publicUploadMaxFileSize);
				if(pl.map.has("showHiddenFiles")) us.setShowHiddenFiles(pl.data.showHiddenFiles);
				
				new JsonResult().printTo(out);
			}
			
		} catch (Exception ex) {
			logger.error("Error executing UserOptions", ex);
			new JsonResult(false).printTo(out);
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
}
