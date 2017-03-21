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
import com.sonicle.commons.db.DbUtils;
import com.sonicle.commons.time.DateTimeUtils;
import com.sonicle.commons.web.json.CompositeId;
import com.sonicle.vfs2.FileSelector;
import com.sonicle.vfs2.TypeNameComparator;
import com.sonicle.vfs2.VfsURI;
import com.sonicle.webtop.core.CoreManager;
import com.sonicle.webtop.core.CoreServiceSettings;
import com.sonicle.webtop.core.app.CoreManifest;
import com.sonicle.webtop.core.app.RunContext;
import com.sonicle.webtop.core.app.WT;
import com.sonicle.webtop.core.bol.OShare;
import com.sonicle.webtop.core.bol.Owner;
import com.sonicle.webtop.core.model.IncomingShareRoot;
import com.sonicle.webtop.core.model.SharePermsElements;
import com.sonicle.webtop.core.model.SharePermsFolder;
import com.sonicle.webtop.core.model.SharePermsRoot;
import com.sonicle.webtop.core.bol.model.Sharing;
import com.sonicle.webtop.core.dal.DAOException;
import com.sonicle.webtop.core.sdk.AuthException;
import com.sonicle.webtop.core.sdk.BaseManager;
import com.sonicle.webtop.core.sdk.UserProfile;
import com.sonicle.webtop.core.sdk.UserProfileId;
import com.sonicle.webtop.core.sdk.WTException;
import com.sonicle.webtop.core.sdk.WTRuntimeException;
import com.sonicle.webtop.core.util.NotificationHelper;
import com.sonicle.webtop.vfs.bol.OSharingLink;
import com.sonicle.webtop.vfs.bol.OStore;
import com.sonicle.webtop.vfs.model.SharingLink;
import com.sonicle.webtop.vfs.model.StoreFileType;
import com.sonicle.webtop.vfs.model.StoreShareFolder;
import com.sonicle.webtop.vfs.model.StoreShareRoot;
import com.sonicle.webtop.vfs.dal.SharingLinkDAO;
import com.sonicle.webtop.vfs.dal.StoreDAO;
import com.sonicle.webtop.vfs.model.Store;
import com.sonicle.webtop.vfs.sfs.DefaultSFS;
import com.sonicle.webtop.vfs.sfs.StoreFileSystem;
import com.sonicle.webtop.vfs.sfs.DropboxSFS;
import com.sonicle.webtop.vfs.sfs.FtpSFS;
import com.sonicle.webtop.vfs.sfs.FtpsSFS;
import com.sonicle.webtop.vfs.sfs.GoogleDriveSFS;
import com.sonicle.webtop.vfs.sfs.SftpSFS;
import freemarker.template.TemplateException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import javax.mail.internet.InternetAddress;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.Selectors;
import org.slf4j.Logger;

/**
 *
 * @author malbinola
 */
public class VfsManager extends BaseManager implements IVfsManager {
	private static final Logger logger = WT.getLogger(VfsManager.class);
	private static final String GROUPNAME_STORE = "STORE";
	private static final String MYDOCUMENTS_FOLDER = "mydocuments";
	private static final String MYDOCUMENTS_URI_SCHEME = "mydocs";
	private static final String IMAGES_URI_SCHEME = "images";
	
	private final HashMap<Integer, UserProfileId> cacheOwnerByStore = new HashMap<>();
	private final Object shareCacheLock = new Object();
	private final HashMap<UserProfileId, String> cacheShareRootByOwner = new HashMap<>();
	private final HashMap<UserProfileId, String> cacheWildcardShareFolderByOwner = new HashMap<>();
	private final HashMap<Integer, String> cacheShareFolderByStore = new HashMap<>();
	
	private final HashMap<String, StoreFileSystem> storeFileSystems = new HashMap<>();
	
	public VfsManager(boolean fastInit, UserProfileId targetProfileId) throws WTException {
		super(fastInit, targetProfileId);
		if(!fastInit) {
			initMyDocuments();
			initFileSystems();
		}
	}
	
	private synchronized void initMyDocuments() throws WTException {
		File myDocsDir = new File(WT.getServiceHomePath(SERVICE_ID,getTargetProfileId()) + MYDOCUMENTS_FOLDER);
		try {
			if (!myDocsDir.exists()) myDocsDir.mkdir();
		} catch(SecurityException ex) {
			throw new WTException("Cannot create mydocuments folder [{0}]", ex, myDocsDir.toString());
		}
	}
	
	private String getMyDocumentsUserPath(UserProfileId profileId) {
		return WT.getServiceHomePath(SERVICE_ID,profileId) + MYDOCUMENTS_FOLDER + "/" + profileId.getUserId() + "/";
	}
	
	private void initFileSystems() throws WTException {
		synchronized(storeFileSystems) {
			
			List<Store> myStores = listStores();
			for(Store store : myStores) {
				addStoreFileSystemToCache(store);
			}
			
			List<StoreShareRoot> roots = listIncomingStoreRoots();
			for(StoreShareRoot root : roots) {
				HashMap<Integer, StoreShareFolder> folders = listIncomingStoreFolders(root.getShareId());
				for(StoreShareFolder folder : folders.values()) {
					addStoreFileSystemToCache(folder.getStore());
				}
			}
		}
	}
	
