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
package com.sonicle.webtop.vfs.sfs;

import com.sonicle.commons.LangUtils;
import com.sonicle.vfs2.provider.googledrive.GDriveFileSystemConfigBuilder;
import com.sonicle.vfs2.util.GoogleDriveApiUtils;
import com.sonicle.vfs2.util.GoogleDriveAppInfo;
import com.sonicle.webtop.core.app.WT;
import com.sonicle.webtop.core.sdk.WTException;
import com.sonicle.webtop.vfs.bol.model.ParamsGoogleDrive;
import com.sonicle.webtop.vfs.bol.model.Store;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.jooq.tools.StringUtils;

/**
 *
 * @author malbinola
 */
public class GoogleDriveSFS extends StoreFileSystem {
	private final GoogleDriveAppInfo appInfo;
	
	public GoogleDriveSFS(int storeId, URI uri, String parameters, String clientId, String clientSecret) {
		super(storeId, uri, parameters);
		this.appInfo = GoogleDriveApiUtils.createAppInfo(WT.getPlatformName(), clientId, clientSecret);
	}

	@Override
	protected void configureOptions() throws FileSystemException {
		GDriveFileSystemConfigBuilder builder = GDriveFileSystemConfigBuilder.getInstance();
		builder.setApplicationName(fso, appInfo.applicationName);
	}

	@Override
	public FileObject getRootFileObject() throws FileSystemException {
		synchronized(lock) {
			if(rootFo == null) {
				configureOptions();
				refreshTokenIfNecessary();
				rootFo = resolveRoot();
			} else {
				refreshTokenIfNecessary();
			}
			return rootFo;
		}
	}
	
	private void refreshTokenIfNecessary() throws FileSystemException {
		try {
			ParamsGoogleDrive params = readParams();
			String newAccessToken = GoogleDriveApiUtils.refreshTokenIfNecessary(params.accessToken, params.refreshToken, appInfo);
			if (!StringUtils.isBlank(newAccessToken)) {
				uri = Store.buildGoogleDriveURI(params.accountEmail, newAccessToken);
				params.accessToken = newAccessToken;
				writeParams(params);
				int ret = updateStore();
				if (ret != 1) throw new WTException("Unable to update store");
			}
			
		} catch(IOException | URISyntaxException | WTException ex) {
			throw new FileSystemException(ex);
		}
	}
	
	private ParamsGoogleDrive readParams() {
		return LangUtils.deserialize(this.parameters, ParamsGoogleDrive.class);
	}
	
	private void writeParams(ParamsGoogleDrive params) {
		this.parameters = LangUtils.serialize(params, ParamsGoogleDrive.class);
	}
}
