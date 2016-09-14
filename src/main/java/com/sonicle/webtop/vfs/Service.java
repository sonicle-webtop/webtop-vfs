/*
 * webtop-vfs is a WebTop Service developed by Sonicle S.r.l.
 * Copyright (C) 2014 Sonicle S.r.l.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation with the addition of the following permission
 * added to Section 15 as permitted in Section 7(a): FOR ANY PART OF THE COVERED
 * WORK IN WHICH THE COPYRIGHT IS OWNED BY SONICsonicLE, SONICLE DISCLAIMS THE
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

import com.dropbox.core.DbxAccountInfo;
import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxRequestConfig;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.oauth2.model.Userinfoplus;
import com.sonicle.commons.LangUtils;
import com.sonicle.commons.PathUtils;
import com.sonicle.commons.time.DateTimeUtils;
import com.sonicle.commons.web.Crud;
import com.sonicle.commons.web.DispositionType;
import com.sonicle.commons.web.ServletUtils;
import com.sonicle.commons.web.ServletUtils.StringArray;
import com.sonicle.commons.web.json.CompositeId;
import com.sonicle.commons.web.json.JsonResult;
import com.sonicle.commons.web.json.MapItem;
import com.sonicle.commons.web.json.Payload;
import com.sonicle.commons.web.json.extjs.ExtTreeNode;
import com.sonicle.vfs2.util.DropboxApiUtils;
import com.sonicle.vfs2.util.GoogleDriveApiUtils;
import com.sonicle.vfs2.util.GoogleDriveAppInfo;
import com.sonicle.webtop.core.app.RunContext;
import com.sonicle.webtop.core.app.WT;
import com.sonicle.webtop.core.app.WebTopSession;
import com.sonicle.webtop.core.bol.js.JsWizardData;
import com.sonicle.webtop.core.bol.model.SharePermsRoot;
import com.sonicle.webtop.core.bol.model.Sharing;
import com.sonicle.webtop.core.sdk.BaseService;
import com.sonicle.webtop.core.sdk.ServiceMessage;
import com.sonicle.webtop.core.sdk.UploadException;
import com.sonicle.webtop.core.sdk.UserProfile;
import com.sonicle.webtop.core.sdk.WTException;
import com.sonicle.webtop.core.sdk.interfaces.IServiceUploadListener;
import com.sonicle.webtop.core.sdk.interfaces.IServiceUploadStreamListener;
import com.sonicle.webtop.core.servlet.ServletHelper;
import com.sonicle.webtop.vfs.bol.js.JsGridFile;
import com.sonicle.webtop.vfs.bol.js.JsGridSharingLink;
import com.sonicle.webtop.vfs.bol.js.JsSharing;
import com.sonicle.webtop.vfs.bol.js.JsSharingLink;
import com.sonicle.webtop.vfs.bol.js.JsStore;
import com.sonicle.webtop.vfs.bol.model.Store;
import com.sonicle.webtop.vfs.bol.model.SetupParamsDropbox;
import com.sonicle.webtop.vfs.bol.model.SetupParamsFtp;
import com.sonicle.webtop.vfs.bol.model.SetupParamsGoogleDrive;
import com.sonicle.webtop.vfs.bol.model.StoreShareFolder;
import com.sonicle.webtop.vfs.bol.model.StoreShareRoot;
import com.sonicle.webtop.vfs.bol.model.MyStoreFolder;
import com.sonicle.webtop.vfs.bol.model.MyStoreRoot;
import com.sonicle.webtop.vfs.bol.model.SetupParamsFile;
import com.sonicle.webtop.vfs.bol.model.StoreFileType;
import com.sonicle.webtop.vfs.bol.model.SetupParamsOther;
import com.sonicle.webtop.vfs.bol.model.SharingLink;
import com.sonicle.webtop.vfs.sfs.StoreFileSystem;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;

/**
 *
 * @author malbinola
 */
public class Service extends BaseService {
	private static final Logger logger = WT.getLogger(Service.class);
	
	private VfsManager manager = null;
	private VfsServiceSettings ss = null;
	private VfsUserSettings us = null;
	
	private final LinkedHashMap<String, StoreShareRoot> cacheRootsById = new LinkedHashMap<>();
	private final HashMap<String, ArrayList<StoreShareFolder>> cacheFoldersByRoot = new HashMap<>();
	private final LinkedHashMap<Integer, StoreShareFolder> cacheFoldersByStore = new LinkedHashMap<>();

	@Override
	public void initialize() throws Exception {
		UserProfile up = getEnv().getProfile();
		manager = (VfsManager)WT.getServiceManager(SERVICE_ID, up.getId());
		ss = new VfsServiceSettings(SERVICE_ID, up.getDomainId());
		us = new VfsUserSettings(SERVICE_ID, up.getId());
		initShares();
		
		registerUploadListener("UploadStoreFile", new OnUploadStoreFile());
	}

	@Override
	public void cleanup() throws Exception {
		
	}
	
