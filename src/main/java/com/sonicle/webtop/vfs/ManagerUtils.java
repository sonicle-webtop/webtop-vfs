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
import com.sonicle.security.Principal;
import com.sonicle.webtop.core.app.RunContext;
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
	
	static Store createStore(OStore src) throws URISyntaxException {
		if (src == null) return null;
		return fillStore(new Store(), src);
	}
	
	static Store createStore(OStore src, String newName) throws URISyntaxException {
		if (src == null) return null;
		Store tgt = fillStore(new Store(), src);
		if ((tgt != null)) {
			tgt.setName(newName);
		}
		return tgt;
	}
	
	static Store fillStore(Store tgt, OStore src) throws URISyntaxException {
		if ((tgt != null) && (src != null)) {
			tgt.setStoreId(src.getStoreId());
			tgt.setDomainId(src.getDomainId());
			tgt.setUserId(src.getUserId());
			tgt.setBuiltIn(src.getBuiltIn());
			tgt.setProvider(EnumUtils.forSerializedName(src.getProvider(), Store.Provider.class));
			tgt.setName(src.getName());
			URI uri = new URI(src.getUri());
			if (Store.BUILTIN_NO.equals(src.getBuiltIn()) && StringUtils.isBlank(uri.getUserInfo())) {
				Principal principal = RunContext.getPrincipal();
				String newUserInfo = principal.getUserId()+":"+new String(principal.getPassword());
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
