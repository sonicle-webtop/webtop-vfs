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

import com.sonicle.commons.LangUtils;
import com.sonicle.commons.time.DateTimeUtils;
import com.sonicle.commons.web.DispositionType;
import com.sonicle.commons.web.ServletUtils;
import com.sonicle.commons.web.json.JsonResult;
import com.sonicle.webtop.core.app.RunContext;
import com.sonicle.webtop.core.app.WT;
import com.sonicle.webtop.core.app.WebTopSession;
import com.sonicle.webtop.core.bol.js.JsWTSPublic;
import com.sonicle.webtop.core.sdk.BasePublicService;
import com.sonicle.webtop.core.sdk.WTException;
import com.sonicle.webtop.core.servlet.ServletHelper;
import com.sonicle.webtop.vfs.bol.model.SharingLink;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.joda.time.DateTime;
import org.slf4j.Logger;

/**
 *
 * @author malbinola
 */
public class PublicService extends BasePublicService {
	private static final Logger logger = WT.getLogger(PublicService.class);
	private static final String WTSPROP_AUTHED_LINKS = "AUTHEDLINKS";
	
	private final Object lock1 = new Object();
	private VfsManager manager;
	
	@Override
	public void initialize() throws Exception {
		manager = new VfsManager(true);
	}

	@Override
	public void cleanup() throws Exception {
		manager = null;
	}
	
	private HashSet<String> getAuthedLinks() {
		WebTopSession wts = RunContext.getWebTopSession();
		synchronized(lock1) {
			if(!wts.hasProperty(SERVICE_ID, WTSPROP_AUTHED_LINKS)) {
				return (HashSet<String>)wts.setProperty(SERVICE_ID, WTSPROP_AUTHED_LINKS, new HashSet<String>());
			} else {
				return (HashSet<String>)wts.getProperty(SERVICE_ID, WTSPROP_AUTHED_LINKS);
			}
		}
	}
	
	private void addServiceVar(JsWTSPublic jswts, String key, Object value) {
		jswts.servicesVars.get(1).put(key, value);
	}
	
	@Override
	public void processDefaultAction(HttpServletRequest request, HttpServletResponse response) throws Exception {
		PublicPath path = parsePathInfo(request.getPathInfo());
		WebTopSession wts = RunContext.getWebTopSession();
		
		if(path.context.equals("file")) {
			Integer raw = ServletUtils.getIntParameter(request, "raw", 0);
			
			SharingLink link = manager.getSharingLink(path.relative);
			
			if(link == null) {
				//TODO: pagina errore link rimosso...
				throw new WTException("Link not found [{0}]", path.relative);
				
			} else if(link.isExpired(DateTimeUtils.now())) {
				//TODO: pagina errore link scaduto...
				throw new WTException("Link not expired [{0}]", path.relative);
				
			} else if(isLinkAuthorized(link) && raw == 1) {
				processDownloadFile(request, response, link);
				
			} else {
				Map vars = new HashMap();
				
				// Startup variables
				JsWTSPublic jswts = new JsWTSPublic();
				wts.fillStartup(jswts, SERVICE_ID);
				addServiceVar(jswts, "linkId", path.relative);
				vars.put("WTS", LangUtils.unescapeUnicodeBackslashes(jswts.toJson()));
				writePage(ServletUtils.getBaseURL(request), vars, wts.getLocale(), response);
				ServletUtils.setCacheControlPrivate(response);
				ServletUtils.setHtmlContentType(response);
			}
			
		} else {
			throw new WTException("Invalid context [{0}]", path.context);
		}
	}
	
	public void processAuthorizeLink(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		
		try {
			String linkId = ServletUtils.getStringParameter(request, "linkId", true);
			String password = ServletUtils.getStringParameter(request, "password", true);
			
			SharingLink link = manager.getSharingLink(linkId);
			if(link == null) throw new WTException("Link not found [{0}]", linkId);
			
			boolean check = true;
			if(link.getAuthMode().equals(SharingLink.AUTH_MODE_PASSWORD)) {
				check = StringUtils.equals(password, link.getPassword());
			}
			
			if(check) {
				getAuthedLinks().add(linkId);
				new JsonResult().printTo(out);
			} else {
				new JsonResult(false, null).printTo(out);
			}
			
		} catch(Exception ex) {
			logger.error("Error in action CheckLinkPassword", ex);
			new JsonResult(false, "Error").printTo(out);
		}
	}
	
	private boolean isLinkAuthorized(SharingLink link) {
		if(link.getAuthMode().equals(SharingLink.AUTH_MODE_PASSWORD)) {
			return getAuthedLinks().contains(link.getLinkId());
		} else {
			return true;
		}
	}
	
	private boolean authCheck(String linkId) {
		return getAuthedLinks().contains(linkId);
	}
	
	private void processDownloadFile(HttpServletRequest request, HttpServletResponse response, SharingLink link) {
		
		try {
			if(link.isExpired(DateTimeUtils.now())) throw new WTException("expired");
			if(link.getType().equals(SharingLink.TYPE_UPLOAD)) throw new WTException("upload");
			if(link.getAuthMode().equals(SharingLink.AUTH_MODE_PASSWORD)) {
				if(!authCheck(link.getLinkId())) throw new WTException("not authchecked");
			}
			
			FileObject fo = null;
			try {
				fo = manager.getStoreFile(link.getStoreId(), link.getFilePath());
				if(!fo.isFile()) throw new WTException("Requested file is not a real file");
				
				String filename = fo.getName().getBaseName();
				String mediaType = ServletHelper.guessMediaType(filename, true);
				IOUtils.copy(fo.getContent().getInputStream(), response.getOutputStream());
				ServletUtils.setFileStreamHeaders(response, mediaType, DispositionType.ATTACHMENT, filename);
				ServletUtils.setContentLengthHeader(response, fo.getContent().getSize());
			} finally {
				IOUtils.closeQuietly(fo);
			}
			
		} catch(Exception ex) {
			logger.error("Error in action DownloadFile", ex);
			ServletUtils.writeErrorHandlingJs(response, ex.getMessage());
		}
	}
}
