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
package com.sonicle.webtop.vfs.rest.v1;

import com.google.api.client.util.IOUtils;
import com.sonicle.webtop.core.app.RunContext;
import com.sonicle.webtop.core.app.WT;
import com.sonicle.webtop.core.app.servlet.ServletHelper;
import com.sonicle.webtop.core.sdk.UserProfileId;
import com.sonicle.webtop.vfs.IVfsManager;
import com.sonicle.webtop.vfs.VfsManager;
import com.sonicle.webtop.vfs.model.Store;
import com.sonicle.webtop.vfs.model.StoreFSFolder;
import com.sonicle.webtop.vfs.model.StoreFSOrigin;
import com.sonicle.webtop.vfs.swagger.v1.api.MeStoresApi;
import com.sonicle.webtop.vfs.swagger.v1.model.ApiApiError;
import com.sonicle.webtop.vfs.swagger.v1.model.ApiFile;
import com.sonicle.webtop.vfs.swagger.v1.model.ApiFileUpload;
import com.sonicle.webtop.vfs.swagger.v1.model.ApiFolder;
import com.sonicle.webtop.vfs.swagger.v1.model.ApiStore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gabriele.bulfon
 */
public class MeStores extends MeStoresApi {
	private static final Logger logger = LoggerFactory.getLogger(MeStores.class);

	@Override
	public Response listStores() {
		UserProfileId targetPid = RunContext.getRunProfileId();
		try {
			VfsManager mmgr = VfsRestApiUtils.getVfsManager(targetPid);
			ArrayList<ApiStore> items = new ArrayList<>();
			Map<Integer, Store> stores = mmgr.listMyStores();
			for(Store s: stores.values()) {
				ApiStore as = new ApiStore();
				as.setId(s.getStoreId());
				as.setName(s.getName());
				as.setProvider(s.getProvider().name());
				as.setIncoming(false);
				items.add(as);
			}
			
			for (StoreFSOrigin origin: mmgr.listIncomingStoreOrigins().values()) {
				for(StoreFSFolder sf: mmgr.listIncomingStoreFolders(origin).values()) {
					Store s = sf.getStore();
					ApiStore as = new ApiStore();
					as.setId(s.getStoreId());
					as.setName(sf.getDisplayName()+"\n("+WT.getProfileData(origin.getProfileId()).getDisplayName()+")");
					as.setProvider(s.getProvider().name());
					as.setIncoming(true);
					items.add(as);
				}
			}
			return respOk(items);
			
		} catch(Exception ex) {
			logger.error("[{}] listStores()", targetPid, ex);
			return respError(ex);
		}
	}

	@Override
	public Response listFolders(ApiFolder apiFolder) {
		UserProfileId targetPid = RunContext.getRunProfileId();
		try {
			VfsManager mmgr = VfsRestApiUtils.getVfsManager(targetPid);
			FileObject folders[] = mmgr.listStoreFiles(IVfsManager.StoreFileType.FOLDER, apiFolder.getStoreId(), apiFolder.getPath());
			ArrayList<ApiFolder> items = new ArrayList<>();
			for(FileObject folder: folders) {
				ApiFolder af = new ApiFolder();
				af.setStoreId(apiFolder.getStoreId());
				String fpath = StringUtils.stripEnd(apiFolder.getPath(), "/");
				af.setPath(fpath + "/" + folder.getName().getBaseName());
				items.add(af);
			}
			return respOk(items);
			
		} catch(Exception ex) {
			logger.error("[{}] listFolders()", targetPid, ex);
			return respError(ex);
		}
	}

	@Override
	public Response listFiles(ApiFolder apiFolder) {
		UserProfileId targetPid = RunContext.getRunProfileId();
		try {
			VfsManager mmgr = VfsRestApiUtils.getVfsManager(targetPid);
			FileObject files[] = mmgr.listStoreFiles(IVfsManager.StoreFileType.FILE, apiFolder.getStoreId(), apiFolder.getPath());
			ArrayList<ApiFile> items = new ArrayList<>();
			for(FileObject file: files) {
				String fileName = file.getName().getBaseName();
				FileContent fileContent = file.getContent();
				ApiFile af = new ApiFile();
				af.setName(fileName);
				af.setContentType(ServletHelper.guessMediaType(fileName, true));
				af.setSize(fileContent.getSize());
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(fileContent.getLastModifiedTime());
				af.setLastModified(
						cal.get(Calendar.YEAR)+"-"+
						StringUtils.leftPad(""+(cal.get(Calendar.MONTH)+1), 2, '0')+"-"+
						StringUtils.leftPad(""+cal.get(Calendar.DAY_OF_MONTH), 2, '0')+" "+
						StringUtils.leftPad(""+cal.get(Calendar.HOUR_OF_DAY), 2, '0')+":"+
						StringUtils.leftPad(""+cal.get(Calendar.MINUTE), 2, '0'));
				
				af.setStoreId(apiFolder.getStoreId());
				String fpath = StringUtils.stripEnd(apiFolder.getPath(), "/");
				af.setPath(fpath+"/"+fileName);
				items.add(af);
			}
			return respOk(items);
			
		} catch(Exception ex) {
			logger.error("[{}] listFiles()", targetPid, ex);
			return respError(ex);
		}
	}

	@Override
	public Response getFileContent(ApiFile apiFile) {
		UserProfileId targetPid = RunContext.getRunProfileId();
		try {
			VfsManager mmgr = VfsRestApiUtils.getVfsManager(targetPid);
			FileObject file = mmgr.getStoreFile(apiFile.getStoreId(), apiFile.getPath());
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream os) throws IOException, WebApplicationException {
					IOUtils.copy(file.getContent().getInputStream(), os);
				}
			};

			return respOk(stream, apiFile.getContentType());
		} catch(Exception ex) {
			logger.error("[{}] getFileContent()", targetPid, ex);
			return respError(ex);
		}
	}

	@Override
	public Response uploadFileContent(ApiFileUpload apiFileUpload) {
		UserProfileId targetPid = RunContext.getRunProfileId();
		try {
			VfsManager mmgr = VfsRestApiUtils.getVfsManager(targetPid);
			mmgr.addStoreFileFromStream(
					apiFileUpload.getStoreId(), apiFileUpload.getPath(), apiFileUpload.getName(), 
					new ByteArrayInputStream(java.util.Base64.getDecoder().decode(apiFileUpload.getBase64()))
			);

			return respOk();
		} catch(Exception ex) {
			logger.error("[{}] uploadFileContent()", targetPid, ex);
			return respError(ex);
		}
	}

	@Override
	protected Object createErrorEntity(Response.Status status, String message) {
		return new ApiApiError()
				.code(status.getStatusCode())
				.description(message);
	}
	
}
