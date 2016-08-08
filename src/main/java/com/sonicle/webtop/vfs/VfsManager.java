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

import com.sonicle.commons.PathUtils;
import com.sonicle.commons.db.DbUtils;
import com.sonicle.commons.time.DateTimeUtils;
import com.sonicle.commons.web.json.CompositeId;
import com.sonicle.vfs2.FileSelector;
import com.sonicle.vfs2.TypeNameComparator;
import com.sonicle.webtop.core.CoreManager;
import com.sonicle.webtop.core.app.RunContext;
import com.sonicle.webtop.core.app.WT;
import com.sonicle.webtop.core.bol.OShare;
import com.sonicle.webtop.core.bol.Owner;
import com.sonicle.webtop.core.bol.model.IncomingShareRoot;
import com.sonicle.webtop.core.bol.model.SharePermsElements;
import com.sonicle.webtop.core.bol.model.SharePermsFolder;
import com.sonicle.webtop.core.bol.model.SharePermsRoot;
import com.sonicle.webtop.core.bol.model.Sharing;
import com.sonicle.webtop.core.dal.DAOException;
import com.sonicle.webtop.core.sdk.AuthException;
import com.sonicle.webtop.core.sdk.BaseManager;
import com.sonicle.webtop.core.sdk.UserProfile;
import com.sonicle.webtop.core.sdk.WTException;
import com.sonicle.webtop.core.sdk.WTRuntimeException;
import com.sonicle.webtop.vfs.bol.OSharingLink;
import com.sonicle.webtop.vfs.bol.OStore;
import com.sonicle.webtop.vfs.bol.model.DownloadLink;
import com.sonicle.webtop.vfs.bol.model.Store;
import com.sonicle.webtop.vfs.bol.model.StoreFileType;
import com.sonicle.webtop.vfs.bol.model.StoreShareFolder;
import com.sonicle.webtop.vfs.bol.model.StoreShareRoot;
import com.sonicle.webtop.vfs.bol.model.UploadLink;
import com.sonicle.webtop.vfs.dal.SharingLinkDAO;
import com.sonicle.webtop.vfs.dal.StoreDAO;
import com.sonicle.webtop.vfs.sfs.DefaultSFS;
import com.sonicle.webtop.vfs.sfs.StoreFileSystem;
import com.sonicle.webtop.vfs.sfs.DropboxSFS;
import com.sonicle.webtop.vfs.sfs.FtpSFS;
import com.sonicle.webtop.vfs.sfs.FtpsSFS;
import com.sonicle.webtop.vfs.sfs.GoogleDriveSFS;
import com.sonicle.webtop.vfs.sfs.SftpSFS;
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
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.provider.UriParser;
import org.jooq.tools.StringUtils;
import org.slf4j.Logger;

/**
 *
 * @author malbinola
 */
public class VfsManager extends BaseManager {
	private static final Logger logger = WT.getLogger(VfsManager.class);
	private static final String GROUPNAME_STORE = "STORE";
	
	private final HashMap<Integer, UserProfile.Id> cacheOwnerByStore = new HashMap<>();
	private final Object shareCacheLock = new Object();
	private final HashMap<UserProfile.Id, String> cacheShareRootByOwner = new HashMap<>();
	private final HashMap<UserProfile.Id, String> cacheWildcardShareFolderByOwner = new HashMap<>();
	private final HashMap<Integer, String> cacheShareFolderByStore = new HashMap<>();
	
	private final HashMap<String, StoreFileSystem> storeFileSystems = new HashMap<>();
	
	public VfsManager(UserProfile.Id targetProfileId) throws WTException {
		super(targetProfileId);
		initFileSystems();
	}
	
	private StoreFileSystem createFileSystem(Store store) throws URISyntaxException {
		String uri = null;
		
		if(store.getBuiltIn()) {
			VfsSettings.MyDocumentsUriTemplateValues tpl = new VfsSettings.MyDocumentsUriTemplateValues();
			tpl.SERVICE_ID = SERVICE_ID;
			tpl.DOMAIN_ID = store.getDomainId();
			tpl.USER_ID = store.getUserId();
			
			VfsServiceSettings vus = new VfsServiceSettings(SERVICE_ID, store.getDomainId());
			uri = vus.getMyDocumentsUri(tpl);
			if(StringUtils.isBlank(uri)) {
				uri = WT.getServiceHomePath(SERVICE_ID) + "mydocuments/" + store.getUserId() + "/";
			}
			return new DefaultSFS(uri, null, true);
			
		} else {
			uri = store.getUri();
			String scheme = UriParser.extractScheme(uri);
			switch(scheme) {
				case "ftp":
					return new FtpSFS(uri, store.getParameters());
				case "sftp":
					return new SftpSFS(uri, store.getParameters());
				case "ftps":
					return new FtpsSFS(uri, store.getParameters());
				case "dropbox":
					return new DropboxSFS(uri, store.getParameters());
				case "googledrive":
					return new GoogleDriveSFS(uri, store.getParameters());
				default:
					return new DefaultSFS(uri, store.getParameters());
			}
		}
	}
	