	@Override
	public ServiceVars returnServiceVars() {
		ServiceVars co = new ServiceVars();
		Integer maxUpload = WT.getCoreServiceSettings(SERVICE_ID).getUploadMaxFileSize();
		co.put("privateUploadMaxFileSize", LangUtils.coalesce(us.getPrivateUploadMaxFileSize(), maxUpload));
		co.put("publicUploadMaxFileSize", LangUtils.coalesce(us.getPublicUploadMaxFileSize(), maxUpload));
		co.put("uploadLinkExpiration", ss.getUploadLinkExpiration());
		co.put("downloadLinkExpiration", ss.getDownloadLinkExpiration());
		return co;
	}
	
	private class OnUploadStoreFile implements IServiceUploadStreamListener {

		@Override
		public void onUpload(String context, HttpServletRequest request, HashMap<String, String> multipartParams, WebTopSession.UploadedFile file, InputStream is, MapItem responseData) throws UploadException {
			
			if(context.equals("UploadStoreFile")) {
				try {
					String parentFileId = multipartParams.get("fileId");
					if(StringUtils.isBlank(parentFileId)) throw new UploadException("Parameter not specified [fileId]");

					StoreNodeId parentNodeId = (StoreNodeId)new StoreNodeId().parse(parentFileId);
					int storeId = Integer.valueOf(parentNodeId.getStoreId());
					String path = (parentNodeId.getSize() == 2) ? "/" : parentNodeId.getPath();

					String newPath = manager.createStoreFileFromStream(storeId, path, file.getFilename(), is);
					
				} catch(Exception ex) {
					logger.error("Unable to upload", ex);
					throw new UploadException("Unable to upload");
				}
			} else {
				throw new UploadException("Unknown context [{0}]", context);
			}
		}
	}
	
	private void initShares() throws WTException {
		synchronized(cacheRootsById) {
			updateRootsCache();
			updateFoldersCache();
		}
	}
	
	private void updateRootsCache() throws WTException {
		UserProfile.Id pid = getEnv().getProfile().getId();
		synchronized(cacheRootsById) {
			cacheRootsById.clear();
			cacheRootsById.put(MyStoreRoot.SHARE_ID, new MyStoreRoot(pid));
			for(StoreShareRoot root : manager.listIncomingStoreRoots()) {
				cacheRootsById.put(root.getShareId(), root);
			}
		}
	}
	
	private void updateFoldersCache() throws WTException {
		synchronized(cacheRootsById) {
			cacheFoldersByRoot.clear();
			cacheFoldersByStore.clear();
			for(StoreShareRoot root : cacheRootsById.values()) {
				cacheFoldersByRoot.put(root.getShareId(), new ArrayList<StoreShareFolder>());
				if(root instanceof MyStoreRoot) {
					for(Store store : manager.listStores()) {
						MyStoreFolder fold = new MyStoreFolder(root.getShareId(), root.getOwnerProfileId(), store);
						cacheFoldersByRoot.get(root.getShareId()).add(fold);
						cacheFoldersByStore.put(store.getStoreId(), fold);
					}
				} else {
					for(StoreShareFolder fold : manager.listIncomingStoreFolders(root.getShareId()).values()) {
						cacheFoldersByRoot.get(root.getShareId()).add(fold);
						cacheFoldersByStore.put(fold.getStore().getStoreId(), fold);
					}
				}
			}
		}
	}
	
	private Collection<StoreShareRoot> getRootsFromCache() {
		synchronized(cacheRootsById) {
			return cacheRootsById.values();
		}
	}
	
	private StoreShareRoot getRootFromCache(String shareId) {
		synchronized(cacheRootsById) {
			return cacheRootsById.get(shareId);
		}
	}
	
	private List<StoreShareFolder> getFoldersFromCache(String rootShareId) {
		synchronized(cacheRootsById) {
			if(cacheFoldersByRoot.containsKey(rootShareId)) {
				return cacheFoldersByRoot.get(rootShareId);
			} else {
				return new ArrayList<>();
			}
		}
	}
	
	private StoreShareFolder getFolderFromCache(int storeId) {
		synchronized(cacheRootsById) {
			return cacheFoldersByStore.get(storeId);
		}
	}
	
	public static class StoreNodeId extends CompositeId {
		
		public StoreNodeId() {
			super(3);
		}
		
		public StoreNodeId(String shareId, String storeId, String path) {
			this();
			setShareId(shareId);
			setStoreId(storeId);
			setPath(path);
		}
		
		public String getShareId() {
			return getToken(0);
		}
		
		public void setShareId(String shareId) {
			setToken(0, shareId);
		}
		
		public String getStoreId() {
			return getToken(1);
		}
		
		public void setStoreId(String storeId) {
			setToken(1, storeId);
		}
		
		public String getPath() {
			return getToken(2);
		}
		
		public void setPath(String path) {
			setToken(2, path);
		}
	}
	
	private ExtTreeNode createRootNode(StoreShareRoot root) {
		if(root instanceof MyStoreRoot) {
			return createRootNode(root.getShareId(), root.getOwnerProfileId().toString(), root.getPerms().toString(), lookupResource(VfsLocale.STORES_MY), false, "wtvfs-icon-myStore-xs").setExpanded(true);
		} else {
			return createRootNode(root.getShareId(), root.getOwnerProfileId().toString(), root.getPerms().toString(), root.getDescription(), false, "wtvfs-icon-incomingStore-xs");
		}
	}
	