	private StoreFileSystem createFileSystem(Store store) throws URISyntaxException {
				
		if (store.getBuiltIn().equals(Store.BUILTIN_MYDOCUMENTS)) {
			VfsSettings.MyDocumentsUriTemplateValues tpl = new VfsSettings.MyDocumentsUriTemplateValues();
			tpl.SERVICE_HOME = WT.getServiceHomePath(SERVICE_ID,store.getProfileId());
			tpl.SERVICE_ID = SERVICE_ID;
			tpl.DOMAIN_ID = store.getDomainId();
			tpl.USER_ID = store.getUserId();
			
			URI uri = null;
			VfsServiceSettings vus = new VfsServiceSettings(SERVICE_ID, store.getDomainId());
			String suri = vus.getMyDocumentsUri(tpl);
			if (StringUtils.isBlank(suri)) {
				uri = new File(getMyDocumentsUserPath(store.getProfileId())).toURI();
				//uri = Store.buildURI("file", null, null, null, null, getMyDocumentsUserPath(store.getUserId()));
			} else {
				uri = new URI(suri);
			}
			return new DefaultSFS(store.getStoreId(), uri, null, true);
			
		} else if (store.getBuiltIn().equals(Store.BUILTIN_DOMAINIMAGES)) {
			String imagesPath = WT.getDomainImagesPath(store.getUri().getHost());
			URI uri = new File(imagesPath).toURI();
			//URI uri = Store.buildURI("file", null, null, null, null, imagesPath);
			return new DefaultSFS(store.getStoreId(), uri, null, true);
			
		} else {
			CoreServiceSettings css = new CoreServiceSettings(CoreManifest.ID, getTargetProfileId().getDomainId());
			switch(store.getUri().getScheme()) {
				case "ftp":
					return new FtpSFS(store.getStoreId(), store.getUri(), store.getParameters());
				case "sftp":
					return new SftpSFS(store.getStoreId(), store.getUri(), store.getParameters());
				case "ftps":
					return new FtpsSFS(store.getStoreId(), store.getUri(), store.getParameters());
				case "dropbox":
					return new DropboxSFS(store.getStoreId(), store.getUri(), store.getParameters(), css.getDropboxAppKey(), css.getDropboxAppSecret());
				case "googledrive":
					return new GoogleDriveSFS(store.getStoreId(), store.getUri(), store.getParameters(), css.getGoogleDriveClientID(), css.getGoogleDriveClientSecret());
				default:
					return new DefaultSFS(store.getStoreId(), store.getUri(), store.getParameters());
			}
		}
	}
	
	private void addStoreFileSystemToCache(Store store) throws WTException {
		synchronized(storeFileSystems) {
			StoreFileSystem sfs = null;
			try {
				sfs = createFileSystem(store);
			} catch(URISyntaxException ex) {
				throw new WTException(ex, "Unable to parse URI");
			}
			storeFileSystems.put(String.valueOf(store.getStoreId()), sfs);
		}
	}
	
	private void removeStoreFileSystemFromCache(int storeId) throws WTException {
		synchronized(storeFileSystems) {
			storeFileSystems.remove(String.valueOf(storeId));
		}
	}
	
	private StoreFileSystem getStoreFileSystemFromCache(int storeId) throws WTException {
		String key = String.valueOf(storeId);
		synchronized(storeFileSystems) {
			if(!storeFileSystems.containsKey(key)) {
				Store store = getStore(storeId);
				if(store == null) throw new WTException("Store not found [{0}]", storeId);
				addStoreFileSystemToCache(store);
			}
			return storeFileSystems.get(key);
		}
	}
	
	@Override
	public List<StoreShareRoot> listIncomingStoreRoots() throws WTException {
		CoreManager core = WT.getCoreManager(getTargetProfileId());
		ArrayList<StoreShareRoot> roots = new ArrayList();
		HashSet<String> hs = new HashSet<>();
		
		List<IncomingShareRoot> shares = core.listIncomingShareRoots(SERVICE_ID, GROUPNAME_STORE);
		for(IncomingShareRoot share : shares) {
			SharePermsRoot perms = core.getShareRootPermissions(share.getShareId());
			StoreShareRoot root = new StoreShareRoot(share, perms);
			if(hs.contains(root.getShareId())) continue; // Avoid duplicates ??????????????????????
			hs.add(root.getShareId());
			roots.add(root);
		}
		return roots;
	}
	
	@Override
	public HashMap<Integer, StoreShareFolder> listIncomingStoreFolders(String rootShareId) throws WTException {
		CoreManager core = WT.getCoreManager(getTargetProfileId());
		LinkedHashMap<Integer, StoreShareFolder> folders = new LinkedHashMap<>();
		
		// Retrieves incoming folders (from sharing). This lookup already 
		// returns readable shares (we don't need to test READ permission)
		List<OShare> shares = core.listIncomingShareFolders(rootShareId, GROUPNAME_STORE);
		for(OShare share : shares) {
			UserProfileId ownerId = core.userUidToProfileId(share.getUserUid());
			List<Store> stores = null;
			if(share.hasWildcard()) {
				stores = listStores(ownerId);
			} else {
				stores = Arrays.asList(getStore(Integer.valueOf(share.getInstance())));
			}
			
			for(Store store : stores) {
				SharePermsFolder fperms = core.getShareFolderPermissions(share.getShareId().toString());
				SharePermsElements eperms = core.getShareElementsPermissions(share.getShareId().toString());
				
				if(folders.containsKey(store.getStoreId())) {
					StoreShareFolder folder = folders.get(store.getStoreId());
					folder.getPerms().merge(fperms);
					folder.getElementsPerms().merge(eperms);
				} else {
					folders.put(store.getStoreId(), new StoreShareFolder(share.getShareId().toString(), ownerId, fperms, eperms, store));
				}
			}
		}
		return folders;
	}
	
	public Sharing getSharing(String shareId) throws WTException {
		CoreManager core = WT.getCoreManager();
		return core.getSharing(SERVICE_ID, GROUPNAME_STORE, shareId);
	}
	
	public void updateSharing(Sharing sharing) throws WTException {
		CoreManager core = WT.getCoreManager();
		core.updateSharing(SERVICE_ID, GROUPNAME_STORE, sharing);
	}
	
	private String buildStoreName(Locale locale, OStore store) {
		if (store.getBuiltIn().equals(Store.BUILTIN_MYDOCUMENTS)) {
			return lookupResource(locale, VfsLocale.STORE_MYDOCUMENTS);
		} else if (store.getBuiltIn().equals(Store.BUILTIN_DOMAINIMAGES)) {
			URI uri = VfsURI.parseQuietly(store.getUri());
			String domainId = (uri != null) ? uri.getHost() : "?";
			return lookupResource(locale, VfsLocale.STORE_IMAGES) + " (" + domainId + ")";
		} else {
			return store.getName();
		}
	}
	
	@Override
	public List<Store> listStores() throws WTException {
		return listStores(getTargetProfileId());
	}
	