	private void initFileSystems() throws WTException {
		synchronized(storeFileSystems) {
			
			List<Store> myStores = listStores();
			for(Store store : myStores) {
				StoreFileSystem sfs = null;
				try {
					sfs = createFileSystem(store);
				} catch(URISyntaxException ex) {
					throw new WTException(ex, "Unable to parse URI");
				}
				storeFileSystems.put(String.valueOf(store.getStoreId()), sfs);
			}
			
			List<StoreShareRoot> roots = listIncomingStoreRoots();
			for(StoreShareRoot root : roots) {
				HashMap<Integer, StoreShareFolder> folders = listIncomingStoreFolders(root.getShareId());
				for(StoreShareFolder folder : folders.values()) {
					StoreFileSystem sfs = null;
					try {
						sfs = createFileSystem(folder.getStore());
					} catch(URISyntaxException ex) {
						throw new WTException(ex, "Unable to parse URI");
					}
					storeFileSystems.put(String.valueOf(folder.getStore().getStoreId()), sfs);
				}
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
	
	private StoreFileSystem getStoreFileSystemFromCache(String key) {
		synchronized(storeFileSystems) {
			return storeFileSystems.get(key);
		}
	}
	
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
	
	public HashMap<Integer, StoreShareFolder> listIncomingStoreFolders(String rootShareId) throws WTException {
		CoreManager core = WT.getCoreManager(getTargetProfileId());
		LinkedHashMap<Integer, StoreShareFolder> folders = new LinkedHashMap<>();
		
		// Retrieves incoming folders (from sharing). This lookup already 
		// returns readable shares (we don't need to test READ permission)
		List<OShare> shares = core.listIncomingShareFolders(rootShareId, GROUPNAME_STORE);
		for(OShare share : shares) {
			UserProfile.Id ownerId = core.userUidToProfileId(share.getUserUid());
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
	
	public List<Store> listStores() throws WTException {
		return listStores(getTargetProfileId());
	}
	
	private List<Store> listStores(UserProfile.Id pid) throws WTException {
		StoreDAO dao = StoreDAO.getInstance();
		ArrayList<Store> items = new ArrayList<>();
		Connection con = null;
		
		try {
			con = WT.getConnection(SERVICE_ID);
			for(OStore store : dao.selectByDomainUser(con, pid.getDomainId(), pid.getUserId())) {
				items.add(new Store(store));
			}
			return items;
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public Store getStore(int storeId) throws WTException {
		StoreDAO dao = StoreDAO.getInstance();
		Connection con = null;
		
		try {
			checkRightsOnStoreFolder(storeId, "READ"); // Rights check!
			con = WT.getConnection(SERVICE_ID);
			return new Store(dao.selectById(con, storeId));
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public Store addStore(Store item) throws WTException {
		Connection con = null;
		
		try {
			checkRightsOnStoreRoot(item.getProfileId(), "MANAGE"); // Rights check!
			checkRightsOnStoreSchema(item.getUri()); // Rights check!
			con = WT.getConnection(SERVICE_ID, false);
			item.setBuiltIn(false);
			item = new Store(doStoreUpdate(true, con, item));
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
	
	public Store addBuiltInStore(Store item) throws WTException {
		Connection con = null;
		
		try {
			checkRightsOnStoreRoot(item.getProfileId(), "MANAGE"); // Rights check!
			checkRightsOnStoreSchema(item.getUri()); // Rights check!
			con = WT.getConnection(SERVICE_ID, false);
			item.setBuiltIn(true);
			item.setUri("file:///this/is/an/automatic/path");
			item = new Store(doStoreUpdate(true, con, item));
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
	
	public Store updateStore(Store item) throws Exception {
		Connection con = null;
		
		try {
			checkRightsOnStoreFolder(item.getStoreId(), "UPDATE"); // Rights check!
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
	
	public void deleteStore(int storeId) throws WTException {
		Connection con = null;
		
		try {
			checkRightsOnStoreFolder(storeId, "DELETE"); // Rights check!
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
	
	public StoreFileSystem getStoreFileSystem(int storeId) throws WTException {
		return getStoreFileSystemFromCache(String.valueOf(storeId));
	}
	
	public String generateStoreFileHash(int storeId, String path) {
		return DigestUtils.md5Hex(new CompositeId(storeId, path).toString());
	}
	
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
	
	public FileObject getStoreFile(int storeId, String path) throws FileSystemException, WTException {
		try {
			checkRightsOnStoreFolder(storeId, "READ"); // Rights check!
			return getTargetFileObject(storeId, path);
			
		} catch(Exception ex) {
			logger.warn("Error getting store file", ex);
			throw ex;
		}
	}
	
	public String createStoreFileFromStream(int storeId, String parentPath, String name, InputStream is) throws IOException, FileSystemException, WTException {
		FileObject tfo = null, ntfo = null;
		OutputStream os = null;
		
		try {
			checkRightsOnStoreElements(storeId, "UPDATE"); // Rights check!
			
			tfo = getTargetFileObject(storeId, parentPath);
			if(!tfo.isFolder()) throw new IllegalArgumentException("Please provide a valid parentPath");
			
			String newPath = FilenameUtils.separatorsToUnix(FilenameUtils.concat(parentPath, name));
			ntfo = getTargetFileObject(storeId, newPath);
			logger.debug("Creating store file from stream [{}, {}]", storeId, newPath);
			ntfo.createFile();
			try {
				os = ntfo.getContent().getOutputStream();
				IOUtils.copy(is, os);
			} finally {
				IOUtils.closeQuietly(os);
			}
			return newPath;
			
		} catch(Exception ex) {
			logger.warn("Error creating store file from stream", ex);
			throw ex;
		} finally {
			IOUtils.closeQuietly(tfo);
			IOUtils.closeQuietly(ntfo);
		}
	}
	
	public String createStoreFile(StoreFileType fileType, int storeId, String parentPath, String name) throws FileSystemException, WTException {
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
	
	public String renameStoreFile(int storeId, String path, String newName) throws FileSystemException, WTException {
		try {
			checkRightsOnStoreElements(storeId, "UPDATE"); // Rights check!
			
			return doRenameStoreFile(storeId, path, newName);
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		}
	}
	
	public void deleteStoreFile(int storeId, String path) throws FileSystemException, WTException {
		try {
			checkRightsOnStoreElements(storeId, "DELETE"); // Rights check!
			
			doDeleteStoreFile(storeId, path);
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		}
	}
	
	public String generateDownloadLinkId(int storeId, String path) {
		return generateLinkId(getTargetProfileId(), OSharingLink.LINK_TYPE_DOWNLOAD, storeId, path);
	}
	
	public String generateUploadLinkId(int storeId, String path) {
		return generateLinkId(getTargetProfileId(), OSharingLink.LINK_TYPE_UPLOAD, storeId, path);
	}
	
	public LinkedHashMap<String, DownloadLink> listDownloadLinks(int storeId, String path) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		LinkedHashMap<String, DownloadLink> items = new LinkedHashMap<>();
		Connection con = null;
		
		try {
			if(StringUtils.isBlank(path)) return items;
			ensureUser(); // Rights check!
			con = WT.getConnection(SERVICE_ID, false);
			
			logger.debug("path starts with {}", path);
			
			List<OSharingLink> links = dao.selectByProfileTypeStorePath(con, getTargetProfileId(), OSharingLink.LINK_TYPE_DOWNLOAD, storeId, path);
			for(OSharingLink link : links) {
				final DownloadLink dl = new DownloadLink(link);
				items.put(dl.getFileHash(), dl);
			}
			return items;
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	public LinkedHashMap<String, UploadLink> listUploadLinks(int storeId, String path) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		LinkedHashMap<String, UploadLink> items = new LinkedHashMap<>();
		Connection con = null;
		
		try {
			if(StringUtils.isBlank(path)) return items;
			ensureUser(); // Rights check!
			con = WT.getConnection(SERVICE_ID, false);
			
			List<OSharingLink> uls = dao.selectByProfileTypeStorePath(con, getTargetProfileId(), OSharingLink.LINK_TYPE_UPLOAD, storeId, path);
			for(OSharingLink link : uls) {
				final UploadLink ul = new UploadLink(link);
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
	
	public DownloadLink getDownloadLink(String linkId) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		Connection con = null;
		
		try {
			con = WT.getConnection(SERVICE_ID, false);
			OSharingLink olink = dao.selectByIdType(con, linkId, OSharingLink.LINK_TYPE_DOWNLOAD);
			if(olink == null) throw new WTException("Unable to retrieve download link [{0}]", linkId);
			
			checkRightsOnStoreElements(olink.getStoreId(), "READ"); // Rights check!
			
			return new DownloadLink(olink);
			
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
	
	public DownloadLink addDownloadLink(DownloadLink link) throws WTException {
		Connection con = null;
		
		try {
			checkRightsOnStoreElements(link.getStoreId(), "CREATE"); // Rights check!
			
			con = WT.getConnection(SERVICE_ID, false);
			link = new DownloadLink(doDownloadLinkInsert(con, link));
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
	
	public void deleteDownloadLink(String linkId) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		Connection con = null;
		
		try {
			con = WT.getConnection(SERVICE_ID, false);
			OSharingLink olink = dao.selectByIdType(con, linkId, OSharingLink.LINK_TYPE_DOWNLOAD);
			if(olink == null) throw new WTException("Unable to retrieve download link [{0}]", linkId);
			
			checkRightsOnStoreElements(olink.getStoreId(), "READ"); // Rights check!
			
			doDownloadLinkDelete(con, linkId);
			DbUtils.commitQuietly(con);
			writeLog("DOWNLOADLINK_DELETE", linkId);
			
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
	
	public DownloadLink getUploadLink(String linkId) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		Connection con = null;
		
		try {
			con = WT.getConnection(SERVICE_ID, false);
			OSharingLink olink = dao.selectByIdType(con, linkId, OSharingLink.LINK_TYPE_UPLOAD);
			if(olink == null) throw new WTException("Unable to retrieve upload link [{0}]", linkId);
			
			checkRightsOnStoreElements(olink.getStoreId(), "READ"); // Rights check!
			
			return new DownloadLink(olink);
			
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
	
	public UploadLink addUploadLink(UploadLink link) throws WTException {
		Connection con = null;
		
		try {
			checkRightsOnStoreElements(link.getStoreId(), "CREATE"); // Rights check!
			
			con = WT.getConnection(SERVICE_ID, false);
			link = new UploadLink(doUploadLinkInsert(con, link));
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
	
	public void deleteUploadLink(String linkId) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		Connection con = null;
		
		try {
			con = WT.getConnection(SERVICE_ID, false);
			OSharingLink olink = dao.selectByIdType(con, linkId, OSharingLink.LINK_TYPE_UPLOAD);
			if(olink == null) throw new WTException("Unable to retrieve upload link [{0}]", linkId);
			
			checkRightsOnStoreElements(olink.getStoreId(), "CREATE"); // Rights check!
			
			doUploadLinkDelete(con, linkId);
			DbUtils.commitQuietly(con);
			writeLog("UPLOADLINK_DELETE", linkId);
			
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
						UserProfile.Id ownerId = core.userUidToProfileId(folder.getUserUid());
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
	
	private String ownerToRootShareId(UserProfile.Id owner) {
		synchronized(shareCacheLock) {
			if(!cacheShareRootByOwner.containsKey(owner)) buildShareCache();
			return cacheShareRootByOwner.get(owner);
		}
	}
	
	private String ownerToWildcardFolderShareId(UserProfile.Id ownerPid) {
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
	
	private UserProfile.Id storeToOwner(int storeId) {
		synchronized(cacheOwnerByStore) {
			if(cacheOwnerByStore.containsKey(storeId)) {
				return cacheOwnerByStore.get(storeId);
			} else {
				try {
					UserProfile.Id owner = findStoreOwner(storeId);
					cacheOwnerByStore.put(storeId, owner);
					return owner;
				} catch(WTException ex) {
					throw new WTRuntimeException(ex.getMessage());
				}
			}
		}
	}
	
	private UserProfile.Id findStoreOwner(int storeId) throws WTException {
		Connection con = null;
		
		try {
			con = WT.getConnection(SERVICE_ID);
			StoreDAO dao = StoreDAO.getInstance();
			Owner owner = dao.selectOwnerById(con, storeId);
			if(owner == null) throw new WTException("Store not found [{0}]", storeId);
			return new UserProfile.Id(owner.getDomainId(), owner.getUserId());
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	private void checkRightsOnStoreSchema(String uri) {
		String scheme = UriParser.extractScheme(uri);
		switch(scheme) {
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
	
	private void checkRightsOnStoreRoot(UserProfile.Id ownerPid, String action) throws WTException {
		UserProfile.Id targetPid = getTargetProfileId();
		
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
		UserProfile.Id targetPid = getTargetProfileId();
		// Skip rights check if running user is resource's owner
		UserProfile.Id ownerPid = storeToOwner(storeId);
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
		UserProfile.Id targetPid = getTargetProfileId();
		
		if(RunContext.isWebTopAdmin()) return;
		// Skip rights check if running user is resource's owner
		UserProfile.Id ownerPid = storeToOwner(storeId);
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
		tpl.SERVICE_ID = SERVICE_ID;
		tpl.DOMAIN_ID = getTargetProfileId().getDomain();
		
		VfsServiceSettings vus = new VfsServiceSettings(SERVICE_ID, getTargetProfileId().getDomain());
		return PathUtils.concatPaths(vus.getStoreFileBasepath(tpl), uri.getPath());
	}
	
	private OStore doStoreUpdate(boolean insert, Connection con, Store store) throws WTException {
		StoreDAO dao = StoreDAO.getInstance();
		
		OStore item = new OStore(store);
		if(item.getDomainId() == null) item.setDomainId(getTargetProfileId().getDomainId());
		if(item.getUserId() == null) item.setUserId(getTargetProfileId().getUserId());
		
		try {
			URI uri = new URI(store.getUri());
			if(store.getBuiltIn() && uri.getScheme().equals("file")) {
				item.setUri(prependFileBasePath(uri));
			}
		} catch(URISyntaxException ex) {
			throw new WTException("Provided uri is not valid", ex);
		}
		
		if(insert) {
			item.setStoreId(dao.getSequence(con).intValue());
			dao.insert(con, item);
		} else {
			dao.update(con, item);
		}
        return item;
	}
	
	private void doStoreDelete(Connection con, int storeId) throws WTException {
		StoreDAO stdao = StoreDAO.getInstance();
		SharingLinkDAO shdao = SharingLinkDAO.getInstance();
		stdao.deleteById(con, storeId);
		shdao.deleteByStore(con, storeId);
	}
	
	private FileObject getTargetFileObject(int storeId, String path) throws FileSystemException, WTException {
		StoreFileSystem sfs = getStoreFileSystemFromCache(String.valueOf(storeId));
		if(sfs == null) throw new WTException("Unable to get store fileSystem");
		
		FileObject tfo = null;
		if(path.equals("/")) {
			tfo = sfs.getRootFileObject();
		} else {
			tfo = sfs.getRelativeFileObject(path);
		}
		if(tfo == null) throw new WTException("Cannot resolve target path");
		return tfo;
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
	
	private String generateLinkId(UserProfile.Id profileId, String linkType, int storeId, String path) {
		return DigestUtils.md5Hex(new CompositeId(profileId.getDomainId(), profileId.getUserId(), linkType, storeId, path).toString());
	}
	
	private OSharingLink doDownloadLinkInsert(Connection con, DownloadLink dl) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		UserProfile.Id pid = getTargetProfileId();
		
		dl.validate();
		OSharingLink o = new OSharingLink(dl);
		o.setSharingLinkId(generateLinkId(pid, OSharingLink.LINK_TYPE_DOWNLOAD, o.getStoreId(), o.getFilePath()));
		o.setDomainId(pid.getDomainId());
		o.setUserId(pid.getUserId());
		o.setLinkType(OSharingLink.LINK_TYPE_DOWNLOAD);
		o.setFileHash(generateStoreFileHash(o.getStoreId(), o.getFilePath()));
		o.setCreatedOn(DateTimeUtils.now());
        dao.insert(con, o);
        return o;
	}
	
	private void doDownloadLinkDelete(Connection con, String linkId) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		dao.deleteByIdType(con, linkId, OSharingLink.LINK_TYPE_DOWNLOAD);
		//TODO: cancellare collegati
	}
	
	private OSharingLink doUploadLinkInsert(Connection con, UploadLink ul) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		UserProfile.Id pid = getTargetProfileId();
		
		ul.validate();
		OSharingLink o = new OSharingLink(ul);
		o.setSharingLinkId(generateLinkId(pid, OSharingLink.LINK_TYPE_UPLOAD, o.getStoreId(), o.getFilePath()));
		o.setDomainId(pid.getDomainId());
		o.setUserId(pid.getUserId());
		o.setLinkType(OSharingLink.LINK_TYPE_UPLOAD);
		o.setFileHash(generateStoreFileHash(o.getStoreId(), o.getFilePath()));
		o.setCreatedOn(DateTimeUtils.now());
        dao.insert(con, o);
        return o;
	}
	
	private void doUploadLinkDelete(Connection con, String linkId) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		dao.deleteByIdType(con, linkId, OSharingLink.LINK_TYPE_UPLOAD);
		//TODO: cancellare collegati
	}
}
