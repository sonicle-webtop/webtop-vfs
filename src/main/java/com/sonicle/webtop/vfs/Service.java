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

import com.dropbox.core.DbxAccountInfo;
import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxRequestConfig;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.oauth2.model.Userinfoplus;
import com.sonicle.commons.LangUtils;
import com.sonicle.commons.time.DateTimeUtils;
import com.sonicle.commons.web.Crud;
import com.sonicle.commons.web.DispositionType;
import com.sonicle.commons.web.ServletUtils;
import com.sonicle.commons.web.ServletUtils.StringArray;
import com.sonicle.commons.web.json.CompositeId;
import com.sonicle.commons.web.json.JsonResult;
import com.sonicle.commons.web.json.MapItem;
import com.sonicle.commons.web.json.extjs.ExtTreeNode;
import com.sonicle.vfs2.util.DropboxApiUtils;
import com.sonicle.vfs2.util.GoogleDriveApiUtils;
import com.sonicle.vfs2.util.GoogleDriveAppInfo;
import com.sonicle.webtop.core.app.RunContext;
import com.sonicle.webtop.core.app.WT;
import com.sonicle.webtop.core.app.WebTopSession;
import com.sonicle.webtop.core.bol.js.JsWizardData;
import com.sonicle.webtop.core.bol.model.SharePermsRoot;
import com.sonicle.webtop.core.sdk.BaseService;
import com.sonicle.webtop.core.sdk.UploadException;
import com.sonicle.webtop.core.sdk.UserProfile;
import com.sonicle.webtop.core.sdk.WTException;
import com.sonicle.webtop.core.sdk.interfaces.IServiceUploadListener;
import com.sonicle.webtop.core.sdk.interfaces.IServiceUploadStreamListener;
import com.sonicle.webtop.core.servlet.ServletHelper;
import com.sonicle.webtop.vfs.bol.js.JsGridFile;
import com.sonicle.webtop.vfs.bol.model.DownloadLink;
import com.sonicle.webtop.vfs.bol.model.Store;
import com.sonicle.webtop.vfs.bol.model.StoreParamsDropbox;
import com.sonicle.webtop.vfs.bol.model.StoreParamsFtp;
import com.sonicle.webtop.vfs.bol.model.StoreParamsGoogleDrive;
import com.sonicle.webtop.vfs.bol.model.StoreShareFolder;
import com.sonicle.webtop.vfs.bol.model.StoreShareRoot;
import com.sonicle.webtop.vfs.bol.model.MyStoreFolder;
import com.sonicle.webtop.vfs.bol.model.MyStoreRoot;
import com.sonicle.webtop.vfs.bol.model.StoreFileType;
import com.sonicle.webtop.vfs.bol.model.UploadLink;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;

/**
 *
 * @author malbinola
 */
public class Service extends BaseService {
	private static final Logger logger = WT.getLogger(Service.class);
	
	private VfsManager manager = null;
	private VfsUserSettings us = null;
	
	private final LinkedHashMap<String, StoreShareRoot> roots = new LinkedHashMap<>();
	private final HashMap<String, ArrayList<StoreShareFolder>> foldersByRoot = new HashMap<>();
	private final LinkedHashMap<Integer, StoreShareFolder> folders = new LinkedHashMap<>();

	@Override
	public void initialize() throws Exception {
		UserProfile up = getEnv().getProfile();
		manager = (VfsManager)WT.getServiceManager(SERVICE_ID, up.getId());
		us = new VfsUserSettings(SERVICE_ID, up.getId());
		initShares();
		
		registerUploadListener("UploadStoreFile", new OnUploadStoreFile());
	}

	@Override
	public void cleanup() throws Exception {
		
	}
	
	@Override
	public ClientOptions returnClientOptions() {
		ClientOptions co = new ClientOptions();
		Integer maxUpload = WT.getCoreServiceSettings(SERVICE_ID).getUploadMaxFileSize();
		co.put("privateUploadMaxFileSize", LangUtils.coalesce(us.getPrivateUploadMaxFileSize(), maxUpload));
		co.put("publicUploadMaxFileSize", LangUtils.coalesce(us.getPublicUploadMaxFileSize(), maxUpload));
		return co;
	}
	
	private class OnUploadStoreFile implements IServiceUploadStreamListener {