	private ExtTreeNode createRootNode(String shareId, String pid, String perms, String text, boolean leaf, String iconClass) {
		StoreNodeId nodeId = new StoreNodeId();
		nodeId.setShareId(shareId);
		
		ExtTreeNode node = new ExtTreeNode(nodeId.toString(true), text, leaf);
		node.put("_type", "root");//JsFolderNode.TYPE_ROOT);
		node.put("_pid", pid);
		node.put("_rperms", perms);
		node.setIconClass(iconClass);
		return node;
	}
	
	private String storeIcon(Store store) {
		String uri = store.getUri();
		if(store.getBuiltIn()) {
			return "storeMyDocs";
		} else {
			if(StringUtils.startsWith(uri, "dropbox")) {
				return "storeDropbox";
			} else if(StringUtils.startsWith(uri, "file")) {
				return "storeFile";
			} else if(StringUtils.startsWith(uri, "ftp")) {
				return "storeFtp";
			} else if(StringUtils.startsWith(uri, "ftps")) {
				return "storeFtp";
			} else if(StringUtils.startsWith(uri, "googledrive")) {
				return "storeGooDrive";
			} else if(StringUtils.startsWith(uri, "sftp")) {
				return "storeFtp";
			} else if(StringUtils.startsWith(uri, "webdav")) {
				return "storeWebdav";
			} else if(StringUtils.startsWith(uri, "smb")) {
				return "storeSmb";
			}  else {
				return "store-xs";
			}
		}
	}
	
	private ExtTreeNode createFolderNode(StoreShareFolder folder, SharePermsRoot rootPerms) {
		Store store = folder.getStore();
		StoreNodeId nodeId = new StoreNodeId();
		nodeId.setShareId(folder.getShareId());
		nodeId.setStoreId(store.getStoreId().toString());
		
		ExtTreeNode node = new ExtTreeNode(nodeId.toString(true), store.getName(), false);
		node.put("_type", "folder");//JsFolderNode.TYPE_FOLDER);
		node.put("_pid", store.getProfileId().toString());
		node.put("_storeId", store.getStoreId());
		node.put("_builtIn", store.getBuiltIn());
		node.put("_rperms", rootPerms.toString());
		node.put("_fperms", folder.getPerms().toString());
		node.put("_eperms", folder.getElementsPerms().toString());
		
		List<String> classes = new ArrayList<>();
		if(!folder.getElementsPerms().implies("CREATE") 
				&& !folder.getElementsPerms().implies("UPDATE")
				&& !folder.getElementsPerms().implies("DELETE")) classes.add("wttasks-tree-readonly");
		node.setCls(StringUtils.join(classes, " "));
		
		node.setIconClass("wtvfs-icon-"+storeIcon(store)+"-xs");
		
		return node;
	}
	
	private ExtTreeNode createFileNode(StoreShareFolder folder, String filePath, String dlLink, String ulLink, FileObject fo) {
		StoreNodeId nodeId = new StoreNodeId();
		nodeId.setShareId(folder.getShareId());
		nodeId.setStoreId(String.valueOf(folder.getStore().getStoreId()));
		nodeId.setPath(filePath);
		
		ExtTreeNode node = new ExtTreeNode(nodeId.toString(true), fo.getName().getBaseName(), false);
		node.put("_type", "file");//JsFolderNode.TYPE_FOLDER);
		//node.put("_pid", store.getProfileId().toString());
		node.put("_storeId", folder.getStore().getStoreId());
		node.put("_eperms", folder.getElementsPerms().toString());
		node.put("_dlLink", dlLink);
		node.put("_ulLink", ulLink);
		
		return node;
	}
	
