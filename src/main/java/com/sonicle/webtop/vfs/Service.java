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

import com.dropbox.core.DbxAccountInfo;
import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxRequestConfig;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.oauth2.model.Userinfoplus;
import com.sonicle.commons.EnumUtils;
import com.sonicle.commons.PathUtils;
import com.sonicle.commons.URIUtils;
import com.sonicle.commons.time.DateTimeUtils;
import com.sonicle.commons.web.Crud;
import com.sonicle.commons.web.DispositionType;
import com.sonicle.commons.web.ServletUtils;
import com.sonicle.commons.web.ServletUtils.StringArray;
import com.sonicle.commons.web.json.CompositeId;
import com.sonicle.commons.web.json.JsonDataResult;
import com.sonicle.commons.web.json.JsonResult;
import com.sonicle.commons.web.json.MapItem;
import com.sonicle.commons.web.json.Payload;
import com.sonicle.commons.web.json.extjs.ExtTreeNode;
import com.sonicle.vfs2.VfsUtils;
import com.sonicle.vfs2.util.DropboxApiUtils;
import com.sonicle.vfs2.util.GoogleDriveApiUtils;
import com.sonicle.vfs2.util.GoogleDriveAppInfo;
import com.sonicle.webtop.core.CoreUserSettings;
import com.sonicle.webtop.core.app.sdk.BaseDocEditorDocumentHandler;
import com.sonicle.webtop.core.app.DocEditorManager;
import com.sonicle.webtop.core.app.WT;
import com.sonicle.webtop.core.app.WebTopSession;
import com.sonicle.webtop.core.bol.js.JsWizardData;
import com.sonicle.webtop.core.model.SharePermsRoot;
import com.sonicle.webtop.core.bol.model.Sharing;
import com.sonicle.webtop.core.sdk.BaseService;
import com.sonicle.webtop.core.sdk.UploadException;
import com.sonicle.webtop.core.sdk.UserProfile;
import com.sonicle.webtop.core.sdk.UserProfileId;
import com.sonicle.webtop.core.sdk.WTException;
import com.sonicle.webtop.core.sdk.interfaces.IServiceUploadStreamListener;
import com.sonicle.webtop.vfs.IVfsManager.StoreFileTemplate;
import com.sonicle.webtop.vfs.IVfsManager.StoreFileType;
import com.sonicle.webtop.vfs.bol.js.JsGridFile;
import com.sonicle.webtop.vfs.bol.js.JsGridSharingLink;
import com.sonicle.webtop.vfs.bol.js.JsSharing;
import com.sonicle.webtop.vfs.bol.js.JsSharingLink;
import com.sonicle.webtop.vfs.bol.js.JsStore;
import com.sonicle.webtop.vfs.model.Store;
import com.sonicle.webtop.vfs.model.StoreShareFolder;
import com.sonicle.webtop.vfs.model.StoreShareRoot;
import com.sonicle.webtop.vfs.bol.model.MyStoreFolder;
import com.sonicle.webtop.vfs.bol.model.MyStoreRoot;
import com.sonicle.webtop.vfs.model.SharingLink;
import com.sonicle.webtop.vfs.sfs.StoreFileSystem;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
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
		manager = (VfsManager)WT.getServiceManager(SERVICE_ID);
		ss = new VfsServiceSettings(SERVICE_ID, getEnv().getProfileId().getDomainId());
		us = new VfsUserSettings(SERVICE_ID, getEnv().getProfileId());
		initShares();
		
		registerUploadListener("UploadStoreFile", new OnUploadStoreFile());
	}

	@Override
	public void cleanup() throws Exception {
		
	}
	
	@Override
	public ServiceVars returnServiceVars() {
		ServiceVars co = new ServiceVars();
		co.put("privateUploadMaxFileSize", us.getPrivateUploadMaxFileSize(true));
		co.put("uploadLinkExpiration", ss.getUploadLinkExpiration());
		co.put("downloadLinkExpiration", ss.getDownloadLinkExpiration());
		co.put("nextcloudDefaultHost", ss.getNextcloudDefaultHost());
		co.put("nextcloudDefaultPath", ss.getNextcloudDefaultPath());
		co.put("fileEditAction", us.getFileEditAction());
		return co;
	}
	
	private WebTopSession getWts() {
		return getEnv().getWebTopSession();
	}
	
	private class OnUploadStoreFile implements IServiceUploadStreamListener {
		@Override
		public void onUpload(String context, HttpServletRequest request, HashMap<String, String> multipartParams, WebTopSession.UploadedFile file, InputStream is, MapItem responseData) throws UploadException {
			
			try {
				String parentFileId = multipartParams.get("fileId");
				if(StringUtils.isBlank(parentFileId)) throw new UploadException("Parameter not specified [fileId]");
				
				StoreNodeId parentNodeId = (StoreNodeId)new StoreNodeId().parse(parentFileId);
				int storeId = Integer.valueOf(parentNodeId.getStoreId());
				String path = (parentNodeId.getSize() == 2) ? "/" : parentNodeId.getPath();
				
				String newPath = manager.addStoreFileFromStream(storeId, path, file.getFilename(), is);

			} catch(UploadException ex) {
				logger.trace("Upload failure", ex);
				throw ex;
			} catch(Throwable t) {
				logger.error("Upload failure", t);
				throw new UploadException("Upload failure");
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
		UserProfileId pid = getEnv().getProfile().getId();
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
			return createRootNode(root.getShareId(), root.getOwnerProfileId().toString(), root.getPerms().toString(), lookupResource(VfsLocale.STORES_MY), false, "wtvfs-icon-storeMy").setExpanded(true);
		} else {
			return createRootNode(root.getShareId(), root.getOwnerProfileId().toString(), root.getPerms().toString(), root.getDescription(), false, "wtvfs-icon-storeIncoming");
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
		Store.Provider provider = store.getProvider();
		
		if (store.getBuiltIn().equals(Store.BUILTIN_NO)) {
			if (Store.Provider.FTP.equals(provider)) {
				return "storeFtp";
			} else if(Store.Provider.DROPBOX.equals(provider)) {
				return "storeDropbox";
			} else if(Store.Provider.GOOGLEDRIVE.equals(provider)) {
				return "storeGooDrive";
			} else if(Store.Provider.NEXTCLOUD.equals(provider)) {
				return "storeNextcloud";
			} else if(Store.Provider.FILE.equals(provider)) {
				return "storeFile";
			} else if(Store.Provider.SMB.equals(provider)) {
				return "storeSmb";
			} else if(Store.Provider.WEBDAV.equals(provider)) {
				return "storeWebdav";
			} else {
				return "store";
			}
		} else {
			if (Store.Provider.MYDOCUMENTS.equals(provider)) {
				return "storeMyDocs";
			} else if (Store.Provider.DOMAINIMAGES.equals(provider)) {
				return "storeDomainImages";
			} else {
				return "store";
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
		node.put("_scheme", store.getUri().getScheme());
		node.put("_builtIn", store.getBuiltIn());
		node.put("_rperms", rootPerms.toString());
		node.put("_fperms", folder.getPerms().toString());
		node.put("_eperms", folder.getElementsPerms().toString());
		
		List<String> classes = new ArrayList<>();
		if (!folder.getElementsPerms().implies("CREATE") 
				&& !folder.getElementsPerms().implies("UPDATE")
				&& !folder.getElementsPerms().implies("DELETE")) classes.add("wttasks-tree-readonly");
		node.setCls(StringUtils.join(classes, " "));
		
		node.setIconClass("wtvfs-icon-"+storeIcon(store));
		
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
			if (crud.equals(Crud.READ)) {
				String node = ServletUtils.getStringParameter(request, "node", true);
				boolean chooser = ServletUtils.getBooleanParameter(request, "chooser", false);
				
				if (node.equals("root")) { // Share roots...
					for (StoreShareRoot root : getRootsFromCache()) {
						children.add(createRootNode(root));
					}
				} else {
					StoreNodeId nodeId = (StoreNodeId)new StoreNodeId().parse(node);
					if (nodeId.getSize() == 1) { // Root share's folders...
						StoreShareRoot root = getRootFromCache(nodeId.getShareId());
						if (root instanceof MyStoreRoot) {
							for (Store cal : manager.listStores()) {
								MyStoreFolder folder = new MyStoreFolder(node, root.getOwnerProfileId(), cal);
								children.add(createFolderNode(folder, root.getPerms()));
							}
						} else {
							for (StoreShareFolder fold : getFoldersFromCache(root.getShareId())) {
								children.add(createFolderNode(fold, root.getPerms()));
							}
						}

					} else if (nodeId.getSize() == 2 || nodeId.getSize() == 3) { // Store's folders (2) or folder's folders (3)...
						int storeId = Integer.valueOf(nodeId.getStoreId());
						StoreShareFolder folder = getFolderFromCache(storeId);
						String path = (nodeId.getSize() == 2) ? "/" : nodeId.getPath();
						
						boolean showHidden = us.getFileShowHidden();
						
						LinkedHashMap<String, SharingLink> dls = null, uls = null;
						if (!chooser) {
							dls = manager.listDownloadLinks(storeId, path);
							uls = manager.listUploadLinks(storeId, path);
						}
						
						StoreFileSystem sfs = manager.getStoreFileSystem(storeId);
						for (FileObject fo : manager.listStoreFiles(StoreFileType.FOLDER, storeId, path)) {
							if (!showHidden && VfsUtils.isFileObjectHidden(fo)) continue;
							// Relativize path and force trailing separator (it's a folder)
							final String filePath = PathUtils.ensureTrailingSeparator(sfs.getRelativePath(fo), false);
							//final String fileId = new StoreNodeId(nodeId.getShareId(), nodeId.getStoreId(), filePath).toString();
							final String fileHash = VfsManagerUtils.generateStoreFileHash(storeId, filePath);
							
							String dlLink = null, ulLink = null;
							if ((dls != null) && dls.containsKey(fileHash)) {
								dlLink = dls.get(fileHash).getLinkId();
							}
							if ((uls != null) && uls.containsKey(fileHash)) {
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
		WebTopSession wts = getEnv().getWebTopSession();
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
				
				SetupDataFtp setup = new SetupDataFtp();
				setup.profileId = profileId;
				setup.scheme = scheme;
				setup.host = host;
				setup.port = port;
				setup.username = username;
				setup.password = password;
				setup.path = path;
				setup.updateName();
				wts.setProperty(SERVICE_ID, PROPERTY, setup);
				//TODO: controllo connessione
				
				new JsonResult(setup).printTo(out);
				
			} else if(crud.equals("s2")) {
				String name = ServletUtils.getStringParameter(request, "name", true);
				if(!wts.hasProperty(SERVICE_ID, PROPERTY)) throw new WTException();
				SetupDataFtp setup = (SetupDataFtp) wts.getProperty(SERVICE_ID, PROPERTY);
				
				Store store = new Store();
				store.setProfileId(new UserProfileId(setup.profileId));
				store.setName(StringUtils.defaultIfBlank(name, setup.name));
				store.setUri(setup.generateURI());
				store.setParameters(setup.generateParameters());
				store.setProvider(setup.getProvider());
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
		WebTopSession wts = getEnv().getWebTopSession();
		String PROPERTY = "SETUP_DROPBOX";
		String APP_NAME = WT.getPlatformName();
		String DROPBOX_APP_KEY = getEnv().getCoreServiceSettings().getDropboxAppKey();
		String DROPBOX_APP_SECRET = getEnv().getCoreServiceSettings().getDropboxAppSecret();
		String DROPBOX_USER_LOCALE = WT.getUserData(getEnv().getProfileId()).getLanguageTag();
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals("s1")) {
				String profileId = ServletUtils.getStringParameter(request, "profileId", true);
				
				SetupDataDropbox setup = new SetupDataDropbox();
				setup.profileId = profileId;
				setup.authUrl = DropboxApiUtils.getAuthorizationUrl(APP_NAME, DROPBOX_USER_LOCALE, DROPBOX_APP_KEY, DROPBOX_APP_SECRET);
				wts.setProperty(SERVICE_ID, PROPERTY, setup);
				
				new JsonResult(setup).printTo(out);
				
			} else if(crud.equals("s2")) {
				String code = ServletUtils.getStringParameter(request, "code", true);
				if(!wts.hasProperty(SERVICE_ID, PROPERTY)) throw new WTException();
				SetupDataDropbox setup = (SetupDataDropbox) wts.getProperty(SERVICE_ID, PROPERTY);
				
				DbxAppInfo appInfo = DropboxApiUtils.createAppInfo(DROPBOX_APP_KEY, DROPBOX_APP_SECRET);
				DbxRequestConfig reqConfig = DropboxApiUtils.createRequestConfig(APP_NAME, DROPBOX_USER_LOCALE);
				DbxAuthFinish auth = DropboxApiUtils.exchangeAuthorizationCode(code, reqConfig, appInfo);
				DbxAccountInfo ai = DropboxApiUtils.getAccountInfo(auth.accessToken, reqConfig);
				setup.accountId = String.valueOf(ai.userId);
				setup.accountName = ai.displayName;
				setup.accessToken = auth.accessToken;
				setup.updateName();
				
				new JsonResult(setup).printTo(out);
				
			} else if(crud.equals("s3")) {
				String name = ServletUtils.getStringParameter(request, "name", true);
				if(!wts.hasProperty(SERVICE_ID, PROPERTY)) throw new WTException();
				SetupDataDropbox setup = (SetupDataDropbox) wts.getProperty(SERVICE_ID, PROPERTY);
				
				Store store = new Store();
				store.setProfileId(new UserProfileId(setup.profileId));
				store.setName(StringUtils.defaultIfBlank(name, setup.name));
				store.setUri(setup.generateURI());
				store.setParameters(setup.generateParameters());
				store.setProvider(setup.getProvider());
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
		WebTopSession wts = getEnv().getWebTopSession();
		String PROPERTY = "SETUP_GOOGLEDRIVE";
		String APP_NAME = WT.getPlatformName();
		String GDRIVE_CLIENT_ID = getEnv().getCoreServiceSettings().getGoogleDriveClientID();
		String GDRIVE_CLIENT_SECRET = getEnv().getCoreServiceSettings().getGoogleDriveClientSecret();
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals("s1")) {
				String profileId = ServletUtils.getStringParameter(request, "profileId", true);
				
				GoogleDriveAppInfo appInfo = new GoogleDriveAppInfo(APP_NAME, GDRIVE_CLIENT_ID, GDRIVE_CLIENT_SECRET);
				SetupDataGoogleDrive setup = new SetupDataGoogleDrive();
				setup.profileId = profileId;
				setup.authUrl = GoogleDriveApiUtils.getAuthorizationUrl(appInfo);
				wts.setProperty(SERVICE_ID, PROPERTY, setup);
				
				new JsonResult(setup).printTo(out);
				
			} else if(crud.equals("s2")) {
				String code = ServletUtils.getStringParameter(request, "code", true);
				if(!wts.hasProperty(SERVICE_ID, PROPERTY)) throw new WTException();
				SetupDataGoogleDrive setup = (SetupDataGoogleDrive) wts.getProperty(SERVICE_ID, PROPERTY);
				
				GoogleDriveAppInfo appInfo = new GoogleDriveAppInfo(APP_NAME, GDRIVE_CLIENT_ID, GDRIVE_CLIENT_SECRET);
				GoogleCredential cred = GoogleDriveApiUtils.exchangeAuthorizationCode(code, appInfo);
				setup.refreshToken = cred.getRefreshToken();
				setup.accessToken = cred.getAccessToken();
				Userinfoplus uip = GoogleDriveApiUtils.getUserInfo(setup.accessToken, appInfo);
				setup.accountEmail = uip.getEmail();
				setup.accountName = uip.getName();
				setup.updateName();
				
				new JsonResult(setup).printTo(out);
				
			} else if(crud.equals("s3")) {
				String name = ServletUtils.getStringParameter(request, "name", true);
				if(!wts.hasProperty(SERVICE_ID, PROPERTY)) throw new WTException();
				SetupDataGoogleDrive setup = (SetupDataGoogleDrive) wts.getProperty(SERVICE_ID, PROPERTY);
				
				Store store = new Store();
				store.setProfileId(new UserProfileId(setup.profileId));
				store.setName(StringUtils.defaultIfBlank(name, setup.name));
				store.setUri(setup.generateURI());
				store.setParameters(setup.generateParameters());
				store.setProvider(setup.getProvider());
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
	
	public void processSetupStoreNextcloud(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		WebTopSession wts = getEnv().getWebTopSession();
		String PROPERTY = "SETUP_NEXTCLOUD";
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals("s1")) {
				String profileId = ServletUtils.getStringParameter(request, "profileId", true);
				String scheme = ServletUtils.getStringParameter(request, "scheme", true);
				String host = ServletUtils.getStringParameter(request, "host", true);
				Integer port = ServletUtils.getIntParameter(request, "port", null);
				String username = ServletUtils.getStringParameter(request, "username", null);
				String password = ServletUtils.getStringParameter(request, "password", null);
				String path = ServletUtils.getStringParameter(request, "path", null);
				
				SetupDataNextcloud setup = new SetupDataNextcloud();
				setup.profileId = profileId;
				setup.scheme = scheme;
				setup.host = host;
				setup.port = port;
				setup.username = username;
				setup.password = password;
				setup.path = path;
				setup.updateName();
				wts.setProperty(SERVICE_ID, PROPERTY, setup);
				//TODO: controllo connessione
				
				new JsonResult(setup).printTo(out);
				
			} else if(crud.equals("s2")) {
				String name = ServletUtils.getStringParameter(request, "name", true);
				if(!wts.hasProperty(SERVICE_ID, PROPERTY)) throw new WTException();
				SetupDataNextcloud setup = (SetupDataNextcloud) wts.getProperty(SERVICE_ID, PROPERTY);
				
				Store store = new Store();
				store.setProfileId(new UserProfileId(setup.profileId));
				store.setName(StringUtils.defaultIfBlank(name, setup.name));
				store.setUri(setup.generateURI());
				store.setParameters(setup.generateParameters());
				store.setProvider(setup.getProvider());
				manager.addStore(store);
				
				wts.clearProperty(SERVICE_ID, PROPERTY);
				updateFoldersCache();
				
				new JsonResult().printTo(out);
			}
			
		} catch (Exception ex) {
			logger.error("Error in SetupStoreNextcloud", ex);
			new JsonResult(false, ex.getMessage()).printTo(out);
		}
	}
	
	public void processSetupStoreFile(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		WebTopSession wts = getEnv().getWebTopSession();
		String PROPERTY = "SETUP_FILE";
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals("s1")) {
				String profileId = ServletUtils.getStringParameter(request, "profileId", true);
				String path = ServletUtils.getStringParameter(request, "path", null);
				
				SetupDataFile setup = new SetupDataFile();
				setup.profileId = profileId;
				setup.path = path;
				setup.updateName();
				wts.setProperty(SERVICE_ID, PROPERTY, setup);
				
				new JsonResult(setup).printTo(out);
				
			} else if(crud.equals("s2")) {
				String name = ServletUtils.getStringParameter(request, "name", true);
				if(!wts.hasProperty(SERVICE_ID, PROPERTY)) throw new WTException();
				SetupDataFile setup = (SetupDataFile) wts.getProperty(SERVICE_ID, PROPERTY);
				
				Store store = new Store();
				store.setProfileId(new UserProfileId(setup.profileId));
				store.setName(StringUtils.defaultIfBlank(name, setup.name));
				store.setUri(setup.generateURI());
				store.setParameters(setup.generateParameters());
				store.setProvider(setup.getProvider());
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
		WebTopSession wts = getEnv().getWebTopSession();
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
				
				SetupDataOther setup = new SetupDataOther();
				setup.profileId = profileId;
				setup.scheme = scheme;
				setup.host = host;
				setup.port = port;
				setup.username = username;
				setup.password = password;
				setup.path = path;
				setup.provider = EnumUtils.forSerializedName(scheme, Store.Provider.class);
				setup.updateName();
				wts.setProperty(SERVICE_ID, PROPERTY, setup);
				//TODO: controllo connessione
				
				new JsonResult(setup).printTo(out);
				
			} else if(crud.equals("s2")) {
				String name = ServletUtils.getStringParameter(request, "name", true);
				if(!wts.hasProperty(SERVICE_ID, PROPERTY)) throw new WTException();
				SetupDataOther setup = (SetupDataOther) wts.getProperty(SERVICE_ID, PROPERTY);
				
				Store store = new Store();
				store.setProfileId(new UserProfileId(setup.profileId));
				store.setName(StringUtils.defaultIfBlank(name, setup.name));
				store.setUri(setup.generateURI());
				store.setParameters(setup.generateParameters());
				store.setProvider(setup.getProvider());
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
			if (crud.equals(Crud.READ)) {
				String parentFileId = ServletUtils.getStringParameter(request, "fileId", null);
				
				StoreNodeId parentNodeId = (StoreNodeId)new StoreNodeId().parse(parentFileId);
				int storeId = Integer.valueOf(parentNodeId.getStoreId());
				StoreShareFolder folder = getFolderFromCache(storeId);
				String path = (parentNodeId.getSize() == 2) ? "/" : parentNodeId.getPath();
				
				boolean showHidden = us.getFileShowHidden();
				
				LinkedHashMap<String, SharingLink> dls = manager.listDownloadLinks(storeId, path);
				LinkedHashMap<String, SharingLink> uls = manager.listUploadLinks(storeId, path);
				
				StoreFileSystem sfs = manager.getStoreFileSystem(storeId);
				for (FileObject fo : manager.listStoreFiles(StoreFileType.FILE_OR_FOLDER, storeId, path)) {
					if (!showHidden && VfsUtils.isFileObjectHidden(fo)) continue;
					
					// Relativize path and force trailing separator if file is a folder
					final String filePath = fo.isFolder() ? PathUtils.ensureTrailingSeparator(sfs.getRelativePath(fo), false) : sfs.getRelativePath(fo);
					final String fileId = new StoreNodeId(parentNodeId.getShareId(), parentNodeId.getStoreId(), filePath).toString();
					final String fileHash = VfsManagerUtils.generateStoreFileHash(storeId, filePath);
					boolean canBeOpenedWithDocEditor = isFileEditableInDocEditor(fo.getName().getBaseName());
					items.add(new JsGridFile(folder, fo, fileId, canBeOpenedWithDocEditor, dls.get(fileHash), uls.get(fileHash), storeId, filePath));
				}
				new JsonResult("files", items).printTo(out);
			}
		
		} catch(Exception ex) {
			logger.error("Error in ManageGridFiles", ex);
			new JsonResult(false, "Error").printTo(out);
		}
	}
	
	public void processManageFiles(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if (crud.equals(Crud.CREATE)) {
				String parentFileId = ServletUtils.getStringParameter(request, "fileId", true);
				String type = ServletUtils.getStringParameter(request, "type", true);
				String name = ServletUtils.getStringParameter(request, "name", true);
				
				StoreNodeId parentNodeId = (StoreNodeId)new StoreNodeId().parse(parentFileId);
				int storeId = Integer.valueOf(parentNodeId.getStoreId());
				String path = (parentNodeId.getSize() == 2) ? "/" : parentNodeId.getPath();
				
				String newPath = null;
				if (type.equals("folder")) { // Create a folder
					newPath = manager.addStoreFile(StoreFileType.FOLDER, storeId, path, name);
					
				} else { // Create a file using template
					StoreFileTemplate fileTemplate = EnumUtils.forSerializedName(type, null, StoreFileTemplate.class);
					if (fileTemplate == null) throw new WTException("Type not supported [{}]", type);
					
					newPath = manager.addStoreFileFromTemplate(fileTemplate, storeId, path, name, false);
				}
				
				new JsonDataResult()
						.set("fileId", new StoreNodeId(parentNodeId.getShareId(), parentNodeId.getStoreId(), newPath).toString())
						.set("name", PathUtils.getFileName(newPath))
						.set("hash", VfsManagerUtils.generateStoreFileHash(storeId, newPath))
						.printTo(out);
				
			} else if(crud.equals("rename")) {
				String fileId = ServletUtils.getStringParameter(request, "fileId", true);
				String name = ServletUtils.getStringParameter(request, "name", true);
				
				StoreNodeId nodeId = (StoreNodeId)new StoreNodeId().parse(fileId);
				int storeId = Integer.valueOf(nodeId.getStoreId());
				
				try {
					String newPath = manager.renameStoreFile(storeId, nodeId.getPath(), name);
					new JsonDataResult()
							.set("fileId", new StoreNodeId(nodeId.getShareId(), nodeId.getStoreId(), newPath).toString())
							.set("name", PathUtils.getFileName(newPath))
							.set("hash", VfsManagerUtils.generateStoreFileHash(storeId, newPath))
							.printTo(out);
					
				} catch(FileOverwriteException ex) {
					new JsonResult(false, clientResTplString("gpfiles.error.rename")).printTo(out);
				}
				
			} else if(crud.equals(Crud.DELETE)) {
				StringArray fileIds = ServletUtils.getObjectParameter(request, "fileIds", StringArray.class, true);
				
				for(String fileId : fileIds) {
					StoreNodeId nodeId = (StoreNodeId)new StoreNodeId().parse(fileId);
					int storeId = Integer.valueOf(nodeId.getStoreId());
					manager.deleteStoreFile(storeId, nodeId.getPath());
				}
				new JsonResult().printTo(out);
				
			} else if (crud.equals("edit")) {
				String fileId = ServletUtils.getStringParameter(request, "fileId", true);
				
				StoreNodeId nodeId = (StoreNodeId)new StoreNodeId().parse(fileId);
				int storeId = Integer.valueOf(nodeId.getStoreId());
				
				FileObject fo = manager.getStoreFile(storeId, nodeId.getPath());
				if (!fo.isFile()) throw new WTException("Requested file is not a real file");
				final String filename = fo.getName().getBaseName();
				final String fileHash = VfsManagerUtils.generateStoreFileHash(storeId, nodeId.getPath());
				long lastModified = fo.getContent().getLastModifiedTime();
				
				boolean writable = false;
				StoreShareFolder folder = getFolderFromCache(storeId);
				if ((folder != null) && folder.getElementsPerms().implies("UPDATE")) writable = true;
				
				StoreFileDocEditorDocumentHandler docHandler = new StoreFileDocEditorDocumentHandler(writable, getEnv().getProfileId(), fileHash, storeId, nodeId.getPath());
				DocEditorManager.DocumentConfig config = getWts().prepareDocumentEditing(docHandler, filename, lastModified);
				
				new JsonResult(config).printTo(out);
			}
			
		} catch(Exception ex) {
			logger.error("Error in action ManageFiles", ex);
			new JsonResult(false, "Error").printTo(out);
		}
	}
	
	private static class StoreFileDocEditorDocumentHandler extends BaseDocEditorDocumentHandler {
		private final int storeId;
		private final String path;
		
		public StoreFileDocEditorDocumentHandler(boolean writeCapability, UserProfileId targetProfileId, String documentUniqueId, int storeId, String path) {
			super(writeCapability, targetProfileId, documentUniqueId);
			this.storeId = storeId;
			this.path = path;
		}
		
		@Override
		public long getLastModifiedTime() throws IOException {
			VfsManager manager = getVfsManager();
			
			try {
				FileObject fo = manager.getStoreFile(storeId, path);
				return fo.exists() ? fo.getContent().getLastModifiedTime() : -1;
				
			} catch(WTException ex) {
				throw new IOException("Unable to get file content", ex);
			}
		}
		
		@Override
		public InputStream readDocument() throws IOException {
			VfsManager manager = getVfsManager();
			
			try {
				FileObject fo = manager.getStoreFile(storeId, path);
				return fo.getContent().getInputStream();
				
			} catch(WTException ex) {
				throw new IOException("Unable to get file content", ex);
			}
		}
		
		@Override
		public void writeDocument(InputStream is) throws IOException {
			VfsManager manager = getVfsManager();
			String parentPath = PathUtils.getFullParentPath(path);
			String name = PathUtils.getFileName(path);
			
			try {
				manager.addStoreFileFromStream(storeId, parentPath, name, is, true);
				
			} catch(WTException ex) {
				throw new IOException("Unable to get file content", ex);
			}
		}
		
		private VfsManager getVfsManager() {
			return (VfsManager) WT.getServiceManager("com.sonicle.webtop.vfs", true, targetProfileId);
		}
	}
	
	public void processDownloadFiles(HttpServletRequest request, HttpServletResponse response) {
		
		try {
			StringArray fileIds = ServletUtils.getObjectParameter(request, "fileIds", StringArray.class, true);
			boolean inline = ServletUtils.getBooleanParameter(request, "inline", false);
			
			if (fileIds.size() > 1) throw new WTException("Unable to download multiple files for now");
			//if (fileIds.size() > 1) inline = false;
			String fileId = fileIds.get(0);
			//TODO: Implementare download file multipli
			
			StoreNodeId nodeId = (StoreNodeId)new StoreNodeId().parse(fileId);
			int storeId = Integer.valueOf(nodeId.getStoreId());
			
			FileObject fo = null;
			try {
				fo = manager.getStoreFile(storeId, nodeId.getPath());
				
				if (!fo.isFile()) {
					logger.warn("Cannot read a non-file [{}, {}]", storeId, nodeId.getPath());
					throw new WTException("Requested file is not a real file");
				}
				
				String filename = fo.getName().getBaseName();
				//String mediaType = ServletHelper.guessMediaType(filename, true);
				
				ServletUtils.setContentLengthHeader(response, fo.getContent().getSize());
				if (inline) {
					ServletUtils.setFileStreamHeaders(response, filename);
				} else {
					ServletUtils.setFileStreamHeadersForceDownload(response, filename);
				}
				IOUtils.copy(fo.getContent().getInputStream(), response.getOutputStream());
				
			} finally {
				IOUtils.closeQuietly(fo);
			}
			
		} catch(Exception ex) {
			logger.error("Error in DownloadFiles", ex);
			ServletUtils.writeErrorHandlingJs(response, ex.getMessage());
		}
	}
	
	public void processWizardDownloadLink(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		UserProfile up = getEnv().getProfile();
		CoreUserSettings cus = getEnv().getCoreUserSettings();
		
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
				dl.setLinkType(SharingLink.LinkType.DOWNLOAD);
				dl.setStoreId(storeId);
				dl.setFilePath(nodeId.getPath());
				if(!StringUtils.isBlank(expirationDate)) {
					DateTime dt = ymdHmsFmt.parseDateTime(expirationDate);
					dl.setExpiresOn(dt.withTimeAtStartOfDay());
				}
				dl.setAuthMode(EnumUtils.forSerializedName(authMode, SharingLink.AuthMode.class));
				dl.setPassword(password);
				dl.setNotify(true);
				dl = manager.addDownloadLink(dl);
				
				final String servicePublicUrl = WT.getServicePublicUrl(up.getDomainId(), SERVICE_ID);
				final URI[] urls = VfsManager.generateLinkPublicURLs(servicePublicUrl, dl);
				final String html = VfsManager.generateLinkEmbedCode(up.getLocale(), cus.getShortDateFormat(), servicePublicUrl, dl);
				
				JsWizardData data = new JsWizardData();
				data.put("urls", URIUtils.toURIStrings(urls, false));
				data.put("embed", html);
				new JsonResult(data).printTo(out);
			}
			
		} catch (Exception ex) {
			logger.error("Error in WizardDownloadLink", ex);
			new JsonResult(false, ex.getMessage()).printTo(out);
		}
	}
	
	public void processWizardUploadLink(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		UserProfile up = getEnv().getProfile();
		CoreUserSettings cus = getEnv().getCoreUserSettings();
		
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
				ul.setLinkType(SharingLink.LinkType.UPLOAD);
				ul.setStoreId(storeId);
				ul.setFilePath(nodeId.getPath());
				if (!StringUtils.isBlank(expirationDate)) {
					DateTime dt = ymdHmsFmt.parseDateTime(expirationDate);
					ul.setExpiresOn(dt.withTimeAtStartOfDay());
				}
				ul.setAuthMode(EnumUtils.forSerializedName(authMode, SharingLink.AuthMode.class));
				ul.setPassword(password);
				ul.setNotify(true);
				ul = manager.addUploadLink(ul);
				
				final String servicePublicUrl = WT.getServicePublicUrl(up.getDomainId(), SERVICE_ID);
				final URI[] urls = VfsManager.generateLinkPublicURLs(servicePublicUrl, ul);
				final String html = VfsManager.generateLinkEmbedCode(up.getLocale(), cus.getShortDateFormat(), servicePublicUrl, ul);
				
				JsWizardData data = new JsWizardData();
				data.put("urls", URIUtils.toURIStrings(urls, false));
				data.put("embed", html);
				new JsonResult(data).printTo(out);
			}
			
		} catch (Exception ex) {
			logger.error("Error in WizardUploadLink", ex);
			new JsonResult(false, ex.getMessage()).printTo(out);
		}
	}
	
	public void processGetLinkEmbedCode(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		UserProfile up = getEnv().getProfile();
		CoreUserSettings cus = getEnv().getCoreUserSettings();
		SharingLink item = null;
		
		try {
			String linkId = ServletUtils.getStringParameter(request, "linkId", true);
			
			item = manager.getSharingLink(linkId);
			String servicePublicUrl = WT.getServicePublicUrl(up.getDomainId(), SERVICE_ID);
			String html = VfsManager.generateLinkEmbedCode(up.getLocale(), cus.getShortDateFormat(), servicePublicUrl, item);
			
			new JsonResult(html).printTo(out);
			
		} catch(Exception ex) {
			logger.error("Error in GetLinkEmbedCode", ex);
			new JsonResult(false, "Error").printTo(out);
		}
	}
	
	public void processManageSharingLink(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		UserProfile up = getEnv().getProfile();
		DateTimeZone ptz = up.getTimeZone();
		SharingLink item = null;
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.READ)) {
				String linkId = ServletUtils.getStringParameter(request, "id", null);
				if(linkId == null) {
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
					item = manager.getSharingLink(linkId);
					
					final String servicePublicUrl = WT.getServicePublicUrl(up.getDomainId(), SERVICE_ID);
					final URI[] urls = VfsManager.generateLinkPublicURLs(servicePublicUrl, item);
					new JsonResult(new JsSharingLink(item, urls, up.getTimeZone())).printTo(out);
				}
				
			} else if(crud.equals(Crud.UPDATE)) {
				Payload<MapItem, JsSharingLink> pl = ServletUtils.getPayload(request, JsSharingLink.class);
				
				manager.updateSharingLink(JsSharingLink.createSharingLink(pl.data, up.getTimeZone()));
				new JsonResult().printTo(out);
				
			} else if(crud.equals(Crud.DELETE)) {
				String linkId = ServletUtils.getStringParameter(request, "id", true);
				
				manager.deleteSharingLink(linkId);
				new JsonResult().printTo(out);
			}
			
		} catch(Exception ex) {
			logger.error("Error in ManageSharingLink", ex);
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
}