		@Override
		public void onUpload(String context, HttpServletRequest request, WebTopSession.UploadedFile file, InputStream is, MapItem responseData) throws UploadException {
			
			
			
			
			/*
			try {
				String id = ServletUtils.getStringParameter(request, "folder", true);
				String name = ServletUtils.getStringParameter(request, "name", true);
				
				StoreNodeId nodeId = (StoreNodeId)new StoreNodeId().parse(id);
				int storeId = Integer.valueOf(nodeId.getStoreId());
				
				
				
			}
			*/
			
			
			
			
		}
	}
	
	private void initShares() throws WTException {
		synchronized(roots) {
			updateRootsCache();
			updateFoldersCache();
		}
	}
	
	private void updateRootsCache() throws WTException {
		UserProfile.Id pid = getEnv().getProfile().getId();
		synchronized(roots) {
			roots.clear();
			roots.put(MyStoreRoot.SHARE_ID, new MyStoreRoot(pid));
			for(StoreShareRoot root : manager.listIncomingStoreRoots()) {
				roots.put(root.getShareId(), root);
			}
		}
	}
	
	private void updateFoldersCache() throws WTException {
		synchronized(roots) {
			foldersByRoot.clear();
			folders.clear();
			for(StoreShareRoot root : roots.values()) {
				foldersByRoot.put(root.getShareId(), new ArrayList<StoreShareFolder>());
				if(root instanceof MyStoreRoot) {
					for(Store store : manager.listStores()) {
						MyStoreFolder fold = new MyStoreFolder(root.getShareId(), root.getOwnerProfileId(), store);
						foldersByRoot.get(root.getShareId()).add(fold);
						folders.put(store.getStoreId(), fold);
					}
				} else {
					for(StoreShareFolder fold : manager.listIncomingStoreFolders(root.getShareId()).values()) {
						foldersByRoot.get(root.getShareId()).add(fold);
						folders.put(fold.getStore().getStoreId(), fold);
					}
				}
			}
		}
	}
	
	private class StoreNodeId extends CompositeId {
		
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
	
	private ExtTreeNode createRootNode(String shareId, String pid, String rights, String text, boolean leaf, String iconClass) {
		StoreNodeId nodeId = new StoreNodeId();
		nodeId.setShareId(shareId);
		
		ExtTreeNode node = new ExtTreeNode(nodeId.toString(true), text, leaf);
		node.put("_type", "root");//JsFolderNode.TYPE_ROOT);
		node.put("_pid", pid);
		node.put("_rrights", rights);
		node.setIconClass(iconClass);
		return node;
	}
	
	private ExtTreeNode createFolderNode(StoreShareFolder folder, SharePermsRoot rootPerms) {
		Store store = folder.getStore();
		StoreNodeId nodeId = new StoreNodeId();
		nodeId.setShareId(folder.getShareId());
		nodeId.setStoreId(store.getStoreId().toString());
		
		ExtTreeNode node = new ExtTreeNode(nodeId.toString(true), store.getName(), false);
		node.put("_type", "folder");//JsFolderNode.TYPE_FOLDER);
		node.put("_pid", store.getProfileId().toString());
		node.put("_rrights", rootPerms.toString());
		node.put("_frights", folder.getPerms().toString());
		node.put("_erights", folder.getElementsPerms().toString());
		node.put("_storeId", store.getStoreId());
		
		List<String> classes = new ArrayList<>();
		if(!folder.getElementsPerms().implies("CREATE") 
				&& !folder.getElementsPerms().implies("UPDATE")
				&& !folder.getElementsPerms().implies("DELETE")) classes.add("wttasks-tree-readonly");
		node.setCls(StringUtils.join(classes, " "));
		
		//node.setIconClass("wt-palette-" + cat.getHexColor());
		
		return node;
	}
	
	private ExtTreeNode createFileNode(String shareId, int storeId, String filePath, String dLink, String uLink, FileObject fo) {
		StoreNodeId nodeId = new StoreNodeId();
		nodeId.setShareId(shareId);
		nodeId.setStoreId(String.valueOf(storeId));
		nodeId.setPath(filePath);
		
		ExtTreeNode node = new ExtTreeNode(nodeId.toString(true), fo.getName().getBaseName(), false);
		node.put("_type", "file");//JsFolderNode.TYPE_FOLDER);
		//node.put("_pid", store.getProfileId().toString());
		node.put("_storeId", storeId);
		node.put("_dLink", dLink);
		node.put("_uLink", uLink);
		//node.setIconClass("wt-palette-" + cat.getHexColor());
		
		return node;
	}
	