	public void processManageStoresTree(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		ArrayList<ExtTreeNode> children = new ArrayList<>();
		
		// Node ID is composed in this way:
		//	$shareId|$storeId|$path
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.READ)) {
				String node = ServletUtils.getStringParameter(request, "node", true);
				
				if(node.equals("root")) { // Share roots...
					for(StoreShareRoot root : getRootsFromCache()) {
						children.add(createRootNode(root));
					}
				} else {
					StoreNodeId nodeId = (StoreNodeId)new StoreNodeId().parse(node);
					if(nodeId.getSize() == 1) { // Root share's folders...
						StoreShareRoot root = getRootFromCache(nodeId.getShareId());
						if(root instanceof MyStoreRoot) {
							for(Store cal : manager.listStores()) {
								MyStoreFolder folder = new MyStoreFolder(node, root.getOwnerProfileId(), cal);
								children.add(createFolderNode(folder, root.getPerms()));
							}
						} else {
							for(StoreShareFolder fold : getFoldersFromCache(root.getShareId())) {
								children.add(createFolderNode(fold, root.getPerms()));
							}
						}

					} else if(nodeId.getSize() == 2 || nodeId.getSize() == 3) { // Store's folders (2) or folder's folders (3)...
						int storeId = Integer.valueOf(nodeId.getStoreId());
						StoreShareFolder folder = getFolderFromCache(storeId);
						String path = (nodeId.getSize() == 2) ? "/" : nodeId.getPath();
						
						boolean showHidden = us.getShowHiddenFiles();
						
						LinkedHashMap<String, SharingLink> dls = manager.listDownloadLinks(storeId, path);
						LinkedHashMap<String, SharingLink> uls = manager.listUploadLinks(storeId, path);
						
						StoreFileSystem sfs = manager.getStoreFileSystem(storeId);
						for(FileObject fo : manager.listStoreFiles(StoreFileType.FOLDER, storeId, path)) {
							if(!showHidden && isFileHidden(fo)) continue;
							// Relativize path and force trailing separator (it's a folder)
							final String filePath = PathUtils.ensureTrailingSeparator(sfs.getRelativePath(fo), false);
							//final String fileId = new StoreNodeId(nodeId.getShareId(), nodeId.getStoreId(), filePath).toString();
							final String fileHash = manager.generateStoreFileHash(storeId, filePath);
							
							String dlLink = null, ulLink = null;
							if(dls.containsKey(fileHash)) {
								dlLink = dls.get(fileHash).getLinkId();
							}
							if(uls.containsKey(fileHash)) {
								ulLink = uls.get(fileHash).getLinkId();
							}
							children.add(createFileNode(folder, filePath, dlLink, ulLink, fo));
						}
					}
				}
				
				new JsonResult("children", children).printTo(out);
			}
			
		} catch(Exception ex) {
			logger.error("Error in ManageStoresTree", ex);
		}
	}
	
	public void processManageSharing(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.READ)) {
				String id = ServletUtils.getStringParameter(request, "id", true);
				
				Sharing sharing = manager.getSharing(id);
				String description = buildSharingPath(sharing);
				new JsonResult(new JsSharing(sharing, description)).printTo(out);
				
			} else if(crud.equals(Crud.UPDATE)) {
				Payload<MapItem, Sharing> pl = ServletUtils.getPayload(request, Sharing.class);
				
				manager.updateSharing(pl.data);
				new JsonResult().printTo(out);
			}
			
		} catch(Exception ex) {
			logger.error("Error in action ManageSharing", ex);
			new JsonResult(false, "Error").printTo(out);
		}
	}
	
	public void processManageStores(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		Store item = null;
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.READ)) {
				Integer id = ServletUtils.getIntParameter(request, "id", true);
				
				item = manager.getStore(id);
				new JsonResult(new JsStore(item)).printTo(out);
				
			} else if(crud.equals(Crud.UPDATE)) {
				Payload<MapItem, JsStore> pl = ServletUtils.getPayload(request, JsStore.class);
				
				manager.updateStore(JsStore.createStore(pl.data));
				new JsonResult().printTo(out);
				
			} else if(crud.equals(Crud.DELETE)) {
				String storeId = ServletUtils.getStringParameter(request, "storeId", true);
				manager.deleteStore(Integer.valueOf(storeId));
				
				updateFoldersCache();
				
				new JsonResult().printTo(out);
			}
		
		} catch(Exception ex) {
			logger.error("Error in ManageStores", ex);
		}
	}
	
	public void processSetupStoreFtp(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		WebTopSession wts = RunContext.getWebTopSession();
		String PROPERTY = "SETUP_FTP";
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals("s1")) {
				String profileId = ServletUtils.getStringParameter(request, "profileId", true);
				String scheme = ServletUtils.getStringParameter(request, "scheme", true);
				String host = ServletUtils.getStringParameter(request, "host", true);
				Integer port = ServletUtils.getIntParameter(request, "port", null);
				String username = ServletUtils.getStringParameter(request, "username", true);
				String password = ServletUtils.getStringParameter(request, "password", null);
				String path = ServletUtils.getStringParameter(request, "path", null);
				
				SetupParamsFtp params = new SetupParamsFtp();
				params.profileId = profileId;
				params.scheme = scheme;
				params.host = host;
				params.port = port;
				params.username = username;
				params.password = password;
				params.path = path;
				params.buildName();
				wts.setProperty(SERVICE_ID, PROPERTY, params);
				//TODO: controllo connessione
				
				new JsonResult(params).printTo(out);
				
			} else if(crud.equals("s2")) {
				String name = ServletUtils.getStringParameter(request, "name", true);
				if(!wts.hasProperty(SERVICE_ID, PROPERTY)) throw new WTException();
				SetupParamsFtp params = (SetupParamsFtp) wts.getProperty(SERVICE_ID, PROPERTY);
				
				Store store = new Store();
				store.setProfileId(new UserProfile.Id(params.profileId));
				store.setName(StringUtils.defaultIfBlank(name, params.name));
				store.setUri(params.generateURI());
				manager.addStore(store);
				
				wts.clearProperty(SERVICE_ID, PROPERTY);
				updateFoldersCache();
				
				new JsonResult().printTo(out);
			}
			
		} catch (Exception ex) {
			logger.error("Error in SetupStoreFtp", ex);
			new JsonResult(false, ex.getMessage()).printTo(out);
		}
	}
	
	public void processSetupStoreDropbox(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		WebTopSession wts = RunContext.getWebTopSession();
		String PROPERTY = "SETUP_DROPBOX";
		String APP_NAME = WT.getPlatformName();
		String DROPBOX_APP_KEY = getEnv().getCoreServiceSettings().getDropboxAppKey();
		String DROPBOX_APP_SECRET = getEnv().getCoreServiceSettings().getDropboxAppSecret();
		String DROPBOX_USER_LOCALE = WT.getUserData(getEnv().getProfileId()).getLanguageTag();
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals("s1")) {
				String profileId = ServletUtils.getStringParameter(request, "profileId", true);
				
				SetupParamsDropbox params = new SetupParamsDropbox();
				params.profileId = profileId;
				params.authUrl = DropboxApiUtils.getAuthorizationUrl(APP_NAME, DROPBOX_USER_LOCALE, DROPBOX_APP_KEY, DROPBOX_APP_SECRET);
				wts.setProperty(SERVICE_ID, PROPERTY, params);
				
				new JsonResult(params).printTo(out);
				
			} else if(crud.equals("s2")) {
				String code = ServletUtils.getStringParameter(request, "code", true);
				if(!wts.hasProperty(SERVICE_ID, PROPERTY)) throw new WTException();
				SetupParamsDropbox params = (SetupParamsDropbox) wts.getProperty(SERVICE_ID, PROPERTY);
				
				DbxAppInfo appInfo = DropboxApiUtils.createAppInfo(DROPBOX_APP_KEY, DROPBOX_APP_SECRET);
				DbxRequestConfig reqConfig = DropboxApiUtils.createRequestConfig(APP_NAME, DROPBOX_USER_LOCALE);
				DbxAuthFinish auth = DropboxApiUtils.exchangeAuthorizationCode(code, reqConfig, appInfo);
				DbxAccountInfo ai = DropboxApiUtils.getAccountInfo(auth.accessToken, reqConfig);
				params.accountId = String.valueOf(ai.userId);
				params.accountName = ai.displayName;
				params.accessToken = auth.accessToken;
				params.buildName();
				
				new JsonResult(params).printTo(out);
				
			} else if(crud.equals("s3")) {
				String name = ServletUtils.getStringParameter(request, "name", true);
				if(!wts.hasProperty(SERVICE_ID, PROPERTY)) throw new WTException();
				SetupParamsDropbox params = (SetupParamsDropbox) wts.getProperty(SERVICE_ID, PROPERTY);
				
				Store store = new Store();
				store.setProfileId(new UserProfile.Id(params.profileId));
				store.setName(StringUtils.defaultIfBlank(name, params.name));
				store.setUri(params.generateURI());
				store.setParameters(LangUtils.serialize(params, SetupParamsDropbox.class));
				manager.addStore(store);
				
				wts.clearProperty(SERVICE_ID, PROPERTY);
				updateFoldersCache();
				
				new JsonResult().printTo(out);
			}
			
		} catch (Exception ex) {
			logger.error("Error in SetupStoreDropbox", ex);
			new JsonResult(false, ex.getMessage()).printTo(out);
		}
	}
	
	public void processSetupStoreGoogleDrive(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		WebTopSession wts = RunContext.getWebTopSession();
		String PROPERTY = "SETUP_GOOGLEDRIVE";
		String APP_NAME = WT.getPlatformName();
		String GDRIVE_CLIENT_ID = getEnv().getCoreServiceSettings().getGoogleDriveClientID();
		String GDRIVE_CLIENT_SECRET = getEnv().getCoreServiceSettings().getGoogleDriveClientSecret();
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals("s1")) {
				String profileId = ServletUtils.getStringParameter(request, "profileId", true);
				
				GoogleDriveAppInfo appInfo = new GoogleDriveAppInfo(APP_NAME, GDRIVE_CLIENT_ID, GDRIVE_CLIENT_SECRET);
				SetupParamsGoogleDrive params = new SetupParamsGoogleDrive();
				params.profileId = profileId;
				params.authUrl = GoogleDriveApiUtils.getAuthorizationUrl(appInfo);
				wts.setProperty(SERVICE_ID, PROPERTY, params);
				
				new JsonResult(params).printTo(out);
				
			} else if(crud.equals("s2")) {
				String code = ServletUtils.getStringParameter(request, "code", true);
				if(!wts.hasProperty(SERVICE_ID, PROPERTY)) throw new WTException();
				SetupParamsGoogleDrive params = (SetupParamsGoogleDrive) wts.getProperty(SERVICE_ID, PROPERTY);
				
				GoogleDriveAppInfo appInfo = new GoogleDriveAppInfo(APP_NAME, GDRIVE_CLIENT_ID, GDRIVE_CLIENT_SECRET);
				GoogleCredential cred = GoogleDriveApiUtils.exchangeAuthorizationCode(code, appInfo);
				params.refreshToken = cred.getRefreshToken();
				params.accessToken = cred.getAccessToken();
				Userinfoplus uip = GoogleDriveApiUtils.getUserInfo(params.accessToken, appInfo);
				params.accountEmail = uip.getEmail();
				params.accountName = uip.getName();
				params.buildName();
				
				new JsonResult(params).printTo(out);
				
			} else if(crud.equals("s3")) {
				String name = ServletUtils.getStringParameter(request, "name", true);
				if(!wts.hasProperty(SERVICE_ID, PROPERTY)) throw new WTException();
				SetupParamsGoogleDrive params = (SetupParamsGoogleDrive) wts.getProperty(SERVICE_ID, PROPERTY);
				
				Store store = new Store();
				store.setProfileId(new UserProfile.Id(params.profileId));
				store.setName(StringUtils.defaultIfBlank(name, params.name));
				store.setUri(params.generateURI());
				store.setParameters(LangUtils.serialize(params, SetupParamsGoogleDrive.class));
				manager.addStore(store);
				
				wts.clearProperty(SERVICE_ID, PROPERTY);
				updateFoldersCache();
				
				new JsonResult().printTo(out);
			}
			
		} catch (Exception ex) {
			logger.error("Error in SetupStoreGoogleDrive", ex);
			new JsonResult(false, ex.getMessage()).printTo(out);
		}
	}
	
	public void processSetupStoreFile(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		WebTopSession wts = RunContext.getWebTopSession();
		String PROPERTY = "SETUP_FILE";
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals("s1")) {
				String profileId = ServletUtils.getStringParameter(request, "profileId", true);
				String path = ServletUtils.getStringParameter(request, "path", null);
				
				SetupParamsFile params = new SetupParamsFile();
				params.profileId = profileId;
				params.path = path;
				params.buildName();
				wts.setProperty(SERVICE_ID, PROPERTY, params);
				
				new JsonResult(params).printTo(out);
				
			} else if(crud.equals("s2")) {
				String name = ServletUtils.getStringParameter(request, "name", true);
				if(!wts.hasProperty(SERVICE_ID, PROPERTY)) throw new WTException();
				SetupParamsFile params = (SetupParamsFile) wts.getProperty(SERVICE_ID, PROPERTY);
				
				Store store = new Store();
				store.setProfileId(new UserProfile.Id(params.profileId));
				store.setName(StringUtils.defaultIfBlank(name, params.name));
				store.setUri(params.generateURI());
				manager.addStore(store);
				
				wts.clearProperty(SERVICE_ID, PROPERTY);
				updateFoldersCache();
				
				new JsonResult().printTo(out);
			}
			
		} catch (Exception ex) {
			logger.error("Error in SetupStoreFile", ex);
			new JsonResult(false, ex.getMessage()).printTo(out);
		}
	}
	
	public void processSetupStoreOther(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		WebTopSession wts = RunContext.getWebTopSession();
		String PROPERTY = "SETUP_OTHER";
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals("s1")) {
				String profileId = ServletUtils.getStringParameter(request, "profileId", true);
				String scheme = ServletUtils.getStringParameter(request, "scheme", true);
				String host = ServletUtils.getStringParameter(request, "host", true);
				Integer port = ServletUtils.getIntParameter(request, "port", null);
				String username = ServletUtils.getStringParameter(request, "username", true);
				String password = ServletUtils.getStringParameter(request, "password", null);
				String path = ServletUtils.getStringParameter(request, "path", null);
				
				SetupParamsOther params = new SetupParamsOther();
				params.profileId = profileId;
				params.scheme = scheme;
				params.host = host;
				params.port = port;
				params.username = username;
				params.password = password;
				params.path = path;
				params.buildName();
				wts.setProperty(SERVICE_ID, PROPERTY, params);
				//TODO: controllo connessione
				
				new JsonResult(params).printTo(out);
				
			} else if(crud.equals("s2")) {
				String name = ServletUtils.getStringParameter(request, "name", true);
				if(!wts.hasProperty(SERVICE_ID, PROPERTY)) throw new WTException();
				SetupParamsOther params = (SetupParamsOther) wts.getProperty(SERVICE_ID, PROPERTY);
				
				Store store = new Store();
				store.setProfileId(new UserProfile.Id(params.profileId));
				store.setName(StringUtils.defaultIfBlank(name, params.name));
				store.setUri(params.generateURI());
				manager.addStore(store);
				
				wts.clearProperty(SERVICE_ID, PROPERTY);
				updateFoldersCache();
				
				new JsonResult().printTo(out);
			}
			
		} catch (Exception ex) {
			logger.error("Error in SetupStoreOther", ex);
			new JsonResult(false, ex.getMessage()).printTo(out);
		}
	}
	
	public void processManageGridFiles(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		ArrayList<JsGridFile> items = new ArrayList<>();
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.READ)) {
				String parentFileId = ServletUtils.getStringParameter(request, "fileId", null);
				
				StoreNodeId parentNodeId = (StoreNodeId)new StoreNodeId().parse(parentFileId);
				int storeId = Integer.valueOf(parentNodeId.getStoreId());
				StoreShareFolder folder = getFolderFromCache(storeId);
				String path = (parentNodeId.getSize() == 2) ? "/" : parentNodeId.getPath();
				
				boolean showHidden = us.getShowHiddenFiles();
				
				LinkedHashMap<String, SharingLink> dls = manager.listDownloadLinks(storeId, path);
				LinkedHashMap<String, SharingLink> uls = manager.listUploadLinks(storeId, path);
				
				StoreFileSystem sfs = manager.getStoreFileSystem(storeId);
				for(FileObject fo : manager.listStoreFiles(StoreFileType.FILE_OR_FOLDER, storeId, path)) {
					if(!showHidden && isFileHidden(fo)) continue;
					// Relativize path and force trailing separator if file is a folder
					final String filePath = fo.isFolder() ? PathUtils.ensureTrailingSeparator(sfs.getRelativePath(fo), false) : sfs.getRelativePath(fo);
					final String fileId = new StoreNodeId(parentNodeId.getShareId(), parentNodeId.getStoreId(), filePath).toString();
					final String fileHash = manager.generateStoreFileHash(storeId, filePath);
					items.add(new JsGridFile(folder, fo, fileId, dls.get(fileHash), uls.get(fileHash)));
				}
				new JsonResult("files", items).printTo(out);
			}
		
		} catch(Exception ex) {
			logger.error("Error in action ManageGridFiles", ex);
			new JsonResult(false, "Error").printTo(out);
		}
	}
	
	
	
	public void processManageFiles(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.CREATE)) {
				String parentFileId = ServletUtils.getStringParameter(request, "fileId", true);
				String name = ServletUtils.getStringParameter(request, "name", true);
				
				StoreNodeId parentNodeId = (StoreNodeId)new StoreNodeId().parse(parentFileId);
				int storeId = Integer.valueOf(parentNodeId.getStoreId());
				String path = (parentNodeId.getSize() == 2) ? "/" : parentNodeId.getPath();
				
				String newPath = manager.createStoreFile(StoreFileType.FOLDER, storeId, path, name);
				final String fileHash = manager.generateStoreFileHash(storeId, newPath);
				new JsonResult(fileHash).printTo(out);
				
			} else if(crud.equals("rename")) {
				String fileId = ServletUtils.getStringParameter(request, "fileId", true);
				String name = ServletUtils.getStringParameter(request, "name", true);
				
				StoreNodeId nodeId = (StoreNodeId)new StoreNodeId().parse(fileId);
				int storeId = Integer.valueOf(nodeId.getStoreId());
				
				String newPath = manager.renameStoreFile(storeId, nodeId.getPath(), name);
				final String fileHash = manager.generateStoreFileHash(storeId, newPath);
				new JsonResult(fileHash).printTo(out);
				
			} else if(crud.equals(Crud.DELETE)) {
				StringArray fileIds = ServletUtils.getObjectParameter(request, "fileIds", StringArray.class, true);
				
				for(String fileId : fileIds) {
					StoreNodeId nodeId = (StoreNodeId)new StoreNodeId().parse(fileId);
					int storeId = Integer.valueOf(nodeId.getStoreId());
					
					manager.deleteStoreFile(storeId, nodeId.getPath());
				}
				new JsonResult().printTo(out);
			}
			
		} catch(Exception ex) {
			logger.error("Error in action ManageFiles", ex);
			new JsonResult(false, "Error").printTo(out);
		}
	}
	
	public void processDownloadFiles(HttpServletRequest request, HttpServletResponse response) {
		
		try {
			StringArray fileIds = ServletUtils.getObjectParameter(request, "fileIds", StringArray.class, true);
			
			if(fileIds.size() > 1) throw new WTException("Unable to download multiple files for now");
			String fileId = fileIds.get(0);
			//TODO: Implementare download file multipli
			
			StoreNodeId nodeId = (StoreNodeId)new StoreNodeId().parse(fileId);
			int storeId = Integer.valueOf(nodeId.getStoreId());
			
			FileObject fo = null;
			try {
				fo = manager.getStoreFile(storeId, nodeId.getPath());
				
				if(!fo.isFile()) {
					logger.warn("Cannot download a non-file [{}, {}]", storeId, nodeId.getPath());
					throw new WTException("Requested file is not a real file");
				}
				
				String filename = fo.getName().getBaseName();
				String mediaType = ServletHelper.guessMediaType(filename, true);
				IOUtils.copy(fo.getContent().getInputStream(), response.getOutputStream());
				ServletUtils.setFileStreamHeaders(response, mediaType, DispositionType.ATTACHMENT, filename);
				ServletUtils.setContentLengthHeader(response, fo.getContent().getSize());
			} finally {
				IOUtils.closeQuietly(fo);
			}
			
		} catch(Exception ex) {
			logger.error("Error in action DownloadFiles", ex);
			ServletUtils.writeErrorHandlingJs(response, ex.getMessage());
		}
	}
	
	public void processWizardDownloadLink(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		UserProfile up = getEnv().getProfile();
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals("s2")) {
				String fileId = ServletUtils.getStringParameter(request, "fileId", true);
				String expirationDate = ServletUtils.getStringParameter(request, "expirationDate", null);
				String authMode = ServletUtils.getStringParameter(request, "authMode", true);
				String password = ServletUtils.getStringParameter(request, "password", null);
				
				StoreNodeId nodeId = (StoreNodeId)new StoreNodeId().parse(fileId);
				int storeId = Integer.valueOf(nodeId.getStoreId());
				
				DateTimeFormatter ymdHmsFmt = DateTimeUtils.createYmdHmsFormatter(up.getTimeZone());
				SharingLink dl = new SharingLink();
				dl.setType(SharingLink.TYPE_DOWNLOAD);
				dl.setStoreId(storeId);
				dl.setFilePath(nodeId.getPath());
				if(!StringUtils.isBlank(expirationDate)) {
					DateTime dt = ymdHmsFmt.parseDateTime(expirationDate);
					dl.setExpiresOn(DateTimeUtils.withTimeAtEndOfDay(dt));
				}
				dl.setAuthMode(authMode);
				dl.setPassword(password);
				dl = manager.addDownloadLink(dl);
				
				new JsonResult(new JsWizardData(dl)).printTo(out);
			}
			
		} catch (Exception ex) {
			logger.error("Error in WizardDownloadLink", ex);
			new JsonResult(false, ex.getMessage()).printTo(out);
		}
	}
	
	public void processWizardUploadLink(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		UserProfile up = getEnv().getProfile();
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals("s2")) {
				String fileId = ServletUtils.getStringParameter(request, "fileId", true);
				String expirationDate = ServletUtils.getStringParameter(request, "expirationDate", null);
				String authMode = ServletUtils.getStringParameter(request, "authMode", true);
				String password = ServletUtils.getStringParameter(request, "password", null);
				
				StoreNodeId nodeId = (StoreNodeId)new StoreNodeId().parse(fileId);
				int storeId = Integer.valueOf(nodeId.getStoreId());
				
				DateTimeFormatter ymdHmsFmt = DateTimeUtils.createYmdHmsFormatter(up.getTimeZone());
				SharingLink ul = new SharingLink();
				ul.setType(SharingLink.TYPE_UPLOAD);
				ul.setStoreId(storeId);
				ul.setFilePath(nodeId.getPath());
				if(!StringUtils.isBlank(expirationDate)) {
					DateTime dt = ymdHmsFmt.parseDateTime(expirationDate);
					ul.setExpiresOn(DateTimeUtils.withTimeAtEndOfDay(dt));
				}
				ul.setAuthMode(authMode);
				ul.setPassword(password);
				ul = manager.addUploadLink(ul);
				
				new JsonResult(new JsWizardData(ul)).printTo(out);
			}
			
		} catch (Exception ex) {
			logger.error("Error in WizardUploadLink", ex);
			new JsonResult(false, ex.getMessage()).printTo(out);
		}
	}
	
	public void processManageSharingLink(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		UserProfile up = getEnv().getProfile();
		DateTimeZone ptz = up.getTimeZone();
		SharingLink item = null;
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.READ)) {
				String id = ServletUtils.getStringParameter(request, "id", null);
				if(id == null) {
					List<JsGridSharingLink> items = new ArrayList<>();
					for(StoreShareRoot root : getRootsFromCache()) {
						for(StoreShareFolder folder : getFoldersFromCache(root.getShareId())) {
							final Store store = folder.getStore();
							StoreNodeId baseNodeId = new StoreNodeId();
							baseNodeId.setShareId(folder.getShareId());
							baseNodeId.setStoreId(store.getStoreId().toString());
							
							for(SharingLink dl : manager.listDownloadLinks(folder.getStore().getStoreId(), "/").values()) {
								items.add(new JsGridSharingLink(dl, "", store.getName(), storeIcon(folder.getStore()), baseNodeId, ptz));
							}
						}
					}
					for(StoreShareRoot root : getRootsFromCache()) {
						for(StoreShareFolder folder : getFoldersFromCache(root.getShareId())) {
							final Store store = folder.getStore();
							StoreNodeId baseNodeId = new StoreNodeId();
							baseNodeId.setShareId(folder.getShareId());
							baseNodeId.setStoreId(store.getStoreId().toString());
							
							for(SharingLink ul : manager.listUploadLinks(folder.getStore().getStoreId(), "/").values()) {
								items.add(new JsGridSharingLink(ul, "", store.getName(), storeIcon(folder.getStore()), baseNodeId, ptz));
							}
						}
					}
					new JsonResult("sharingLinks", items, items.size()).printTo(out);
				} else {
					item = manager.getSharingLink(id);
					
					new JsonResult(new JsSharingLink(item, up.getTimeZone())).printTo(out);
				}
				
			} else if(crud.equals(Crud.UPDATE)) {
				Payload<MapItem, JsSharingLink> pl = ServletUtils.getPayload(request, JsSharingLink.class);
				
				manager.updateSharingLink(JsSharingLink.createSharingLink(pl.data, up.getTimeZone()));
				new JsonResult().printTo(out);
				
			} else if(crud.equals(Crud.DELETE)) {
				String id = ServletUtils.getStringParameter(request, "id", true);
				
				manager.deleteSharingLink(id);
				new JsonResult().printTo(out);
			}
			
		} catch(Exception ex) {
			logger.error("Error in ManageActivities", ex);
			new JsonResult(false, "Error").printTo(out);
			
		}
	}
	
	private String buildSharingPath(Sharing sharing) throws WTException {
		StringBuilder sb = new StringBuilder();
		
		// Root description part
		CompositeId cid = new CompositeId().parse(sharing.getId());
		StoreShareRoot root = getRootFromCache(cid.getToken(0));
		if(root != null) {
			if(root instanceof MyStoreRoot) {
				sb.append(lookupResource(VfsLocale.STORES_MY));
			} else {
				sb.append(root.getDescription());
			}
		}
		
		// Folder description part
		if(sharing.getLevel() == 1) {
			int storeId = Integer.valueOf(cid.getToken(1));
			Store store = manager.getStore(storeId);
			sb.append("/");
			sb.append((store != null) ? store.getName() : cid.getToken(1));
		}
		
		return sb.toString();
	}
	
	private boolean isFileHidden(FileObject fo) throws FileSystemException {
		return fo.isHidden() || StringUtils.startsWith(fo.getName().getBaseName(), ".");
	}
}
