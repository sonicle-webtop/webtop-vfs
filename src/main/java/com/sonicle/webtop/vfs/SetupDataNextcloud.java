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

import com.sonicle.vfs2.VfsURI;
import com.sonicle.webtop.core.app.RunContext;
import com.sonicle.webtop.vfs.model.Store;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author malbinola
 */
public class SetupDataNextcloud extends SetupData {
	public String scheme = null;
	public String host = null;
	public Integer port = null;
	public String username = null;
	public String password = null;
	public String path = null;
	
	public SetupDataNextcloud() {
		provider = Store.Provider.NEXTCLOUD;
	}

	public static URI buildNextcloudURI(String scheme, String host, Integer port, String username, String password, String path) throws URISyntaxException {
		return new VfsURI.Builder()
			.scheme(scheme)
			.host(host)
			.port(port)
			.username(username)
			.password(password)
			.path(path)
			.build();
	}
	
	@Override
	public URI generateURI() throws URISyntaxException {
		return buildNextcloudURI(scheme, host, port, username, password, path);
	}
	
	@Override
	public String generateParameters() {
		return null;
	}
	
	@Override
	public void updateName() {
		name = MessageFormat.format("{0} ({1})", host, StringUtils.isBlank(username)?RunContext.getPrincipal().getUserId():username);
	}
}