	public void processManageStoresTree(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		ArrayList<ExtTreeNode> children = new ArrayList<>();
		ExtTreeNode child = null;
		
		// Node ID is composed in this way:
		//	$shareId|$storeId|$path
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.READ)) {
				String node = ServletUtils.getStringParameter(request, "node", true);
				
				if(node.equals("root")) { // Share roots...
					for(StoreShareRoot root : roots.values()) {
						children.add(createRootNode(root));
					}
				} else {
					StoreNodeId nodeId = (StoreNodeId)new StoreNodeId().parse(node);
					if(nodeId.getSize() == 1) { // Root share's folders...
						StoreShareRoot root = roots.get(nodeId.getShareId());
						if(root instanceof MyStoreRoot) {
							for(Store cal : manager.listStores()) {
								MyStoreFolder folder = new MyStoreFolder(node, root.getOwnerProfileId(), cal);
								children.add(createFolderNode(folder, root.getPerms()));
							}
						} else {
							if(foldersByRoot.containsKey(root.getShareId())) {
								for(StoreShareFolder fold : foldersByRoot.get(root.getShareId())) {
									children.add(createFolderNode(fold, root.getPerms()));
								}
							}
						}

					} else if(nodeId.getSize() == 2 || nodeId.getSize() == 3) { // Store's folders (2) or folder's folders (3)...
						int storeId = Integer.valueOf(nodeId.getStoreId());
						String path = (nodeId.getSize() == 2) ? "/" : nodeId.getPath();
						
						LinkedHashMap<String, DownloadLink> dls = manager.listDownloadLinks(storeId, path);
						LinkedHashMap<String, UploadLink> uls = manager.listUploadLinks(storeId, path);

						for(FileObject fo : manager.listStoreFiles(StoreFileType.FOLDER, storeId, path)) {
							final String filePath = fo.getName().getPath();
							//final String fileId = new StoreNodeId(nodeId.getShareId(), nodeId.getStoreId(), filePath).toString();
							final String fileHash = manager.generateStoreFileHash(storeId, filePath);
							
							String dLink = null, uLink = null;
							if(dls.containsKey(fileHash)) {
								dLink = dls.get(fileHash).getLinkId();
							}
							if(uls.containsKey(fileHash)) {
								uLink = uls.get(fileHash).getLinkId();
							}
							children.add(createFileNode(nodeId.getShareId(), storeId, filePath, dLink, uLink, fo));
						}
						
						
					}/* else if(nodeId.getSize() == 2) { // Store's folders...
						int storeId = Integer.valueOf(nodeId.getStoreId());
						for(FileObject file : manager.listStoreFiles(StoreFileType.FOLDER, storeId, "/")) {
							children.add(createFileNode(nodeId.getShareId(), storeId, file));
						}
						
						//TODO: listare file(cartelle) dello store
					} else if(nodeId.getSize() == 3) { // Folder's folders...
						int storeId = Integer.valueOf(nodeId.getStoreId());
						String path = nodeId.getPath();
						for(FileObject file : manager.listStoreFiles(StoreFileType.FOLDER, storeId, path)) {
							children.add(createFileNode(nodeId.getShareId(), storeId, file));
						}
						
						//TODO: listare file(cartelle) della cartella
					}*/
				}
				
				new JsonResult("children", children).printTo(out);
				
			} else if(crud.equals(Crud.UPDATE)) {
				/*
				PayloadAsList<JsFolderNodeList> pl = ServletUtils.getPayloadAsList(request, JsFolderNodeList.class);
				
				for(JsFolderNode node : pl.data) {
					if(node._type.equals(JsFolderNode.TYPE_ROOT)) {
						toggleCheckedRoot(node.id, node._visible);
						
					} else if(node._type.equals(JsFolderNode.TYPE_FOLDER)) {
						CompositeId cid = new CompositeId().parse(node.id);
						toggleCheckedFolder(Integer.valueOf(cid.getToken(1)), node._visible);
					}
				}
				new JsonResult().printTo(out);
				*/
				
			} else if(crud.equals(Crud.DELETE)) {
				/*
				PayloadAsList<JsFolderNodeList> pl = ServletUtils.getPayloadAsList(request, JsFolderNodeList.class);
				
				for(JsFolderNode node : pl.data) {
					if(node._type.equals(JsFolderNode.TYPE_FOLDER)) {
						CompositeId cid = new CompositeId().parse(node.id);
						manager.deleteCategory(Integer.valueOf(cid.getToken(1)));
					}
				}
				new JsonResult().printTo(out);
				*/
			}
			
		} catch(Exception ex) {
			logger.error("Error in ManageStoresTree", ex);
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
				String path = (parentNodeId.getSize() == 2) ? "/" : parentNodeId.getPath();
				
				LinkedHashMap<String, DownloadLink> dls = manager.listDownloadLinks(storeId, path);
				LinkedHashMap<String, UploadLink> uls = manager.listUploadLinks(storeId, path);
				
				for(FileObject fo : manager.listStoreFiles(StoreFileType.FILE_OR_FOLDER, storeId, path)) {
					final String filePath = fo.getName().getPath();
					final String fileId = new StoreNodeId(parentNodeId.getShareId(), parentNodeId.getStoreId(), filePath).toString();
					final String fileHash = manager.generateStoreFileHash(storeId, filePath);
					
					String dLink = null, uLink = null;
					if(dls.containsKey(fileHash)) {
						dLink = dls.get(fileHash).getLinkId();
					}
					if(uls.containsKey(fileHash)) {
						uLink = uls.get(fileHash).getLinkId();
					}
					items.add(new JsGridFile(fo, fileId, dLink, uLink));
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
			if(crud.equals("rename")) {
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
			String fileId = ServletUtils.getStringParameter(request, "fileId", true);
			
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
				DownloadLink dl = new DownloadLink();
				dl.setStoreId(storeId);
				dl.setFilePath(nodeId.getPath());
				if(!StringUtils.isBlank(expirationDate)) dl.setExpiresOn(ymdHmsFmt.parseDateTime(expirationDate));
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
	
	public void processManageDownloadLink(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.DELETE)) {
				String linkId = ServletUtils.getStringParameter(request, "id", true);
				
				manager.deleteDownloadLink(linkId);
				new JsonResult().printTo(out);
			}
			
		} catch (Exception ex) {
			logger.error("Error in ManageDownloadLink", ex);
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
				UploadLink ul = new UploadLink();
				ul.setStoreId(storeId);
				ul.setFilePath(nodeId.getPath());
				if(!StringUtils.isBlank(expirationDate)) ul.setExpiresOn(ymdHmsFmt.parseDateTime(expirationDate));
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
	
	public void processManageUploadLink(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals(Crud.DELETE)) {
				String linkId = ServletUtils.getStringParameter(request, "id", true);
				
				manager.deleteUploadLink(linkId);
				new JsonResult().printTo(out);
			}
			
		} catch (Exception ex) {
			logger.error("Error in ManageUploadLink", ex);
			new JsonResult(false, ex.getMessage()).printTo(out);
		}
	}
	
	public void processSetupStoreFtp(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		WebTopSession wts = RunContext.getWebTopSession();
		String PROPERTY = "SETUP_FTP";
		
		try {
			String crud = ServletUtils.getStringParameter(request, "crud", true);
			if(crud.equals("s1")) {
				String schema = ServletUtils.getStringParameter(request, "schema", true);
				String host = ServletUtils.getStringParameter(request, "host", true);
				Integer port = ServletUtils.getIntParameter(request, "port", null);
				String username = ServletUtils.getStringParameter(request, "username", true);
				String password = ServletUtils.getStringParameter(request, "password", null);
				String path = ServletUtils.getStringParameter(request, "path", null);
				
				StoreParamsFtp ftpparams = new StoreParamsFtp();
				ftpparams.schema = schema;
				ftpparams.host = host;
				ftpparams.port = port;
				ftpparams.username = username;
				ftpparams.password = password;
				ftpparams.path = path;
				ftpparams.buildName();
				wts.setProperty(PROPERTY, ftpparams);
				//TODO: controllo connessione
				
				new JsonResult(ftpparams).printTo(out);
				
			} else if(crud.equals("s2")) {
				String name = ServletUtils.getStringParameter(request, "name", true);
				if(!wts.hasProperty(PROPERTY)) throw new WTException();
				StoreParamsFtp ftpparams = (StoreParamsFtp) wts.getProperty(PROPERTY);
				
				Store store = new Store();
				store.setName(StringUtils.defaultIfBlank(name, ftpparams.name));
				store.setUri(ftpparams.generateURI());
				manager.addStore(store);
				wts.clearProperty(PROPERTY);
				
				new JsonResult().printTo(out);
			}
			
		} catch (Exception ex) {
			logger.error("Error in SetupFtp", ex);
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
				StoreParamsDropbox dbxparams = new StoreParamsDropbox();
				dbxparams.authUrl = DropboxApiUtils.getAuthorizationUrl(APP_NAME, DROPBOX_USER_LOCALE, DROPBOX_APP_KEY, DROPBOX_APP_SECRET);
				wts.setProperty(PROPERTY, dbxparams);
				
				new JsonResult(dbxparams).printTo(out);
				
			} else if(crud.equals("s2")) {
				String code = ServletUtils.getStringParameter(request, "code", true);
				if(!wts.hasProperty(PROPERTY)) throw new WTException();
				StoreParamsDropbox dbxparams = (StoreParamsDropbox) wts.getProperty(PROPERTY);
				
				DbxAppInfo appInfo = DropboxApiUtils.createAppInfo(DROPBOX_APP_KEY, DROPBOX_APP_SECRET);
				DbxRequestConfig reqConfig = DropboxApiUtils.createRequestConfig(APP_NAME, DROPBOX_USER_LOCALE);
				DbxAuthFinish auth = DropboxApiUtils.exchangeAuthorizationCode(code, reqConfig, appInfo);
				DbxAccountInfo ai = DropboxApiUtils.getAccountInfo(auth.accessToken, reqConfig);
				dbxparams.accountId = String.valueOf(ai.userId);
				dbxparams.accountName = ai.displayName;
				dbxparams.accessToken = auth.accessToken;
				dbxparams.buildName();
				
				new JsonResult(dbxparams).printTo(out);
				
			} else if(crud.equals("s3")) {
				String name = ServletUtils.getStringParameter(request, "name", true);
				if(!wts.hasProperty(PROPERTY)) throw new WTException();
				StoreParamsDropbox dbxparams = (StoreParamsDropbox) wts.getProperty(PROPERTY);
				
				Store store = new Store();
				store.setName(StringUtils.defaultIfBlank(name, dbxparams.name));
				store.setUri(dbxparams.generateURI());
				store.setParameters(LangUtils.serialize(dbxparams, StoreParamsDropbox.class));
				manager.addStore(store);
				wts.clearProperty(PROPERTY);
				
				new JsonResult().printTo(out);
			}
			
		} catch (Exception ex) {
			logger.error("Error in SetupDropbox", ex);
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
				GoogleDriveAppInfo appInfo = new GoogleDriveAppInfo(APP_NAME, GDRIVE_CLIENT_ID, GDRIVE_CLIENT_SECRET);
				StoreParamsGoogleDrive gdparams = new StoreParamsGoogleDrive();
				gdparams.authUrl = GoogleDriveApiUtils.getAuthorizationUrl(appInfo);
				wts.setProperty(PROPERTY, gdparams);
				
				new JsonResult(gdparams).printTo(out);
				
			} else if(crud.equals("s2")) {
				String code = ServletUtils.getStringParameter(request, "code", true);
				if(!wts.hasProperty(PROPERTY)) throw new WTException();
				StoreParamsGoogleDrive gdparams = (StoreParamsGoogleDrive) wts.getProperty(PROPERTY);
				
				GoogleDriveAppInfo appInfo = new GoogleDriveAppInfo(APP_NAME, GDRIVE_CLIENT_ID, GDRIVE_CLIENT_SECRET);
				GoogleCredential cred = GoogleDriveApiUtils.exchangeAuthorizationCode(code, appInfo);
				gdparams.refreshToken = cred.getRefreshToken();
				gdparams.accessToken = cred.getAccessToken();
				Userinfoplus uip = GoogleDriveApiUtils.getUserInfo(gdparams.accessToken, appInfo);
				gdparams.accountEmail = uip.getEmail();
				gdparams.accountName = uip.getName();
				gdparams.buildName();
				
				new JsonResult(gdparams).printTo(out);
				
			} else if(crud.equals("s3")) {
				String name = ServletUtils.getStringParameter(request, "name", true);
				if(!wts.hasProperty(PROPERTY)) throw new WTException();
				StoreParamsGoogleDrive gdparams = (StoreParamsGoogleDrive) wts.getProperty(PROPERTY);
				
				Store store = new Store();
				store.setName(StringUtils.defaultIfBlank(name, gdparams.name));
				store.setUri(gdparams.generateURI());
				store.setParameters(LangUtils.serialize(gdparams, StoreParamsGoogleDrive.class));
				manager.addStore(store);
				
				new JsonResult().printTo(out);
			}
			
		} catch (Exception ex) {
			logger.error("Error in SetupGoogleDrive", ex);
			new JsonResult(false, ex.getMessage()).printTo(out);
		}
	}
}