	private List<Store> listStores(UserProfileId pid) throws WTException {
		StoreDAO dao = StoreDAO.getInstance();
		ArrayList<Store> items = new ArrayList<>();
		Connection con = null;
		
		try {
			
			con = WT.getConnection(SERVICE_ID);
			for(OStore store : dao.selectByDomainUser(con, pid.getDomainId(), pid.getUserId())) {
				items.add(createStore(store, buildStoreName(getLocale(), store)));
			}
			return items;
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} catch(URISyntaxException ex) {
			throw new WTException(ex, "Bad store URI");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public Integer getMyDocumentsStoreId() throws WTException {
		StoreDAO dao = StoreDAO.getInstance();
		Connection con = null;
		
		try {
			con = WT.getConnection(SERVICE_ID);
			
			List<Integer> oids = dao.selectIdByDomainUserBuiltIn(con, getTargetProfileId().getDomainId(), getTargetProfileId().getUserId(), Store.BUILTIN_MYDOCUMENTS);
			if (oids.isEmpty()) {
				logger.debug("MyDocuments built-in store not found");
				return null;
			}
			
			return oids.get(0);
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public Store getStore(int storeId) throws WTException {
		StoreDAO dao = StoreDAO.getInstance();
		Connection con = null;
		
		try {
			checkRightsOnStoreFolder(storeId, "READ"); // Rights check!
			con = WT.getConnection(SERVICE_ID);
			OStore ostore = dao.selectById(con, storeId);
			return createStore(ostore, buildStoreName(getLocale(), ostore));
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} catch(URISyntaxException ex) {
			throw new WTException(ex, "Bad store URI");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public Store addStore(Store item) throws WTException {
		Connection con = null;
		
		try {
			checkRightsOnStoreRoot(item.getProfileId(), "MANAGE");
			checkRightsOnStoreSchema(item.getUri());
			
			con = WT.getConnection(SERVICE_ID, false);
			item.setBuiltIn(Store.BUILTIN_NO);
			item = doStoreUpdate(true, con, item);
			DbUtils.commitQuietly(con);
			writeLog("STORE_INSERT", item.getStoreId().toString());
			addStoreFileSystemToCache(item);
			return item;
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} catch(Exception ex) {
			DbUtils.rollbackQuietly(con);
			throw ex;
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public Store addBuiltInStoreMyDocuments() throws WTException {
		StoreDAO dao = StoreDAO.getInstance();
		Connection con = null;
		
		try {
			checkRightsOnStoreRoot(getTargetProfileId(), "MANAGE");
			con = WT.getConnection(SERVICE_ID, false);
			
			List<OStore> oitems = dao.selectByDomainUserBuiltIn(con, getTargetProfileId().getDomainId(), getTargetProfileId().getUserId(), Store.BUILTIN_MYDOCUMENTS);
			if (!oitems.isEmpty()) {
				logger.debug("MyDocuments built-in store already present");
				return null;
			}
			
			Store item = new Store();
			item.setDomainId(getTargetProfileId().getDomainId());
			item.setUserId(getTargetProfileId().getUserId());
			item.setBuiltIn(Store.BUILTIN_MYDOCUMENTS);
			item.setName("");
			item.setUri(Store.buildURI(MYDOCUMENTS_URI_SCHEME, getTargetProfileId().getUserId(), null, null, null, null));
			item = doStoreUpdate(true, con, item);
			
			DbUtils.commitQuietly(con);
			writeLog("STORE_INSERT", item.getStoreId().toString());
			return item;
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} catch(URISyntaxException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "Unable to generate URI");
		} catch(Exception ex) {
			DbUtils.rollbackQuietly(con);
			throw ex;
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public Store addBuiltInStoreDomainImages(String domainId) throws WTException {
		StoreDAO dao = StoreDAO.getInstance();
		Connection con = null;
		
		try {
			if (!RunContext.isSysAdmin()) {
				ensureUserDomain(domainId);
			}
			
			checkRightsOnStoreRoot(getTargetProfileId(), "MANAGE");
			con = WT.getConnection(SERVICE_ID, false);
			
			URI uri = Store.buildURI(IMAGES_URI_SCHEME, domainId, null, null, null, null);
			
			List<OStore> oitems = dao.selectByDomainUserBuiltInUri(con, getTargetProfileId().getDomainId(), getTargetProfileId().getUserId(), Store.BUILTIN_DOMAINIMAGES, uri.toString());
			if (!oitems.isEmpty()) {
				logger.debug("Domain's images built-in store already present");
				return null;
			}
			
			Store item = new Store();
			item.setDomainId(getTargetProfileId().getDomainId());
			item.setUserId(getTargetProfileId().getUserId());
			item.setBuiltIn(Store.BUILTIN_DOMAINIMAGES);
			item.setName("");
			item.setUri(uri);
			item = doStoreUpdate(true, con, item);
			DbUtils.commitQuietly(con);
			writeLog("STORE_INSERT", item.getStoreId().toString());
			return item;
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} catch(URISyntaxException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "Unable to generate URI");
		} catch(Exception ex) {
			DbUtils.rollbackQuietly(con);
			throw ex;
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public Store updateStore(Store item) throws WTException {
		Connection con = null;
		
		try {
			checkRightsOnStoreFolder(item.getStoreId(), "UPDATE");
			con = WT.getConnection(SERVICE_ID, false);
			doStoreUpdate(false, con, item);
			DbUtils.commitQuietly(con);
			writeLog("STORE_UPDATE", String.valueOf(item.getStoreId()));
			
			return item;
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} catch(Exception ex) {
			DbUtils.rollbackQuietly(con);
			throw ex;
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public void deleteStore(int storeId) throws WTException {
		Connection con = null;
		
		try {
			checkRightsOnStoreFolder(storeId, "DELETE");
			con = WT.getConnection(SERVICE_ID, false);
			doStoreDelete(con, storeId);
			DbUtils.commitQuietly(con);
			writeLog("STORE_DELETE", String.valueOf(storeId));
			removeStoreFileSystemFromCache(storeId);
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} catch(Exception ex) {
			DbUtils.rollbackQuietly(con);
			throw ex;
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public void deleteBuiltInStoreDomainImages(String domainId) throws WTException {
		Connection con = null;
		
		try {
			if (!RunContext.isSysAdmin()) {
				ensureUserDomain(domainId);
			}
			String imagesPath = WT.getDomainImagesPath(domainId);
			
			Integer storeId = null;
			for (Store store : listStores()) {
				if (store.getBuiltIn().equals(Store.BUILTIN_DOMAINIMAGES) && StringUtils.endsWith(store.getUri().toString(), imagesPath)) {
					storeId = store.getStoreId();
					break;
				}
			}
			if (storeId != null) deleteStore(storeId);
			
		} catch(Exception ex) {
			DbUtils.rollbackQuietly(con);
			throw ex;
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public StoreFileSystem getStoreFileSystem(int storeId) throws WTException {
		return getStoreFileSystemFromCache(storeId);
	}
	
	public String generateStoreFileHash(int storeId, String path) {
		return DigestUtils.md5Hex(new CompositeId(storeId, path).toString());
	}
	
	@Override
	public FileObject[] listStoreFiles(StoreFileType fileType, int storeId, String path) throws FileSystemException, WTException {
		FileObject tfo = null;
		
		try {
			tfo = getTargetFileObject(storeId, path);
			FileSelector selector = null;
			if(fileType.equals(StoreFileType.FILE)) {
				selector = new FileSelector(false, true);
			} else if(fileType.equals(StoreFileType.FOLDER)) {
				selector = new FileSelector(true, false);
			} else {
				selector = new FileSelector(true, true);
			}
			FileObject[] fos = tfo.findFiles(selector);
			Arrays.sort(fos, new TypeNameComparator());
			return fos;
			
		} finally {
			IOUtils.closeQuietly(tfo);
		}	
	}
	
	@Override
	public FileObject getStoreFile(int storeId, String path) throws FileSystemException, WTException {
		try {
			checkRightsOnStoreFolder(storeId, "READ"); // Rights check!
			return getTargetFileObject(storeId, path);
			
		} catch(Exception ex) {
			logger.warn("Error getting store file", ex);
			throw ex;
		}
	}
	
	@Override
	public String addStoreFileFromStream(int storeId, String parentPath, String name, InputStream is) throws IOException, FileSystemException, WTException {
		return addStoreFileFromStream(storeId, parentPath, name, is, false);
	}
	
	@Override
	public String addStoreFileFromStream(int storeId, String parentPath, String name, InputStream is, boolean overwrite) throws IOException, FileSystemException, WTException {
		FileObject tfo = null;
		NewTargetFile ntf = null;
		OutputStream os = null;
		
		try {
			checkRightsOnStoreElements(storeId, "UPDATE"); // Rights check!
			
			tfo = getTargetFileObject(storeId, parentPath);
			if(!tfo.isFolder()) throw new IllegalArgumentException("Please provide a valid parentPath");
			
			ntf = getNewTargetFileObject(storeId, parentPath, name, overwrite);
			logger.debug("Creating store file from stream [{}, {}]", storeId, ntf.path);
			
			ntf.tfo.createFile();
			try {
				os = ntf.tfo.getContent().getOutputStream();
				IOUtils.copy(is, os);
			} finally {
				IOUtils.closeQuietly(os);
			}
			return ntf.path;
			
		} catch(Exception ex) {
			logger.warn("Error creating store file from stream", ex);
			throw ex;
		} finally {
			IOUtils.closeQuietly(tfo);
			if(ntf != null) IOUtils.closeQuietly(ntf.tfo);
		}
	}
	
	@Override
	public String addStoreFile(StoreFileType fileType, int storeId, String parentPath, String name) throws FileSystemException, WTException {
		FileObject tfo = null, ntfo = null;
		
		try {
			checkRightsOnStoreElements(storeId, "UPDATE"); // Rights check!
			
			tfo = getTargetFileObject(storeId, parentPath);
			if(!tfo.isFolder()) throw new IllegalArgumentException("Please provide a valid parentPath");
			
			String newPath = FilenameUtils.separatorsToUnix(FilenameUtils.concat(parentPath, name));
			ntfo = getTargetFileObject(storeId, newPath);
			logger.debug("Creating store file [{}, {}]", storeId, newPath);
			if(fileType.equals(StoreFileType.FOLDER)) {
				ntfo.createFolder();
			} else {
				ntfo.createFile();
			}
			return newPath;
			
		} catch(Exception ex) {
			logger.warn("Error creating store file", ex);
			throw ex;
		} finally {
			IOUtils.closeQuietly(tfo);
			IOUtils.closeQuietly(ntfo);
		}
	}
	
	@Override
	public String renameStoreFile(int storeId, String path, String newName) throws FileSystemException, WTException {
		try {
			checkRightsOnStoreElements(storeId, "UPDATE"); // Rights check!
			
			return doRenameStoreFile(storeId, path, newName);
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		}
	}
	
	@Override
	public void deleteStoreFile(int storeId, String path) throws FileSystemException, WTException {
		try {
			checkRightsOnStoreElements(storeId, "DELETE"); // Rights check!
			
			doDeleteStoreFile(storeId, path);
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		}
	}
	
	public String generateSharingLinkId(String type, int storeId, String path) {
		return generateLinkId(getTargetProfileId(), type, storeId, path);
	}
	
	@Override
	public LinkedHashMap<String, SharingLink> listDownloadLinks(int storeId, String path) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		LinkedHashMap<String, SharingLink> items = new LinkedHashMap<>();
		Connection con = null;
		
		try {
			if(StringUtils.isBlank(path)) return items;
			ensureUser(); // Rights check!
			con = WT.getConnection(SERVICE_ID, false);
			
			logger.debug("path starts with {}", path);
			
			List<OSharingLink> olinks = dao.selectByProfileTypeStorePath(con, getTargetProfileId(), SharingLink.TYPE_DOWNLOAD, storeId, path);
			for(OSharingLink olink : olinks) {
				final SharingLink dl = createSharingLink(olink);
				items.put(dl.getFileHash(), createSharingLink(olink));
			}
			return items;
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public LinkedHashMap<String, SharingLink> listUploadLinks(int storeId, String path) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		LinkedHashMap<String, SharingLink> items = new LinkedHashMap<>();
		Connection con = null;
		
		try {
			if(StringUtils.isBlank(path)) return items;
			ensureUser(); // Rights check!
			con = WT.getConnection(SERVICE_ID, false);
			
			List<OSharingLink> olinks = dao.selectByProfileTypeStorePath(con, getTargetProfileId(), SharingLink.TYPE_UPLOAD, storeId, path);
			for(OSharingLink olink : olinks) {
				final SharingLink ul = createSharingLink(olink);
				items.put(ul.getFileHash(), ul);
			}
			return items;
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public SharingLink getSharingLink(String linkId) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		Connection con = null;
		
		try {
			con = WT.getConnection(SERVICE_ID, false);
			OSharingLink olink = dao.selectById(con, linkId);
			if(olink == null) return null;
			
			checkRightsOnStoreElements(olink.getStoreId(), "READ"); // Rights check!
			
			return createSharingLink(olink);
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} catch(Exception ex) {
			DbUtils.rollbackQuietly(con);
			throw ex;
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public SharingLink addDownloadLink(SharingLink link) throws WTException {
		Connection con = null;
		
		try {
			checkRightsOnStoreElements(link.getStoreId(), "CREATE"); // Rights check!
			
			con = WT.getConnection(SERVICE_ID, false);
			link.setType(SharingLink.TYPE_DOWNLOAD);
			link = createSharingLink(doSharingLinkUpdate(true, con, link));
			DbUtils.commitQuietly(con);
			writeLog("DOWNLOADLINK_INSERT", link.getLinkId());
			return link;
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} catch(Exception ex) {
			DbUtils.rollbackQuietly(con);
			throw ex;
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public SharingLink addUploadLink(SharingLink link) throws WTException {
		Connection con = null;
		
		try {
			checkRightsOnStoreElements(link.getStoreId(), "CREATE"); // Rights check!
			
			con = WT.getConnection(SERVICE_ID, false);
			link.setType(SharingLink.TYPE_UPLOAD);
			link = createSharingLink(doSharingLinkUpdate(true, con, link));
			DbUtils.commitQuietly(con);
			writeLog("UPLOADLINK_INSERT", link.getLinkId());
			return link;
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} catch(Exception ex) {
			DbUtils.rollbackQuietly(con);
			throw ex;
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public void updateSharingLink(SharingLink link) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		Connection con = null;
		
		try {
			con = WT.getConnection(SERVICE_ID, false);
			OSharingLink olink = dao.selectById(con, link.getLinkId());
			if(olink == null) throw new WTException("Unable to retrieve sharing link [{0}]", link.getLinkId());
			
			checkRightsOnStoreElements(olink.getStoreId(), "READ"); // Rights check!
			
			doSharingLinkUpdate(false, con, link);
			DbUtils.commitQuietly(con);
			writeLog("SHARINGLINK_UPDATE", link.getLinkId());
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} catch(Exception ex) {
			DbUtils.rollbackQuietly(con);
			throw ex;
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public void deleteSharingLink(String linkId) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		Connection con = null;
		
		try {
			con = WT.getConnection(SERVICE_ID, false);
			OSharingLink olink = dao.selectById(con, linkId);
			if(olink == null) throw new WTException("Unable to retrieve sharing link [{0}]", linkId);
			
			checkRightsOnStoreElements(olink.getStoreId(), "READ"); // Rights check!
			
			doSharingLinkDelete(con, linkId);
			DbUtils.commitQuietly(con);
			writeLog("SHARINGLINK_DELETE", linkId);
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} catch(Exception ex) {
			DbUtils.rollbackQuietly(con);
			throw ex;
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public void notifySharingLinkUsage(String linkId, String path, String ipAddress, String userAgent) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		Connection con = null;
		
		try {
			con = WT.getConnection(SERVICE_ID);
			OSharingLink olink = dao.selectById(con, linkId);
			if(olink == null) throw new WTException("Unable to retrieve sharing link [{0}]", linkId);
			
			if(olink.getLinkType().equals(SharingLink.TYPE_DOWNLOAD)) {
				checkRightsOnStoreFolder(olink.getStoreId(), "READ"); // Rights check!
				sendLinkUsageEmail(olink, path, ipAddress, userAgent);
				
			} else if(olink.getLinkType().equals(SharingLink.TYPE_UPLOAD)) {
				checkRightsOnStoreElements(olink.getStoreId(), "UPDATE"); // Rights check!
				sendLinkUsageEmail(olink, path, ipAddress, userAgent);
			}
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} catch(Exception ex) {
			throw ex;
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	private void buildShareCache() {
		CoreManager core = WT.getCoreManager();
		
		try {
			cacheShareRootByOwner.clear();
			cacheWildcardShareFolderByOwner.clear();
			cacheShareFolderByStore.clear();
			for(StoreShareRoot root : listIncomingStoreRoots()) {
				cacheShareRootByOwner.put(root.getOwnerProfileId(), root.getShareId());
				for(OShare folder : core.listIncomingShareFolders(root.getShareId(), GROUPNAME_STORE)) {
					if(folder.hasWildcard()) {
						UserProfileId ownerId = core.userUidToProfileId(folder.getUserUid());
						cacheWildcardShareFolderByOwner.put(ownerId, folder.getShareId().toString());
					} else {
						cacheShareFolderByStore.put(Integer.valueOf(folder.getInstance()), folder.getShareId().toString());
					}
				}
			}
		} catch(WTException ex) {
			throw new WTRuntimeException(ex.getMessage());
		}
	}
	
	private String ownerToRootShareId(UserProfileId owner) {
		synchronized(shareCacheLock) {
			if(!cacheShareRootByOwner.containsKey(owner)) buildShareCache();
			return cacheShareRootByOwner.get(owner);
		}
	}
	
	private String ownerToWildcardFolderShareId(UserProfileId ownerPid) {
		synchronized(shareCacheLock) {
			if(!cacheWildcardShareFolderByOwner.containsKey(ownerPid) && cacheShareRootByOwner.isEmpty()) buildShareCache();
			return cacheWildcardShareFolderByOwner.get(ownerPid);
		}
	}
	
	private String storeToFolderShareId(int storeId) {
		synchronized(shareCacheLock) {
			if(!cacheShareFolderByStore.containsKey(storeId)) buildShareCache();
			return cacheShareFolderByStore.get(storeId);
		}
	}
	
	private UserProfileId storeToOwner(int storeId) {
		synchronized(cacheOwnerByStore) {
			if(cacheOwnerByStore.containsKey(storeId)) {
				return cacheOwnerByStore.get(storeId);
			} else {
				try {
					UserProfileId owner = findStoreOwner(storeId);
					cacheOwnerByStore.put(storeId, owner);
					return owner;
				} catch(WTException ex) {
					throw new WTRuntimeException(ex.getMessage());
				}
			}
		}
	}
	
	private UserProfileId findStoreOwner(int storeId) throws WTException {
		Connection con = null;
		
		try {
			con = WT.getConnection(SERVICE_ID);
			StoreDAO dao = StoreDAO.getInstance();
			Owner owner = dao.selectOwnerById(con, storeId);
			if(owner == null) throw new WTException("Store not found [{0}]", storeId);
			return new UserProfileId(owner.getDomainId(), owner.getUserId());
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	
	
	
	private Store createStore(OStore ostore, String newName) throws URISyntaxException {
		if (ostore == null) return null;
		Store sto = new Store();
		sto.setStoreId(ostore.getStoreId());
		sto.setDomainId(ostore.getDomainId());
		sto.setUserId(ostore.getUserId());
		sto.setBuiltIn(ostore.getBuiltIn());
		sto.setName(!StringUtils.isBlank(newName) ? newName : ostore.getName());
		sto.setUri(new URI(ostore.getUri()));
		sto.setParameters(ostore.getParameters());
		return sto;
	}
	
	private OStore createOStore(Store store) {
		if (store == null) return null;
		OStore ostore = new OStore();
		ostore.setStoreId(store.getStoreId());
		ostore.setDomainId(store.getDomainId());
		ostore.setUserId(store.getUserId());
		ostore.setBuiltIn(store.getBuiltIn());
		ostore.setName(store.getName());
		ostore.setUri(store.getUri().toString());
		ostore.setParameters(store.getParameters());
		return ostore;
	}
	
	private SharingLink createSharingLink(OSharingLink oslink) {
		if (oslink == null) return null;
		SharingLink link = new SharingLink();
		link.setLinkId(oslink.getSharingLinkId());
		link.setDomainId(oslink.getDomainId());
		link.setType(oslink.getLinkType());
		link.setUserId(oslink.getUserId());
		link.setStoreId(oslink.getStoreId());
		link.setFilePath(oslink.getFilePath());
		link.setFileHash(oslink.getFileHash());
		link.setCreatedOn(oslink.getCreatedOn());
		link.setExpiresOn(oslink.getExpiresOn());
		link.setAuthMode(oslink.getAuthMode());
		link.setPassword(oslink.getPassword());
		return link;
	}
	
	private void checkRightsOnStoreSchema(URI uri) {
		switch(uri.getScheme()) {
			case "file":
				RunContext.ensureIsPermitted(SERVICE_ID, "STORE_FILE", "CREATE");
				break;
			case "dropbox":
			case "googledrive":
				RunContext.ensureIsPermitted(SERVICE_ID, "STORE_CLOUD", "CREATE");
				break;
			default:
				RunContext.ensureIsPermitted(SERVICE_ID, "STORE_OTHER", "CREATE");
		}
	}
	
	private void checkRightsOnStoreRoot(UserProfileId ownerPid, String action) throws WTException {
		UserProfileId targetPid = getTargetProfileId();
		
		if(RunContext.isWebTopAdmin()) return;
		if(ownerPid.equals(targetPid)) return;
		
		String shareId = ownerToRootShareId(ownerPid);
		if(shareId == null) throw new WTException("ownerToRootShareId({0}) -> null", ownerPid);
		CoreManager core = WT.getCoreManager(targetPid);
		if(core.isShareRootPermitted(shareId, action)) return;
		
		throw new AuthException("Action not allowed on root share [{0}, {1}, {2}, {3}]", shareId, action, GROUPNAME_STORE, targetPid.toString());
	}
	
	private void checkRightsOnStoreFolder(int storeId, String action) throws WTException {
		if(RunContext.isWebTopAdmin()) return;
		UserProfileId targetPid = getTargetProfileId();
		// Skip rights check if running user is resource's owner
		UserProfileId ownerPid = storeToOwner(storeId);
		if(ownerPid.equals(targetPid)) return;
		
		// Checks rights on the wildcard instance (if present)
		CoreManager core = WT.getCoreManager(targetPid);
		String wildcardShareId = ownerToWildcardFolderShareId(ownerPid);
		if(wildcardShareId != null) {
			if(core.isShareFolderPermitted(wildcardShareId, action)) return;
		}
		
		// Checks rights on store instance
		String shareId = storeToFolderShareId(storeId);
		if(shareId == null) throw new WTException("storeToFolderShareId({0}) -> null", storeId);
		if(core.isShareFolderPermitted(shareId, action)) return;
		
		throw new AuthException("Action not allowed on folder share [{0}, {1}, {2}, {3}]", shareId, action, GROUPNAME_STORE, targetPid.toString());
	}
	
	private void checkRightsOnStoreElements(int storeId, String action) throws WTException {
		UserProfileId targetPid = getTargetProfileId();
		
		if(RunContext.isWebTopAdmin()) return;
		// Skip rights check if running user is resource's owner
		UserProfileId ownerPid = storeToOwner(storeId);
		if(ownerPid.equals(getTargetProfileId())) return;
		
		// Checks rights on the wildcard instance (if present)
		CoreManager core = WT.getCoreManager(targetPid);
		String wildcardShareId = ownerToWildcardFolderShareId(ownerPid);
		if(wildcardShareId != null) {
			if(core.isShareElementsPermitted(wildcardShareId, action)) return;
			//if(core.isShareElementsPermitted(SERVICE_ID, RESOURCE_CATEGORY, action, wildcardShareId)) return;
		}
		
		// Checks rights on calendar instance
		String shareId = storeToFolderShareId(storeId);
		if(shareId == null) throw new WTException("storeToLeafShareId({0}) -> null", storeId);
		if(core.isShareElementsPermitted(shareId, action)) return;
		//if(core.isShareElementsPermitted(SERVICE_ID, RESOURCE_CATEGORY, action, shareId)) return;
		
		throw new AuthException("Action not allowed on folderEls share [{0}, {1}, {2}, {3}]", shareId, action, GROUPNAME_STORE, targetPid.toString());
	}
	
	private String prependFileBasePath(URI uri) {
		VfsSettings.StoreFileBasepathTemplateValues tpl = new VfsSettings.StoreFileBasepathTemplateValues();
		
		tpl.SERVICE_HOME = WT.getServiceHomePath(SERVICE_ID,getTargetProfileId());
		tpl.SERVICE_ID = SERVICE_ID;
		tpl.DOMAIN_ID = getTargetProfileId().getDomain();
		
		VfsServiceSettings vus = new VfsServiceSettings(SERVICE_ID, getTargetProfileId().getDomain());
		return PathUtils.concatPaths(vus.getStoreFileBasepath(tpl), uri.getPath());
	}
	
	private Store doStoreUpdate(boolean insert, Connection con, Store store) throws WTException {
		StoreDAO dao = StoreDAO.getInstance();
		
		OStore ostore = createOStore(store);
		if(ostore.getDomainId() == null) ostore.setDomainId(getTargetProfileId().getDomainId());
		if(ostore.getUserId() == null) ostore.setUserId(getTargetProfileId().getUserId());
		
		try {
			URI uri = store.getUri();
			if((store.getBuiltIn() == 0) && uri.getScheme().equals("file")) {
				ostore.setUri(Store.buildURI("file", null, null, null, null, prependFileBasePath(uri)).toString());
			}
			
			if(insert) {
				ostore.setStoreId(dao.getSequence(con).intValue());
				dao.insert(con, ostore);
			} else {
				dao.update(con, ostore);
			}
			
			return createStore(ostore, buildStoreName(getLocale(), ostore));
			
		} catch(URISyntaxException ex) {
			throw new WTException("Provided URI is not valid", ex);
		}
	}
	
	private void doStoreDelete(Connection con, int storeId) throws WTException {
		StoreDAO stdao = StoreDAO.getInstance();
		SharingLinkDAO shdao = SharingLinkDAO.getInstance();
		stdao.deleteById(con, storeId);
		shdao.deleteByStore(con, storeId);
	}
	
	private FileObject getTargetFileObject(int storeId, String path) throws FileSystemException, WTException {
		StoreFileSystem sfs = getStoreFileSystemFromCache(storeId);
		if(sfs == null) throw new WTException("Unable to get store fileSystem");
		
		FileObject tfo = null;
		if(path.equals("/")) {
			tfo = sfs.getRootFileObject();
		} else {
			tfo = sfs.getDescendantFileObject(path);
		}
		if(tfo == null) throw new WTException("Cannot resolve target path [{0}]", path);
		return tfo;
	}
	
	private NewTargetFile getNewTargetFileObject(int storeId, String parentPath, String name, boolean overwrite) throws FileSystemException, WTException {
		String newPath = FilenameUtils.separatorsToUnix(FilenameUtils.concat(parentPath, name));
		
		if(overwrite) {
			return new NewTargetFile(newPath, getTargetFileObject(storeId, newPath));
		} else {
			FileObject newFo = getTargetFileObject(storeId, newPath);
			if(!newFo.exists()) {
				return new NewTargetFile(newPath, newFo);
			} else {
				String ext = FilenameUtils.getExtension(name);
				String suffix = StringUtils.isBlank(ext) ? "" : "." + ext;
				String baseName = FilenameUtils.getBaseName(name);
				int i = 0;
				do {
					i++;
					final String newName = baseName + " (" + i + ")" + suffix;
					newPath = FilenameUtils.separatorsToUnix(FilenameUtils.concat(parentPath, newName));
					newFo = getTargetFileObject(storeId, newPath);
				} while(newFo.exists());
				return new NewTargetFile(newPath, newFo);
			}
		}
	}
	
	private String doRenameStoreFile(int storeId, String path, String newName) throws FileSystemException, SQLException, DAOException, WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		FileObject tfo = null, ntfo = null;
		Connection con = null;
		
		try {
			tfo = getTargetFileObject(storeId, path);
			String newPath = FilenameUtils.separatorsToUnix(FilenameUtils.concat(FilenameUtils.getFullPath(path), newName));
			ntfo = getTargetFileObject(storeId, newPath);
			
			logger.debug("Renaming store file [{}, {} -> {}]", storeId, path, newPath);
			try {
				con = WT.getConnection(SERVICE_ID, false);
				
				dao.deleteByStorePath(con, storeId, path);
				tfo.moveTo(ntfo);
				DbUtils.commitQuietly(con);
				
			} catch(FileSystemException ex1) {
				DbUtils.rollbackQuietly(con);
				throw ex1;
			} finally {
				DbUtils.closeQuietly(con);
			}
			return newPath;
			
		} finally {
			IOUtils.closeQuietly(tfo);
			IOUtils.closeQuietly(ntfo);
		}
	}
	
	private void doDeleteStoreFile(int storeId, String path) throws FileSystemException, SQLException, DAOException, WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		FileObject tfo = null;
		Connection con = null;
		
		try {
			tfo = getTargetFileObject(storeId, path);
			
			logger.debug("Deleting store file [{}, {}]", storeId, path);
			
			try {
				con = WT.getConnection(SERVICE_ID, false);
				
				dao.deleteByStorePath(con, storeId, path);
				tfo.delete(Selectors.SELECT_ALL);
				DbUtils.commitQuietly(con);
				
			} catch(FileSystemException ex1) {
				DbUtils.rollbackQuietly(con);
				throw ex1;
			} finally {
				DbUtils.closeQuietly(con);
			}
			
		} finally {
			IOUtils.closeQuietly(tfo);
		}
	}
	
	private String generateLinkId(UserProfileId profileId, String linkType, int storeId, String path) {
		return DigestUtils.md5Hex(new CompositeId(profileId.getDomainId(), profileId.getUserId(), linkType, storeId, path).toString());
	}
	
	private OSharingLink doSharingLinkUpdate(boolean insert, Connection con, SharingLink sl) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		UserProfileId pid = getTargetProfileId();
		
		sl.validate(insert);
		
		OSharingLink o = new OSharingLink(sl);
		if(o.getDomainId() == null) o.setDomainId(pid.getDomainId());
		if(o.getUserId() == null) o.setUserId(pid.getUserId());
		
		if(insert) {
			o.setSharingLinkId(generateLinkId(pid, sl.getType(), o.getStoreId(), o.getFilePath()));
			o.setFileHash(generateStoreFileHash(o.getStoreId(), o.getFilePath()));
			o.setCreatedOn(DateTimeUtils.now());
			dao.insert(con, o);
		} else {
			dao.update(con, o);
		}
		return o;
	}
	
	/*
	private OSharingLink doSharingLinkInsert(Connection con, SharingLink sl) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		UserProfileId pid = getTargetProfileId();
		
		sl.validate();
		OSharingLink o = new OSharingLink(sl);
		o.setSharingLinkId(generateLinkId(pid, sl.getType(), o.getStoreId(), o.getFilePath()));
		o.setDomainId(pid.getDomainId());
		o.setUserId(pid.getUserId());
		o.setFileHash(generateStoreFileHash(o.getStoreId(), o.getFilePath()));
		o.setCreatedOn(DateTimeUtils.now());
        dao.insert(con, o);
        return o;
	}
	*/
	
	private void doSharingLinkDelete(Connection con, String linkId) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		dao.deleteById(con, linkId);
		//TODO: cancellare collegati
	}
	
	private void sendLinkUsageEmail(OSharingLink olink, String path, String ipAddress, String userAgent) throws WTException {
		final String BHD_KEY = (olink.getLinkType().equals(SharingLink.TYPE_DOWNLOAD)) ? VfsLocale.TPL_EMAIL_SHARINGLINKUSAGE_BODY_HEADER_DL : VfsLocale.TPL_EMAIL_SHARINGLINKUSAGE_BODY_HEADER_UL;
		UserProfileId pid = olink.getProfileId();
		
		//TODO: rendere relativa la path del file rispetto allo Store???
		try {
			UserProfile.Data ud = WT.getUserData(olink.getProfileId());
			String bodyHeader = lookupResource(ud.getLocale(), BHD_KEY);
			String source = NotificationHelper.buildSource(ud.getLocale(), SERVICE_ID);
			String subject = TplHelper.buildLinkUsageEmailSubject(ud.getLocale(), bodyHeader);
			String customBody = TplHelper.buildLinkUsageBodyTpl(ud.getLocale(), olink.getSharingLinkId(), PathUtils.getFileName(olink.getFilePath()), path, ipAddress, userAgent);
			String html = NotificationHelper.buildCustomBodyTplForNoReplay(ud.getLocale(), source, bodyHeader, customBody);

			//InternetAddress from = WT.buildDomainInternetAddress(pid.getDomainId(), "webtop-notification", null);
			//if(from == null) throw new WTException("Error building sender address");
			InternetAddress from = WT.getNotificationAddress(pid.getDomainId());
			InternetAddress to = ud.getEmail();
			if(to == null) throw new WTException("Error building destination address");
			WT.sendEmail(getMailSession(), true, from, to, subject, html);

		} catch(IOException | TemplateException ex) {
			logger.error("Unable to build email template", ex);
		} catch(Exception ex) {
			logger.error("Unable to send email", ex);
		}
	}
	
	public static String generateLinkEmbedCode(Locale locale, String dateFormat, String publicBaseUrl, SharingLink link) {
		
		try {
			String[] urls = VfsManager.generateLinkPublicURLs(publicBaseUrl, link);
			
			String url = (urls[1] != null) ? urls[1] : urls[0];
			String expiration = (link.getExpiresOn() != null) ? DateTimeUtils.createFormatter(dateFormat).print(link.getExpiresOn()) : null;
			String password = (link.getAuthMode().equals(SharingLink.AUTH_MODE_PASSWORD)) ? link.getPassword() : null;
			
			return TplHelper.buildLinkEmbedCodeTpl(locale, link.getType(), url, PathUtils.getFileName(link.getFilePath()), expiration, password);
			
		} catch(IOException | TemplateException ex) {
			logger.error("Unable to build embed template", ex);
			return null;
		}
	}
	
	public static String[] generateLinkPublicURLs(String publicBaseUrl, SharingLink link) {
		if(link.getType().equals(SharingLink.TYPE_DOWNLOAD)) {
			String url = null, durl = null;
			if(PathUtils.isFolder(link.getFilePath())) {
				url = buildLinkPublicUrl(publicBaseUrl, link, false);
				//TODO: implementare nel pubblico la gestione link diretti per le cartelle
			} else {
				//TODO: implementare nel pubblico l'anteprima dei file
				durl = buildLinkPublicUrl(publicBaseUrl, link, true);
			}
			return new String[]{url, durl};
		} else {
			String url = buildLinkPublicUrl(publicBaseUrl, link, false);
			return new String[]{url, null};
		}
	}
	
	/**
	 * Builds an URL suitable for links that point to shared file.
	 * @param publicBaseUrl The base URL up to the public servlet path (eg. http://localhost/webtop/public/cloud)
	 * @param link Shared link
	 * @param direct True to point directly to binary file (not suitable for folders)
	 * @return Generated URL
	 */
	public static String buildLinkPublicUrl(String publicBaseUrl, SharingLink link, boolean direct) {
		String s = PublicService.PUBPATH_CONTEXT_LINK + "/" + link.getLinkId() + (direct ? "?raw=1" : "");
		return PathUtils.concatPaths(publicBaseUrl, s);
	}
	
	/**
	 * Builds an URL suitable for redirecting to public file download stream.
	 * @param publicBaseUrl The base URL up to the public servlet path (eg. http://localhost/webtop/public/cloud)
	 * @param link Shared link
	 * @return Generated URL
	 */
	public static String buildLinkPublicGetUrl(String publicBaseUrl, SharingLink link) {
		String s = PublicService.PUBPATH_CONTEXT_LINK + "/" + link.getLinkId() + "/get/" + PathUtils.getFileName(link.getFilePath());
		return PathUtils.concatPaths(publicBaseUrl, s);
	}
	
	private class NewTargetFile {
		public String path;
		public FileObject tfo;
		
		public NewTargetFile(String path, FileObject tfo) {
			this.path = path;
			this.tfo = tfo;
		}
	}
}
