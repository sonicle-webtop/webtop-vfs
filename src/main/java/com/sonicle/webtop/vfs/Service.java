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
import com.sonicle.commons.web.ParameterException;
import com.sonicle.commons.web.ServletUtils;
import com.sonicle.commons.web.ServletUtils.StringArray;
import com.sonicle.commons.web.json.CompositeId;
import com.sonicle.commons.web.json.JsonDataResult;
import com.sonicle.commons.web.json.JsonResult;
import com.sonicle.commons.web.json.MapItem;
import com.sonicle.commons.web.json.Payload;
import com.sonicle.commons.web.json.extjs.ExtTreeNode;
import com.sonicle.vfs2.FileSelector;
import com.sonicle.vfs2.VfsUtils;
import com.sonicle.vfs2.util.DropboxApiUtils;
import com.sonicle.vfs2.util.GoogleDriveApiUtils;
import com.sonicle.vfs2.util.GoogleDriveAppInfo;
import com.sonicle.webtop.core.CoreUserSettings;
import com.sonicle.webtop.core.app.sdk.BaseDocEditorDocumentHandler;
import com.sonicle.webtop.core.app.DocEditorManager;
import com.sonicle.webtop.core.app.WT;
import com.sonicle.webtop.core.app.WebTopSession;
import com.sonicle.webtop.core.app.model.FolderShare;
import com.sonicle.webtop.core.app.model.FolderSharing;
import com.sonicle.webtop.core.app.sdk.AbstractFolderTreeCache;
import com.sonicle.webtop.core.app.sdk.WTParseException;
import com.sonicle.webtop.core.bol.js.JsWizardData;
import com.sonicle.webtop.core.sdk.BaseService;
import com.sonicle.webtop.core.sdk.UploadException;
import com.sonicle.webtop.core.sdk.UserProfile;
import com.sonicle.webtop.core.sdk.UserProfileId;
import com.sonicle.webtop.core.sdk.WTException;
import com.sonicle.webtop.core.sdk.interfaces.IServiceUploadStreamListener;
import com.sonicle.webtop.mail.IMailManager;
import com.sonicle.webtop.vfs.IVfsManager.StoreFileTemplate;
import com.sonicle.webtop.vfs.IVfsManager.StoreFileType;
import com.sonicle.webtop.vfs.bol.js.JsGridFile;
import com.sonicle.webtop.vfs.bol.js.JsGridSharingLink;
import com.sonicle.webtop.vfs.bol.js.JsSharingLink;
import com.sonicle.webtop.vfs.bol.js.JsStore;
import com.sonicle.webtop.vfs.bol.js.JsStoreSharing;
import com.sonicle.webtop.vfs.bol.model.MyStoreFSFolder;
import com.sonicle.webtop.vfs.bol.model.MyStoreFSOrigin;
import com.sonicle.webtop.vfs.bol.model.StoreNodeId;
import com.sonicle.webtop.vfs.model.Store;
import com.sonicle.webtop.vfs.model.SharingLink;
import com.sonicle.webtop.vfs.model.StoreFSFolder;
import com.sonicle.webtop.vfs.model.StoreFSOrigin;
import com.sonicle.webtop.vfs.sfs.StoreFileSystem;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.provider.UriParser;
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
	
	private final FoldersTreeCache foldersTreeCache = new FoldersTreeCache();

	@Override
	public void initialize() throws Exception {
		manager = (VfsManager)WT.getServiceManager(SERVICE_ID);
		ss = new VfsServiceSettings(SERVICE_ID, getEnv().getProfileId().getDomainId());
		us = new VfsUserSettings(SERVICE_ID, getEnv().getProfileId());
		initFolders();
		
		registerUploadListener("UploadStoreFile", new OnUploadStoreFile());
	}

	@Override
	public void cleanup() throws Exception {
		us = null;
		ss = null;
		manager = null;
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
	
	private void initFolders() throws WTException {
		foldersTreeCache.init();
	}
	
	private class OnUploadStoreFile implements IServiceUploadStreamListener {
		@Override
		public void onUpload(String context, HttpServletRequest request, HashMap<String, String> multipartParams, WebTopSession.UploadedFile file, InputStream is, MapItem responseData) throws UploadException {
			
			try {
				String parentFileId = multipartParams.get("fileId");
				String duplMode = StringUtils.defaultIfBlank(multipartParams.get("dupl"), "rename");
				if (StringUtils.isBlank(parentFileId)) throw new UploadException("Parameter not specified [fileId]");
				
				final StoreNodeId parentNodeId = new StoreNodeId(parentFileId);
				final String path = StoreNodeId.Type.FOLDER.equals(parentNodeId.getType()) ? "/" : parentNodeId.getFilePath();
				
				boolean overwrite = "overwrite".equals(duplMode);
				String newPath = manager.addStoreFileFromStream(parentNodeId.getFolderId(), path, file.getFilename(), is, overwrite);

			} catch(UploadException ex) {
				logger.trace("Upload failure", ex);
				throw ex;
			} catch(Throwable t) {
				logger.error("Upload failure", t);
				throw new UploadException("Upload failure");
			}
		}
	}
	
	private ExtTreeNode createFolderNodeLevel0(StoreFSOrigin origin) {
		StoreNodeId nodeId = StoreNodeId.build(StoreNodeId.Type.ORIGIN, origin.getProfileId());
		if (origin instanceof MyStoreFSOrigin) {
			return createFolderNodeLevel0(nodeId, lookupResource(VfsLocale.STORES_MY), "wtvfs-icon-storeMy", origin.getWildcardPermissions());
		} else {
			return createFolderNodeLevel0(nodeId, origin.getDisplayName(), "wtvfs-icon-storeIncoming", origin.getWildcardPermissions());
		}
	}
	
	private ExtTreeNode createFolderNodeLevel0(StoreNodeId nodeId, String text, String iconClass, FolderShare.Permissions originPermissions) {
		ExtTreeNode node = new ExtTreeNode(nodeId.toString(), text, false);
		node.put("_orPerms", originPermissions.getFolderPermissions().toString(true));
		node.setIconClass(iconClass);
		node.put("expandable", false);
		node.setExpanded(true);
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
			} else if(Store.Provider.WEBDAV.equals(provider) || Store.Provider.WEBDAVS.equals(provider)) {
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
	
	private ExtTreeNode createFolderNodeLevel1(StoreFSOrigin origin, StoreFSFolder folder) {
		StoreNodeId.Type type = StoreNodeId.Type.FOLDER;
		final StoreNodeId nodeId = StoreNodeId.build(type, origin.getProfileId(), folder.getFolderId());
		final String name = folder.getDisplayName();
		//ExtTreeNode node = createFolderNodeLevel1(nodeId, name, folder.getPermissions(), folder.getStore());
		//if (origin instanceof MyStoreFSOrigin && folder.getStore().getBuiltIn() == 100) node.setExpanded(true);
		//return node;
		return createFolderNodeLevel1(nodeId, name, folder.getPermissions(), folder.getStore());
	}
	
	private ExtTreeNode createFolderNodeLevel1(StoreNodeId nodeId, String name, FolderShare.Permissions folderPermissions, Store store) {
		ExtTreeNode node = new ExtTreeNode(nodeId.toString(), name, false);
		node.put("_foPerms", folderPermissions.getFolderPermissions().toString());
		//node.put("_foPerms", store.getBuiltIn() > 0 ? "r" : folderPermissions.getFolderPermissions().toString());
		node.put("_itPerms", folderPermissions.getItemsPermissions().toString());
		node.put("_scheme", store.getUri().getScheme());
		node.put("_builtIn", store.getBuiltIn());
		
		List<String> classes = new ArrayList<>();
		if (!folderPermissions.getItemsPermissions().has(FolderShare.ItemsRight.CREATE) 
			&& !folderPermissions.getItemsPermissions().has(FolderShare.ItemsRight.UPDATE)
			&& !folderPermissions.getItemsPermissions().has(FolderShare.ItemsRight.DELETE)) classes.add("wttasks-tree-readonly");
		node.setCls(StringUtils.join(classes, " "));
		node.setIconClass("wtvfs-icon-"+storeIcon(store));
		return node;
	}
	
	private ExtTreeNode createFolderNodeLevel2(StoreFSOrigin origin, StoreFSFolder folder, FileObject fo, String filePath, String dlLink, String ulLink) throws FileSystemException {
		StoreNodeId.Type type = StoreNodeId.Type.FILEOBJECT;
		final StoreNodeId nodeId = StoreNodeId.build(type, origin.getProfileId(), folder.getFolderId(), filePath);
		final String name = UriParser.decode(fo.getName().getBaseName());
		return createFolderNodeLevel2(nodeId, name, folder.getPermissions(), dlLink, ulLink);
	}
	
	private ExtTreeNode createFolderNodeLevel2(StoreNodeId nodeId, String name, FolderShare.Permissions folderPermissions, String dlLink, String ulLink) {
		ExtTreeNode node = new ExtTreeNode(nodeId.toString(), name, false);
		node.put("_foPerms", folderPermissions.getFolderPermissions().toString());
		node.put("_itPerms", folderPermissions.getItemsPermissions().toString());
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
			if (Crud.READ.equals(crud)) {
				String node = ServletUtils.getStringParameter(request, "node", true);
				boolean chooser = ServletUtils.getBooleanParameter(request, "chooser", false);
				boolean writableOnly = ServletUtils.getBooleanParameter(request, "writableOnly", false);
				
				if (node.equals("root")) { // Tree ROOT node -> list folder origins
					for (StoreFSOrigin origin : foldersTreeCache.getOrigins()) {
						boolean add = true;
						if (writableOnly && !(origin instanceof MyStoreFSOrigin)) {
							// Exclude origins whose folders do NOT have writing rights
							for (StoreFSFolder folder : foldersTreeCache.getFoldersByOrigin(origin)) {
								if (!folder.getPermissions().getItemsPermissions().has(FolderShare.ItemsRight.CREATE)) {
									add = false;
									break;
								}
							}
						}
						if (add) {
							final ExtTreeNode xnode = createFolderNodeLevel0(origin);
							if (xnode != null) children.add(xnode);
						}
					}
					
				} else {
					StoreNodeId nodeId = new StoreNodeId(node);
					if (StoreNodeId.Type.ORIGIN.equals(nodeId.getType())) { // Tree node -> list folder of specified origin
						final StoreFSOrigin origin = foldersTreeCache.getOriginByProfile(nodeId.getOriginAsProfileId());
						for (StoreFSFolder folder : foldersTreeCache.getFoldersByOrigin(origin)) {
							if (writableOnly && !folder.getPermissions().getItemsPermissions().has(FolderShare.ItemsRight.CREATE)) continue;
							
							final ExtTreeNode xnode = createFolderNodeLevel1(origin, folder);
							if (xnode != null) children.add(xnode);
						}
						
					} else if (StoreNodeId.Type.FOLDER.equals(nodeId.getType()) || StoreNodeId.Type.FILEOBJECT.equals(nodeId.getType())) {
						final StoreFSOrigin origin = foldersTreeCache.getOriginByProfile(nodeId.getOriginAsProfileId());
						final StoreFSFolder folder = foldersTreeCache.getFolder(nodeId.getFolderId());
						final String path = StoreNodeId.Type.FOLDER.equals(nodeId.getType()) ? "/" : nodeId.getFilePath();
						
						boolean showHidden = us.getFileShowHidden();
						LinkedHashMap<String, SharingLink> dls = null, uls = null;
						if (!chooser) {
							dls = manager.listDownloadLinks(nodeId.getFolderId(), path);
							uls = manager.listUploadLinks(nodeId.getFolderId(), path);
						}
						
						StoreFileSystem sfs = manager.getStoreFileSystem(nodeId.getFolderId());
						for (FileObject fo : manager.listStoreFiles(StoreFileType.FOLDER, nodeId.getFolderId(), path)) {
							if (!showHidden && VfsUtils.isFileObjectHidden(fo)) continue;
							// Relativize path and force trailing separator (it's a folder)
							final String filePath = PathUtils.ensureTrailingSeparator(sfs.getRelativePath(fo), false);
							//final String fileId = new StoreNodeId(nodeId.getShareId(), nodeId.getStoreId(), filePath).toString();
							final String fileHash = VfsManagerUtils.generateStoreFileHash(nodeId.getFolderId(), filePath);
							
							String dlLink = null, ulLink = null;
							if ((dls != null) && dls.containsKey(fileHash)) {
								dlLink = dls.get(fileHash).getLinkId();
							}
							if ((uls != null) && uls.containsKey(fileHash)) {
								ulLink = uls.get(fileHash).getLinkId();
							}
							children.add(createFolderNodeLevel2(origin, folder, fo, filePath, dlLink, ulLink));
						}
						
					} else {
						throw new WTParseException("Unable to parse '{}' as node ID", node);
					}
				}
				
				new JsonResult("children", children).printTo(out);
			}
			
		} catch (Exception ex) {
			logger.error("Error in ManageStoresTree", ex);
		}
	}
	
	public void processManageSharing(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if (Crud.READ.equals(crud)) {
				String node = ServletUtils.getStringParameter(request, "id", true);
				
				StoreNodeId nodeId = new StoreNodeId(node);
				FolderSharing.Scope scope = JsStoreSharing.toFolderSharingScope(nodeId);
				Set<FolderSharing.SubjectConfiguration> configurations = manager.getFolderShareConfigurations(nodeId.getOriginAsProfileId(), scope);
				String[] sdn = buildSharingDisplayNames(nodeId);
				new JsonResult(new JsStoreSharing(nodeId, sdn[0], sdn[1], configurations)).printTo(out);
				
			} else if (Crud.UPDATE.equals(crud)) {
				Payload<MapItem, JsStoreSharing> pl = ServletUtils.getPayload(request, JsStoreSharing.class);
				
				StoreNodeId nodeId = new StoreNodeId(pl.data.id);
				FolderSharing.Scope scope = JsStoreSharing.toFolderSharingScope(nodeId);
				manager.updateFolderShareConfigurations(nodeId.getOriginAsProfileId(), scope, pl.data.toSubjectConfigurations());
				new JsonResult().printTo(out);
			}
			
		} catch (Exception ex) {
			logger.error("Error in ManageSharing", ex);
			new JsonResult(ex).printTo(out);
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
				foldersTreeCache.init(AbstractFolderTreeCache.Target.FOLDERS);
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
				foldersTreeCache.init(AbstractFolderTreeCache.Target.FOLDERS);
				
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
				foldersTreeCache.init(AbstractFolderTreeCache.Target.FOLDERS);
				
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
				foldersTreeCache.init(AbstractFolderTreeCache.Target.FOLDERS);
				
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
				foldersTreeCache.init(AbstractFolderTreeCache.Target.FOLDERS);
				
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
				foldersTreeCache.init(AbstractFolderTreeCache.Target.FOLDERS);
				
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
				foldersTreeCache.init(AbstractFolderTreeCache.Target.FOLDERS);
				
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
				
				final StoreNodeId parentNodeId = new StoreNodeId(parentFileId);
				final StoreFSFolder folder = foldersTreeCache.getFolder(parentNodeId.getFolderId());
				final String path = StoreNodeId.Type.FOLDER.equals(parentNodeId.getType()) ? "/" : parentNodeId.getFilePath();
				
				boolean showHidden = us.getFileShowHidden();
				LinkedHashMap<String, SharingLink> dls = manager.listDownloadLinks(parentNodeId.getFolderId(), path);
				LinkedHashMap<String, SharingLink> uls = manager.listUploadLinks(parentNodeId.getFolderId(), path);
				
				StoreFileSystem sfs = manager.getStoreFileSystem(parentNodeId.getFolderId());
				for (FileObject fo : manager.listStoreFiles(StoreFileType.FILE_OR_FOLDER, parentNodeId.getFolderId(), path)) {
					if (!showHidden && VfsUtils.isFileObjectHidden(fo)) continue;
					
					// Relativize path and force trailing separator if file is a folder
					final String filePath = fo.isFolder() ? PathUtils.ensureTrailingSeparator(sfs.getRelativePath(fo), false) : sfs.getRelativePath(fo);
					final String fileId = StoreNodeId.build(StoreNodeId.Type.FILEOBJECT, parentNodeId.getOriginAsProfileId(), parentNodeId.getFolderId(), filePath).toString();
					final String fileHash = VfsManagerUtils.generateStoreFileHash(parentNodeId.getFolderId(), filePath);
					boolean canBeOpenedWithDocEditor = isFileEditableInDocEditor(fo.getName().getBaseName());
					items.add(new JsGridFile(folder, fo, fileId, canBeOpenedWithDocEditor, dls.get(fileHash), uls.get(fileHash), parentNodeId.getFolderId(), filePath));
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
				
				final StoreNodeId parentNodeId = new StoreNodeId(parentFileId);
				final String path = StoreNodeId.Type.FOLDER.equals(parentNodeId.getType()) ? "/" : parentNodeId.getFilePath();
				
				String newPath = null;
				if (type.equals("folder")) { // Create a folder
					newPath = manager.addStoreFile(StoreFileType.FOLDER, parentNodeId.getFolderId(), path, name);
					
				} else { // Create a file using template
					StoreFileTemplate fileTemplate = EnumUtils.forSerializedName(type, null, StoreFileTemplate.class);
					if (fileTemplate == null) throw new WTException("Type not supported [{}]", type);
					
					newPath = manager.addStoreFileFromTemplate(fileTemplate, parentNodeId.getFolderId(), path, name, false);
				}
				
				new JsonDataResult()
					.set("fileId", StoreNodeId.build(StoreNodeId.Type.FILEOBJECT, parentNodeId.getOriginAsProfileId(), parentNodeId.getFolderId(), newPath).toString())
					.set("name", PathUtils.getFileName(newPath))
					.set("hash", VfsManagerUtils.generateStoreFileHash(parentNodeId.getFolderId(), newPath))
					.printTo(out);
				
			} else if(crud.equals("rename")) {
				String fileId = ServletUtils.getStringParameter(request, "fileId", true);
				String name = ServletUtils.getStringParameter(request, "name", true);
				
				final StoreNodeId nodeId = new StoreNodeId(fileId);
				try {
					String newPath = manager.renameStoreFile(nodeId.getFolderId(), nodeId.getFilePath(), name);
					new JsonDataResult()
						.set("fileId", StoreNodeId.build(StoreNodeId.Type.FILEOBJECT, nodeId.getOriginAsProfileId(), nodeId.getFolderId(), newPath).toString())
						.set("name", PathUtils.getFileName(newPath))
						.set("hash", VfsManagerUtils.generateStoreFileHash(nodeId.getFolderId(), newPath))
						.printTo(out);
					
				} catch(FileOverwriteException ex) {
					new JsonResult(false, clientResTplString("gpfiles.error.rename")).printTo(out);
				}
				
			} else if(crud.equals(Crud.DELETE)) {
				StringArray fileIds = ServletUtils.getObjectParameter(request, "fileIds", StringArray.class, true);
				
				for(String fileId : fileIds) {
					final StoreNodeId nodeId = new StoreNodeId(fileId);
					manager.deleteStoreFile(nodeId.getFolderId(), nodeId.getFilePath());
				}
				new JsonResult().printTo(out);
				
			} else if (crud.equals("edit")) {
				String fileId = ServletUtils.getStringParameter(request, "fileId", true);
				
				final StoreNodeId nodeId = new StoreNodeId(fileId);
				
				FileObject fo = manager.getStoreFile(nodeId.getFolderId(), nodeId.getFilePath());
				if (!fo.isFile()) throw new WTException("Requested file is not a real file");
				final String filename = fo.getName().getBaseName();
				final String fileHash = VfsManagerUtils.generateStoreFileHash(nodeId.getFolderId(), nodeId.getFilePath());
				long lastModified = fo.getContent().getLastModifiedTime();
				
				boolean writable = false;
				final StoreFSFolder folder = foldersTreeCache.getFolder(nodeId.getFolderId());
				if ((folder != null) && folder.getPermissions().getItemsPermissions().has(FolderShare.ItemsRight.UPDATE)) writable = true;
				
				StoreFileDocEditorDocumentHandler docHandler = new StoreFileDocEditorDocumentHandler(writable, getEnv().getProfileId(), fileHash, nodeId.getFolderId(), nodeId.getFilePath());
				DocEditorManager.EditingResult result = getWts().docEditorPrepareEditing(docHandler, filename, lastModified);
				
				new JsonResult(result).printTo(out);
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
			boolean recursive = ServletUtils.getBooleanParameter(request, "recursive", false);
			StringArray fileIds = ServletUtils.getObjectParameter(request, "fileIds", StringArray.class, true);
			boolean inline = ServletUtils.getBooleanParameter(request, "inline", false);
			
			if (fileIds.size() > 1) throw new WTException("Unable to download multiple files for now");
			//if (fileIds.size() > 1) inline = false;
			String fileId = fileIds.get(0);
			//TODO: Implementare download file multipli
			
			final StoreNodeId nodeId = new StoreNodeId(fileId);
			
			FileObject fo = null;
			try {
				fo = manager.getStoreFile(nodeId.getFolderId(), nodeId.getFilePath());
				if (fo.isFolder()) {
					String filename = fo.getName().getBaseName() + ".zip";
					ServletUtils.setFileStreamHeaders(response, "application/x-zip-compressed", DispositionType.ATTACHMENT, filename);
					
					JarOutputStream jos = null;
					try {
						jos = new JarOutputStream(response.getOutputStream());
						writeFileObject(fo, recursive, jos);
						
					} finally {
						IOUtils.closeQuietly(jos);
					}
					
				} else if (fo.isFile()) {
					String filename = fo.getName().getBaseName();
					//String mediaType = ServletHelper.guessMediaType(filename, true);
					ServletUtils.setContentLengthHeader(response, fo.getContent().getSize());
					if (inline) {
						ServletUtils.setFileStreamHeaders(response, filename);
					} else {
						ServletUtils.setFileStreamHeadersForceDownload(response, filename);
					}
					IOUtils.copy(fo.getContent().getInputStream(), response.getOutputStream());
					
				} else {
					logger.warn("Cannot read a non-file [{}, {}]", nodeId.getFolderId(), nodeId.getFilePath());
					throw new WTException("Requested file is not a real file");
				}
				
			} finally {
				IOUtils.closeQuietly(fo);
			}
			
		} catch(Exception ex) {
			logger.error("Error in DownloadFiles", ex);
			ServletUtils.writeErrorHandlingJs(response, ex.getMessage());
		}
	}
	
	private void writeFileObject(FileObject baseFo, boolean recursive, JarOutputStream jos) throws FileSystemException, IOException {
		writeFileObject(baseFo, baseFo.findFiles(new FileSelector(true, true)), recursive, jos);
	}
	
	private void writeFileObject(FileObject baseFo, FileObject[] fileObjects, boolean recursive, JarOutputStream jos) throws FileSystemException, IOException {
		for (FileObject fo : fileObjects) {
			if (fo.isFolder()) {
				if (recursive) writeFileObject(baseFo, fo.findFiles(new FileSelector(true, true)), recursive, jos);
			} else {
				String relName = baseFo.getName().getRelativeName(fo.getName());
				jos.putNextEntry(new JarEntry(relName));
				IOUtils.copy(fo.getContent().getInputStream(), jos);
			}
		}
	}
	
	public void processWizardDownloadLink(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		UserProfile up = getEnv().getProfile();
		CoreUserSettings cus = getEnv().getCoreUserSettings();
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if (crud.equals("s2")) {
				String fileId = ServletUtils.getStringParameter(request, "fileId", true);
				String expirationDate = ServletUtils.getStringParameter(request, "expirationDate", null);
				String authMode = ServletUtils.getStringParameter(request, "authMode", true);
				String password = ServletUtils.getStringParameter(request, "password", null);
				final StoreNodeId nodeId = new StoreNodeId(fileId);
				
				DateTimeFormatter ymdHmsFmt = DateTimeUtils.createYmdHmsFormatter(up.getTimeZone());
				SharingLink dl = new SharingLink();
				dl.setLinkType(SharingLink.LinkType.DOWNLOAD);
				dl.setStoreId(nodeId.getFolderId());
				dl.setFilePath(nodeId.getFilePath());
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
				
			} else if (crud.equals("auto")) {
				String fileId = ServletUtils.getStringParameter(request, "fileId", true);
				final StoreNodeId nodeId = new StoreNodeId(fileId);
				
				SharingLink link = null;
				Map<String, SharingLink> links = manager.listDownloadLinks(nodeId.getFolderId(), nodeId.getFilePath());
				if (links.isEmpty()) {
					SharingLink dl = new SharingLink();
					dl.setLinkType(SharingLink.LinkType.DOWNLOAD);
					dl.setStoreId(nodeId.getFolderId());
					dl.setFilePath(nodeId.getFilePath());
					dl.setAuthMode(SharingLink.AuthMode.NONE);
					dl.setNotify(false);
					link = manager.addDownloadLink(dl);
					
				} else {
					link = links.values().iterator().next();
				}
				if (link == null) throw new WTException("bla bla bla");
				
				final String servicePublicUrl = WT.getServicePublicUrl(up.getDomainId(), SERVICE_ID);
				final URI[] urls = VfsManager.generateLinkPublicURLs(servicePublicUrl, link);
				final String html = VfsManager.generateLinkEmbedCode(up.getLocale(), cus.getShortDateFormat(), servicePublicUrl, link);
				
				JsWizardData data = new JsWizardData();
				data.put("id", link.getLinkId());
				data.put("urls", URIUtils.toURIStrings(urls, false));
				data.put("embed", html);
				new JsonResult(data).printTo(out);
			}
			
		} catch (Throwable t) {
			logger.error("Error in WizardDownloadLink", t);
			new JsonResult(t).printTo(out);
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
				final StoreNodeId nodeId = new StoreNodeId(fileId);
				
				DateTimeFormatter ymdHmsFmt = DateTimeUtils.createYmdHmsFormatter(up.getTimeZone());
				SharingLink ul = new SharingLink();
				ul.setLinkType(SharingLink.LinkType.UPLOAD);
				ul.setStoreId(nodeId.getFolderId());
				ul.setFilePath(nodeId.getFilePath());
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
	
	public void processGetLinkQRCode(HttpServletRequest request, HttpServletResponse response) {
		UserProfile up = getEnv().getProfile();
		
		try {
			String linkId = ServletUtils.getStringParameter(request, "id", null);
			boolean download = ServletUtils.getBooleanParameter(request, "download", false);
			
			SharingLink link = manager.getSharingLink(linkId);
			if (linkId == null) throw new WTException("Link not found [{}]", linkId);
			
			String color = ServletUtils.getStringParameter(request, "color", "000000");
			int size = ServletUtils.getIntParameter(request, "size", 200);
			
			final String servicePublicUrl = WT.getServicePublicUrl(up.getDomainId(), SERVICE_ID);
			byte[] qrcode = VfsManager.generateLinkQRCode(servicePublicUrl, link, size, color);
			
			if (download) {
				String filename = "qrcode-" + FilenameUtils.getBaseName(PathUtils.getFileName(link.getFilePath())) + ".png";
				ServletUtils.setFileStreamHeadersForceDownload(response, filename);
				ServletUtils.writeContent(response, qrcode, qrcode.length, null); // Do not set contentType!
			} else {
				ServletUtils.writeContent(response, qrcode, qrcode.length, "image/png");
			}
			
		} catch(Throwable t) {
			logger.error("Error in GetLinkQRCode", t);
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
					for (StoreFSOrigin origin : foldersTreeCache.getOrigins()) {
						for (StoreFSFolder folder : foldersTreeCache.getFoldersByOrigin(origin)) {
							StoreNodeId folderNodeId = StoreNodeId.build(StoreNodeId.Type.FOLDER, origin.getProfileId(), folder.getFolderId());
							for(SharingLink dl : manager.listDownloadLinks(folder.getStore().getStoreId(), "/").values()) {
								items.add(new JsGridSharingLink(dl, folder.getStore().getName(), storeIcon(folder.getStore()), folderNodeId, ptz));
							}
						}
					}
					
					for (StoreFSOrigin origin : foldersTreeCache.getOrigins()) {
						for (StoreFSFolder folder : foldersTreeCache.getFoldersByOrigin(origin)) {
							StoreNodeId folderNodeId = StoreNodeId.build(StoreNodeId.Type.FOLDER, origin.getProfileId(), folder.getFolderId());
							for(SharingLink ul : manager.listUploadLinks(folder.getStore().getStoreId(), "/").values()) {
								items.add(new JsGridSharingLink(ul, folder.getStore().getName(), storeIcon(folder.getStore()), folderNodeId, ptz));
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
	
	public void processSaveMessageAttachment(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		try {
			String parentFileId = ServletUtils.getStringParameter(request, "fileId", true);
			String name = ServletUtils.getStringParameter(request, "name", true);
			String mailAccount = ServletUtils.getStringParameter(request, "mailAccount", true);
			String mailFolder = ServletUtils.getStringParameter(request, "mailFolder", true);
			int mailMsgId = ServletUtils.getIntParameter(request, "mailMsgId", true);
			int mailAttachId = ServletUtils.getIntParameter(request, "mailAttachId", true);
			
			final StoreNodeId parentNodeId = new StoreNodeId(parentFileId);
			final String path = StoreNodeId.Type.FOLDER.equals(parentNodeId.getType()) ? "/" : parentNodeId.getFilePath();
			
			IMailManager mailMgr = (IMailManager)WT.getServiceManager("com.sonicle.webtop.mail");
			InputStream is = null;
			try {
				//TODO: improve method signature, are all params truly needed?
				is = mailMgr.getAttachmentInputStream(mailAccount, mailFolder, mailMsgId, mailAttachId);
				manager.addStoreFileFromStream(parentNodeId.getFolderId(), path, name, is);
			} finally {
				IOUtils.closeQuietly(is);
			}
			new JsonResult(true).printTo(out);
			
		} catch (Exception ex) {
			logger.error("Error in SaveMessageAttachment", ex);
			new JsonResult(ex).printTo(out);
		}
	}
	
	private String[] buildSharingDisplayNames(StoreNodeId nodeId) throws WTException {
		String originDn = null, folderDn = null;
	
		StoreFSOrigin origin = foldersTreeCache.getOrigin(nodeId.getOriginAsProfileId());
		if (origin instanceof MyStoreFSOrigin) {
			originDn = lookupResource(VfsLocale.STORES_MY);
		} else if (origin instanceof StoreFSOrigin) {
			originDn = origin.getDisplayName();
		}
		
		if (StoreNodeId.Type.FOLDER.equals(nodeId.getType())) {
			StoreFSFolder folder = foldersTreeCache.getFolder(nodeId.getFolderId());
			folderDn = (folder != null) ? folder.getStore().getName() : String.valueOf(nodeId.getFolderId());
		}
		
		return new String[]{originDn, folderDn};
	}
	
	/*
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
	*/
	
	private class FoldersTreeCache extends AbstractFolderTreeCache<Integer, StoreFSOrigin, StoreFSFolder, Object> {
		
		@Override
		protected void internalBuildCache(AbstractFolderTreeCache.Target options) {
			UserProfileId pid = getEnv().getProfile().getId();
				
			if (AbstractFolderTreeCache.Target.ALL.equals(options) || AbstractFolderTreeCache.Target.ORIGINS.equals(options)) {
				try {
					this.internalClear(AbstractFolderTreeCache.Target.ORIGINS);
					this.origins.put(pid, new MyStoreFSOrigin(pid));
					for (StoreFSOrigin origin : manager.listIncomingStoreOrigins().values()) {
						this.origins.put(origin.getProfileId(), origin);
					}
					
				} catch (WTException ex) {
					logger.error("[FoldersTreeCache] Error updating Origins", ex);
				}
			}	
			if (AbstractFolderTreeCache.Target.ALL.equals(options) || AbstractFolderTreeCache.Target.FOLDERS.equals(options)) {
				try {
					this.internalClear(AbstractFolderTreeCache.Target.FOLDERS);
					for (StoreFSOrigin origin : this.origins.values()) {
						if (origin instanceof MyStoreFSOrigin) {
							for (Store category : manager.listMyStores().values()) {
								final MyStoreFSFolder folder = new MyStoreFSFolder(category.getStoreId(), category);
								this.folders.put(folder.getFolderId(), folder);
								this.foldersByOrigin.put(origin.getProfileId(), folder);
								this.originsByFolder.put(folder.getFolderId(), origin);
							}
						} else if (origin instanceof StoreFSOrigin) {
							for (StoreFSFolder folder : manager.listIncomingStoreFolders(origin.getProfileId()).values()) {
								// Make sure to track only folders with at least READ premission: 
								// it is ugly having in UI empty folder nodes for just manage update/delete/sharing operations.
								if (!folder.getPermissions().getFolderPermissions().has(FolderShare.FolderRight.READ)) continue;
								
								this.folders.put(folder.getFolderId(), folder);
								this.foldersByOrigin.put(origin.getProfileId(), folder);
								this.originsByFolder.put(folder.getFolderId(), origin);
							}
						}
					}
					
				} catch (WTException ex) {
					logger.error("[FoldersTreeCache] Error updating Folders", ex);
				}
			}
		}
	}
}
