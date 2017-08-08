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
package com.sonicle.webtop.vfs;

import com.sonicle.commons.PathUtils;
import com.sonicle.commons.web.json.MapItem;
import com.sonicle.webtop.core.app.WT;
import com.sonicle.webtop.core.util.NotificationHelper;
import com.sonicle.webtop.vfs.model.SharingLink;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author malbinola
 */
public class TplHelper {
	private static final String SERVICE_ID = "com.sonicle.webtop.vfs";
	
	public static String buildLinkUsageEmailSubject(Locale locale, String title) {
		return NotificationHelper.buildSubject(locale, SERVICE_ID, title);
	}
	
	public static String buildLinkUsageBodyTpl(Locale locale, String linkId, String linkName, String filePath, String remoteIp, String userAgent) throws IOException, TemplateException {
		MapItem i18n = new MapItem();
		i18n.put("sharedLink", WT.lookupResource(SERVICE_ID, locale, VfsLocale.TPL_EMAIL_SHARINGLINKUSAGE_BODY_SHAREDLINK));
		i18n.put("file", WT.lookupResource(SERVICE_ID, locale, VfsLocale.TPL_EMAIL_SHARINGLINKUSAGE_BODY_FILE));
		i18n.put("remoteUser", WT.lookupResource(SERVICE_ID, locale, VfsLocale.TPL_EMAIL_SHARINGLINKUSAGE_BODY_REMOTEUSER));
		
		MapItem map = new MapItem();
		map.put("i18n", i18n);
		map.put("linkId", linkId);
		map.put("linkName", linkName);
		map.put("filePath", filePath);
		map.put("fileName", PathUtils.getFileName(filePath));
		map.put("remoteIp", remoteIp);
		map.put("userAgent", userAgent);
		
		return WT.buildTemplate(SERVICE_ID, "tpl/email/linkUsage-body.html", map);
	}
	
	public static String buildLinkEmbedCodeTpl(Locale locale, SharingLink.LinkType linkType, String linkUrl, String linkName, String linkExpiration, String password) throws IOException, TemplateException {
		MapItem i18n = new MapItem();
		if (linkType.equals(SharingLink.LinkType.DOWNLOAD)) {
			i18n.put("type", WT.lookupResource(SERVICE_ID, locale, VfsLocale.TPL_EMAIL_LINKEMBEDCODE_TYPE_DL));
		} else if (linkType.equals(SharingLink.LinkType.UPLOAD)) {
			i18n.put("type", WT.lookupResource(SERVICE_ID, locale, VfsLocale.TPL_EMAIL_LINKEMBEDCODE_TYPE_UL));
		}
		i18n.put("expiration", WT.lookupResource(SERVICE_ID, locale, VfsLocale.TPL_EMAIL_LINKEMBEDCODE_EXPIRATION));
		i18n.put("password", WT.lookupResource(SERVICE_ID, locale, VfsLocale.TPL_EMAIL_LINKEMBEDCODE_PASSWORD));
		
		MapItem map = new MapItem();
		map.put("i18n", i18n);
		map.put("linkUrl", linkUrl);
		map.put("linkName", linkName);
		if (!StringUtils.isBlank(linkExpiration)) {
			map.put("linkExpiration", linkExpiration);
		}
		if (!StringUtils.isBlank(password)) {
			map.put("linkPassword", password);
		}
		
		return WT.buildTemplate(SERVICE_ID, "tpl/email/linkEmbed.html", map);
	}
}
