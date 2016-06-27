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

import com.sonicle.commons.db.DbUtils;
import com.sonicle.commons.time.DateTimeUtils;
import com.sonicle.commons.web.json.CompositeId;
import com.sonicle.vfs2.FileSelector;
import com.sonicle.vfs2.FolderSelector;
import com.sonicle.vfs2.NameComparator;
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
import com.sonicle.webtop.vfs.bol.model.StoreShareFolder;
import com.sonicle.webtop.vfs.bol.model.StoreShareRoot;
import com.sonicle.webtop.vfs.bol.model.UploadLink;
import com.sonicle.webtop.vfs.dal.SharingLinkDAO;
import com.sonicle.webtop.vfs.dal.StoreDAO;
import com.sonicle.webtop.vfs.dfs.StoreFileSystem;
import com.sonicle.webtop.vfs.dfs.DropboxSFS;
import com.sonicle.webtop.vfs.dfs.FtpSFS;
import com.sonicle.webtop.vfs.dfs.FtpsSFS;
import com.sonicle.webtop.vfs.dfs.GoogleDriveSFS;
import com.sonicle.webtop.vfs.dfs.SftpSFS;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeSet;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.vfs2.FileDepthSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.FileTypeSelector;
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
	private static final String RESOURCE_STORE = "STORE";
	
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
	
	private StoreFileSystem createFileSystem(Store store) {
		StoreFileSystem sfs = null;
		String uri = store.getUri();
		String scheme = UriParser.extractScheme(uri);
		if(scheme.equals("ftp")) {
			sfs = new FtpSFS(uri, store.getParameters());
		} else if(scheme.equals("sftp")) {
			sfs = new SftpSFS(uri, store.getParameters());
		} else if(scheme.equals("ftps")) {
			sfs = new FtpsSFS(uri, store.getParameters());
		} else if(scheme.equals("dropbox")) {
			sfs = new DropboxSFS(uri, store.getParameters());
		} else if(scheme.equals("googledrive")) {
			sfs = new GoogleDriveSFS(uri, store.getParameters());
		}
		return sfs;
	}
	
	private void initFileSystems() throws WTException {
		synchronized(storeFileSystems) {
			
			List<Store> myStores = listStores();
			for(Store store : myStores) {
				StoreFileSystem sfs = createFileSystem(store);
				storeFileSystems.put(String.valueOf(store.getStoreId()), sfs);
			}
			
			List<StoreShareRoot> roots = listIncomingStoreRoots();
			for(StoreShareRoot root : roots) {
				HashMap<Integer, StoreShareFolder> folders = listIncomingStoreFolders(root.getShareId());
				for(StoreShareFolder folder : folders.values()) {
					StoreFileSystem sfs = createFileSystem(folder.getStore());
					storeFileSystems.put(String.valueOf(folder.getStore().getStoreId()), sfs);
				}
			}
		}
	}
	
	private StoreFileSystem doGetFileSystem(String key) {
		synchronized(storeFileSystems) {
			return storeFileSystems.get(key);
		}
	}
	
	public StoreFileSystem getStoreFileSystem(int storeId) throws WTException {
		return doGetFileSystem(String.valueOf(storeId));
	}
	
	private FileObject getTargetFileObject(int storeId, String path) throws FileSystemException, WTException {
		StoreFileSystem sfs = doGetFileSystem(String.valueOf(storeId));
		if(sfs == null) throw new WTException("Unable to get store fileSystem");
		
		FileObject tfo = null;
		if(path == null) {
			tfo = sfs.getRootFileObject();
		} else {
			tfo = sfs.getRootFileObject().resolveFile(path);
		}
		if(tfo == null) throw new WTException("Cannot resolve target path");
		return tfo;
	}
	
	public FileObject[] listStoreFolders(int storeId) throws FileSystemException, WTException {
		return listStoreFolders(storeId, null);
	}
	
	public FileObject[] listStoreFolders(int storeId, String path) throws FileSystemException, WTException {
		FileObject tfo = getTargetFileObject(storeId, path);
		FileObject[] fos = tfo.findFiles(new FileSelector(true, false));
		Arrays.sort(fos, new NameComparator());
		return fos;
	}
	
	public FileObject[] listStoreFiles(int storeId, String path) throws FileSystemException, WTException {
		FileObject tfo = getTargetFileObject(storeId, path);
		FileObject[] fos = tfo.findFiles(new FileSelector(false, true));
		Arrays.sort(fos, new TypeNameComparator());
		return fos;
	}
	
	public FileObject[] listStoreFileObjects(int storeId, String path) throws FileSystemException, WTException {
		FileObject tfo = getTargetFileObject(storeId, path);
		FileObject[] fos = tfo.findFiles(new FileSelector(true, true));
		Arrays.sort(fos, new TypeNameComparator());
		return fos;
	}
	
	public String generateStoreFileHash(int storeId, String path) {
		return DigestUtils.md5Hex(new CompositeId(storeId, path).toString());
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
			ensureUser(); // Rights check!
			con = WT.getConnection(SERVICE_ID, false);
			
			logger.debug("path starts with {}", path);
			
			List<OSharingLink> links = dao.selectByTypeProfileStorePath(con, OSharingLink.LINK_TYPE_DOWNLOAD, getTargetProfileId(), storeId, path);
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
			ensureUser(); // Rights check!
			con = WT.getConnection(SERVICE_ID, false);
			
			List<OSharingLink> uls = dao.selectByTypeProfileStorePath(con, OSharingLink.LINK_TYPE_UPLOAD, getTargetProfileId(), storeId, path);
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
	
	public DownloadLink addDownloadLink(DownloadLink item) throws WTException {
		Connection con = null;
		
		try {
			ensureUser(); // Rights check!
			con = WT.getConnection(SERVICE_ID, false);
			item = new DownloadLink(doDownloadLinkInsert(con, item));
			DbUtils.commitQuietly(con);
			writeLog("DOWNLOADLINK_INSERT", item.getLinkId());
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
	
	public UploadLink addUploadLink(UploadLink item) throws WTException {
		Connection con = null;
		
		try {
			ensureUser(); // Rights check!
			con = WT.getConnection(SERVICE_ID, false);
			item = new UploadLink(doUploadLinkInsert(con, item));
			DbUtils.commitQuietly(con);
			writeLog("UPLOADLINK_INSERT", item.getLinkId());
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
	
	public void deleteDownloadLink(String linkId) throws WTException {
		Connection con = null;
		
		try {
			//checkRightsOnStoreFolder(linkId, "DELETE"); // Rights check!
			con = WT.getConnection(SERVICE_ID, false);
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
	
	public void deleteUploadLink(String linkId) throws WTException {
		Connection con = null;
		
		try {
			//checkRightsOnStoreFolder(linkId, "DELETE"); // Rights check!
			con = WT.getConnection(SERVICE_ID, false);
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
	
	private String generateLinkId(UserProfile.Id profileId, String linkType, int storeId, String path) {
		return DigestUtils.md5Hex(new CompositeId(profileId.getDomainId(), profileId.getUserId(), linkType, storeId, path).toString());
	}
	
	private OSharingLink doDownloadLinkInsert(Connection con, DownloadLink dl) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		UserProfile.Id pid = getTargetProfileId();
		
		dl.validate();
		OSharingLink o = new OSharingLink(dl);
		o.setSharingLinkId(generateLinkId(pid, o.getLinkType(), o.getStoreId(), o.getFilePath()));
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
		o.setSharingLinkId(generateLinkId(pid, o.getLinkType(), o.getStoreId(), o.getFilePath()));
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
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	public List<StoreShareRoot> listIncomingStoreRoots() throws WTException {
		CoreManager core = WT.getCoreManager(getTargetProfileId());
		ArrayList<StoreShareRoot> roots = new ArrayList();
		HashSet<String> hs = new HashSet<>();
		
		List<IncomingShareRoot> shares = core.listIncomingShareRoots(SERVICE_ID, RESOURCE_STORE);
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
		List<OShare> shares = core.listIncomingShareFolders(rootShareId, RESOURCE_STORE);
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
			ensureUser(); // Rights check!
			con = WT.getConnection(SERVICE_ID, false);
			item = new Store(doStoreInsert(con, item));
			DbUtils.commitQuietly(con);
			writeLog("STORE_INSERT", item.getStoreId().toString());
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
			doStoreUpdate(con, item);
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
				for(OShare folder : core.listIncomingShareFolders(root.getShareId(), RESOURCE_STORE)) {
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
		
		throw new AuthException("Action not allowed on folder share [{0}, {1}, {2}, {3}]", shareId, action, RESOURCE_STORE, targetPid.toString());
	}
	
	private OStore doStoreInsert(Connection con, Store store) throws WTException {
		StoreDAO dao = StoreDAO.getInstance();
		OStore item = new OStore(store);
		if(item.getDomainId() == null) item.setDomainId(getTargetProfileId().getDomainId());
		if(item.getUserId() == null) item.setUserId(getTargetProfileId().getUserId());
        item.setStoreId(dao.getSequence(con).intValue());
        dao.insert(con, item);
        return item;
	}
	
	private void doStoreUpdate(Connection con, Store store) throws WTException {
		StoreDAO dao = StoreDAO.getInstance();
		OStore item = new OStore(store);
        dao.update(con, item);
	}
	
	private void doStoreDelete(Connection con, int storeId) throws WTException {
		StoreDAO dao = StoreDAO.getInstance();
		dao.deleteById(con, storeId);
		//TODO: cancellare collegati
	}
	
	
	
	
	public static <T,F> T copyProperties___(T to, F from) throws WTException {
		try {
			BeanUtils.copyProperties(to, from);
			return to;
		} catch (IllegalAccessException | InvocationTargetException ex) {
			throw new WTException(ex, "Error copying bean");
		}
	}
}
