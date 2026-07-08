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

import com.sonicle.commons.EnumUtils;
import com.sonicle.commons.URIUtils;
import com.sonicle.security.PasswordUtils;
import com.sonicle.webtop.core.app.WT;
import com.sonicle.webtop.core.app.WebTopManager;
import com.sonicle.webtop.core.sdk.UserProfileId;
import com.sonicle.webtop.vfs.bol.OStore;
import com.sonicle.webtop.vfs.model.Store;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author malbinola
 */
public class ManagerUtils {
	
	static Store createStore(OStore src, UserProfileId credentialsProfileId) throws URISyntaxException {
		if (src == null) return null;
		return fillStore(new Store(), src, credentialsProfileId);
	}

	static Store createStore(OStore src, String newName, UserProfileId credentialsProfileId) throws URISyntaxException {
		if (src == null) return null;
		Store tgt = fillStore(new Store(), src, credentialsProfileId);
		if ((tgt != null)) {
			tgt.setName(newName);
		}
		return tgt;
	}

	/**
	 * Fills a Store model from its record. For non-builtin stores whose URI has
	 * no userinfo, WebTop credentials are injected: those of the passed profile
	 * — the MANAGER'S TARGET user, never the running caller. The resulting URI
	 * ends up cached in the manager's StoreFileSystem map, so on a shared
	 * per-user instance a caller-derived value would bake the first toucher's
	 * password (possibly an admin's or a public-context null) into every other
	 * caller's filesystem access.
	 */
	static Store fillStore(Store tgt, OStore src, UserProfileId credentialsProfileId) throws URISyntaxException {
		if ((tgt != null) && (src != null)) {
			tgt.setStoreId(src.getStoreId());
			tgt.setDomainId(src.getDomainId());
			tgt.setUserId(src.getUserId());
			tgt.setBuiltIn(src.getBuiltIn());
			tgt.setProvider(EnumUtils.forSerializedName(src.getProvider(), Store.Provider.class));
			tgt.setName(src.getName());
			URI uri = new URI(src.getUri());
			if (Store.BUILTIN_NO.equals(src.getBuiltIn()) && StringUtils.isBlank(uri.getUserInfo()) && credentialsProfileId != null) {
				String newUserInfo = URIUtils.asUserInfo(credentialsProfileId.getUserId(), PasswordUtils.asString(WT.lookupSecretStoreValue(credentialsProfileId, WebTopManager.PSVKEY_PPW)));
				uri = new URI(uri.getScheme(), newUserInfo, uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
			}
			tgt.setUri(uri);
			tgt.setParameters(src.getParameters());
		}
		return tgt;
	}
	
	static OStore createOStore(Store src) {
		if (src == null) return null;
		return fillOStore(new OStore(), src);
	}
	
	static OStore fillOStore(OStore tgt, Store src) {
		if ((tgt != null) && (src != null)) {
			tgt.setStoreId(src.getStoreId());
			tgt.setDomainId(src.getDomainId());
			tgt.setUserId(src.getUserId());
			tgt.setBuiltIn(src.getBuiltIn());
			tgt.setProvider(EnumUtils.toSerializedName(src.getProvider()));
			tgt.setName(src.getName());
			tgt.setUri(src.getUri().toString());
			tgt.setParameters(src.getParameters());
		}
		return tgt;
	}
}
