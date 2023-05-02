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

import com.sonicle.commons.AlgoUtils;
import com.sonicle.commons.EnumUtils;
import com.sonicle.commons.LangUtils;
import com.sonicle.commons.PathUtils;
import com.sonicle.commons.URIUtils;
import com.sonicle.commons.db.DbUtils;
import com.sonicle.commons.flags.BitFlagsEnum;
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
import com.sonicle.webtop.core.app.model.FolderShare;
import com.sonicle.webtop.core.app.model.FolderShareOriginFolders;
import com.sonicle.webtop.core.app.model.FolderSharing;
import com.sonicle.webtop.core.app.model.ShareOrigin;
import com.sonicle.webtop.core.app.sdk.AbstractFolderShareCache;
import com.sonicle.webtop.core.app.sdk.WTNotFoundException;
import com.sonicle.webtop.core.app.util.ExceptionUtils;
import com.sonicle.webtop.core.bol.Owner;
import com.sonicle.webtop.core.dal.DAOException;
import com.sonicle.webtop.core.sdk.AbstractMapCache;
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
import com.sonicle.webtop.vfs.dal.SharingLinkDAO;
import com.sonicle.webtop.vfs.dal.StoreDAO;
import com.sonicle.webtop.vfs.model.Store;
import com.sonicle.webtop.vfs.model.StoreFSFolder;
import com.sonicle.webtop.vfs.model.StoreFSOrigin;
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
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import jakarta.mail.internet.InternetAddress;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.glxn.qrgen.javase.QRCode;
import net.sf.qualitycheck.Check;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.http.client.utils.URIBuilder;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;

/**
 *
 * @author malbinola
 */
public class VfsManager extends BaseManager implements IVfsManager {
	private static final Logger logger = WT.getLogger(VfsManager.class);
	private static final String SHARE_CONTEXT_STORE = "STORE";
	private static final String MYDOCUMENTS_FOLDER = "mydocuments";
	public static final String URI_SCHEME_MYDOCUMENTS = "mydocs";
	public static final String URI_SCHEME_DOMAINIMAGES = "images";
	
	private final OwnerCache ownerCache = new OwnerCache();
	private final ShareCache shareCache = new ShareCache();
	private final HashMap<String, StoreFileSystem> storeFileSystems = new HashMap<>();
	private final ArrayList<Store> volatileStores = new ArrayList<>();
	private final HashMap<Integer, Store> volatileStoresMap=new HashMap<>();
	private int nextVolatileStoreId=-1;
	
	public VfsManager(boolean fastInit, UserProfileId targetProfileId) throws WTException {
		super(fastInit, targetProfileId);
		if (!fastInit) {
			shareCache.init();
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
			for (Store store : listMyStores().values()) {
				addStoreFileSystemToCache(store);
			}
			for (Store store : listIncomingStores().values()) {
				addStoreFileSystemToCache(store);
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
	
	private CoreManager getCoreManager() {
		return WT.getCoreManager(getTargetProfileId());
	}
	
	@Override
	public Set<FolderSharing.SubjectConfiguration> getFolderShareConfigurations(final UserProfileId originProfileId, final FolderSharing.Scope scope) throws WTException {
		CoreManager coreMgr = getCoreManager();
		return coreMgr.getFolderShareConfigurations(SERVICE_ID, SHARE_CONTEXT_STORE, originProfileId, scope);
	}
	
	@Override
	public void updateFolderShareConfigurations(final UserProfileId originProfileId, final FolderSharing.Scope scope, final Set<FolderSharing.SubjectConfiguration> configurations) throws WTException {
		CoreManager coreMgr = getCoreManager();
		coreMgr.updateFolderShareConfigurations(SERVICE_ID, SHARE_CONTEXT_STORE, originProfileId, scope, configurations);
	}
	
	@Override
	public Map<UserProfileId, StoreFSOrigin> listIncomingStoreOrigins() throws WTException {
		return shareCache.getOriginsMap();
	}
	
	@Override
	public StoreFSOrigin getIncomingStoreOriginByFolderId(final int categoryId) throws WTException {
		return shareCache.getOriginByFolderId(categoryId);
	}
	
	@Override
	public Map<Integer, StoreFSFolder> listIncomingStoreFolders(final StoreFSOrigin origin) throws WTException {
		Check.notNull(origin, "origin");
		return listIncomingStoreFolders(origin.getProfileId());
	}
	
	@Override
	public Map<Integer, StoreFSFolder> listIncomingStoreFolders(final UserProfileId originProfileId) throws WTException {
		Check.notNull(originProfileId, "originProfileId");
		CoreManager coreMgr = getCoreManager();
		LinkedHashMap<Integer, StoreFSFolder> folders = new LinkedHashMap<>();
		
		StoreFSOrigin origin = shareCache.getOrigin(originProfileId);
		if (origin != null) {
			for (Integer folderId : shareCache.getFolderIdsByOrigin(originProfileId)) {
				final Store store = getStore(folderId, false);
				if (store == null) continue;

				FolderShare.Permissions permissions = coreMgr.evaluateFolderSharePermissions(SERVICE_ID, SHARE_CONTEXT_STORE, originProfileId, FolderSharing.Scope.folder(String.valueOf(folderId)), false);
				if (permissions == null) {
					// If permissions are not defined at requested folder scope,
					// generates an empty permission object that will be filled below
					// with wildcard rights
					permissions = FolderShare.Permissions.none();
				}
				permissions.getFolderPermissions().set(origin.getWildcardPermissions().getFolderPermissions());
				permissions.getItemsPermissions().set(origin.getWildcardPermissions().getItemsPermissions());

				// Here we can have folders with no READ permission: these folders
				// will be included in cache for now, Manager's clients may filter
				// out them in downstream processing.
				// if (!permissions.getFolderPermissions().has(FolderShare.FolderRight.READ)) continue;
				folders.put(folderId, new StoreFSFolder(folderId, permissions, store));
			}
		}
		return folders;
	}
	
	@Override
	public Set<Integer> listMyStoreIds() throws WTException {
		return listStoreIds(getTargetProfileId());
	}
	
	@Override
	public Set<Integer> listIncomingStoreIds() throws WTException {
		return shareCache.getFolderIds();
	}
	
	@Override
	public Set<Integer> listIncomingStoreIds(final UserProfileId originProfile) throws WTException {
		if (originProfile == null) {
			return listIncomingStoreIds();
		} else {
			return LangUtils.asSet(shareCache.getFolderIdsByOrigin(originProfile));
		}
	}

	@Override
	public Set<Integer> listAllStoreIds() throws WTException {
		return Stream.concat(listMyStoreIds().stream(), listIncomingStoreIds().stream())
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}
	
	private Set<Integer> listStoreIds(UserProfileId pid) throws WTException {
		return listStoreIdsIn(pid, null);
	}
	
	private Set<Integer> listStoreIdsIn(UserProfileId pid, Collection<Integer> storeIds) throws WTException {
		Connection con = null;
		
		try {
			con = WT.getConnection(SERVICE_ID);
			return doListStoreIdsIn(con, pid, storeIds);
			
		} catch (Exception ex) {
			throw ExceptionUtils.wrapThrowable(ex);
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public Integer getMyDocumentsStoreId() throws WTException {
		StoreDAO stoDao = StoreDAO.getInstance();
		Connection con = null;
		
		try {
			con = WT.getConnection(SERVICE_ID);
			List<Integer> oids = stoDao.selectIdByDomainUserBuiltIn(con, getTargetProfileId().getDomainId(), getTargetProfileId().getUserId(), Store.BUILTIN_MYDOCUMENTS);
			if (oids.isEmpty()) {
				logger.debug("MyDocuments built-in store not found");
				return null;
			}
			return oids.get(0);
			
		} catch (Exception ex) {
			throw ExceptionUtils.wrapThrowable(ex);
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	private String buildStoreName(OStore store, Locale locale) {
		if (store == null) return null;
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
	public Map<Integer, Store> listMyStores() throws WTException {
		Map<Integer, Store> items = listStores(getTargetProfileId(), true);
		for (Store store : volatileStores) {
			items.put(store.getStoreId(), store);
		}
		return items;
	}
	
	private Map<Integer, Store> listStores(final UserProfileId ownerPid, final boolean evalRights) throws WTException {
		StoreDAO stoDao = StoreDAO.getInstance();
		LinkedHashMap<Integer, Store> items = new LinkedHashMap<>();
		Connection con = null;
		
		try {
			con = WT.getConnection(SERVICE_ID);
			for (OStore osto : stoDao.selectByProfile(con, ownerPid.getDomainId(), ownerPid.getUserId())) {
				if (evalRights && !quietlyCheckRightsOnStore(osto.getStoreId(), FolderShare.FolderRight.READ)) continue;
				items.put(osto.getStoreId(), ManagerUtils.createStore(osto, buildStoreName(osto, getLocale())));
			}
			return items;
			
		} catch (Exception ex) {
			throw ExceptionUtils.wrapThrowable(ex);
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public Map<Integer, Store> listIncomingStores() throws WTException {
		return listIncomingStores(null);
	}
	
	@Override
	public Map<Integer, Store> listIncomingStores(final UserProfileId owner) throws WTException {
		Set<Integer> ids = listIncomingStoreIds(owner);
		if (ids == null) return null;
		
		StoreDAO stoDao = StoreDAO.getInstance();
		LinkedHashMap<Integer, Store> items = new LinkedHashMap<>();
		Connection con = null;
		
		try {
			con = WT.getConnection(SERVICE_ID);
			for (OStore osto : stoDao.selectByDomainIn(con, getTargetProfileId().getDomainId(), ids)) {
				items.put(osto.getStoreId(), ManagerUtils.createStore(osto));
			}
			return items;
			
		} catch (Exception ex) {
			throw ExceptionUtils.wrapThrowable(ex);
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public UserProfileId getStoreOwner(final int storeId) throws WTException {
		return ownerCache.get(storeId);
	}
	
	@Override
	public boolean existStore(final int storeId) throws WTException {
		StoreDAO stoDao = StoreDAO.getInstance();
		Connection con = null;
		
		try {
			checkRightsOnStore(storeId, FolderShare.FolderRight.READ);
			con = WT.getConnection(SERVICE_ID);
			return stoDao.existsById(con, storeId);
			
		} catch (Exception ex) {
			throw ExceptionUtils.wrapThrowable(ex);
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public Store getStore(final int storeId) throws WTException {
		return getStore(storeId, true);
	}
	
	private Store getStore(final int storeId, final boolean evalRights) throws WTException {
		Connection con = null;
		
		try {
			if (evalRights) checkRightsOnStore(storeId, FolderShare.FolderRight.READ);
			con = WT.getConnection(SERVICE_ID);
			return doStoreGet(con, storeId);
			
		} catch(URISyntaxException ex) {
			throw new WTException(ex, "Bad store URI");
		} catch (Exception ex) {
			throw ExceptionUtils.wrapThrowable(ex);
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	private Store doStoreGet(Connection con, int storeId) throws DAOException, URISyntaxException {
		StoreDAO stoDao = StoreDAO.getInstance();
		OStore osto = stoDao.selectById(con, storeId);
		
		return ManagerUtils.createStore(osto, buildStoreName(osto, getLocale()));
	}
	
	@Override
	public Store addStore(final Store store) throws WTException {
		Connection con = null;
		
		try {
			checkRightsOnStoreOrigin(store.getProfileId(), "MANAGE");
			checkRightsOnStoreSchema(store.getUri());
			
			con = WT.getConnection(SERVICE_ID, false);
			store.setBuiltIn(Store.BUILTIN_NO);
			Store ret = doStoreUpdate(true, con, store);
			
			DbUtils.commitQuietly(con);
//			if (isAuditEnabled()) {
//				auditLogWrite(AuditContext.STORE, AuditAction.CREATE, ret.getStoreId(), null);
//			}
			addStoreFileSystemToCache(ret);
			
			return ret;
			
		} catch(SQLException | DAOException | WTException ex) {
			DbUtils.rollbackQuietly(con);
			throw wrapException(ex);
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public Store addBuiltInVolatileStore(Store store) throws WTException {
		Connection con = null;
		
		try {
			checkRightsOnStoreOrigin(store.getProfileId(), "MANAGE");
			checkRightsOnStoreSchema(store.getUri());
			
			con = WT.getConnection(SERVICE_ID, false);
			store.setBuiltIn(Store.BUILTIN_VOLATILE);
			store.setStoreId(nextVolatileStoreId--);
			Store ret = doStoreUpdate(true, con, store);
			
			DbUtils.commitQuietly(con);
//			if (isAuditEnabled()) {
//				auditLogWrite(AuditContext.STORE, AuditAction.CREATE, ret.getStoreId(), null);
//			}
			addStoreFileSystemToCache(ret);
			volatileStores.add(ret);
			volatileStoresMap.put(store.getStoreId(), store);
			
			return ret;
			
		} catch(SQLException | DAOException | WTException ex) {
			DbUtils.rollbackQuietly(con);
			throw wrapException(ex);
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public Store addBuiltInStoreMyDocuments() throws WTException {
		StoreDAO dao = StoreDAO.getInstance();
		Connection con = null;
		
		try {
			checkRightsOnStoreOrigin(getTargetProfileId(), "MANAGE");
			con = WT.getConnection(SERVICE_ID, false);
			
			List<OStore> oitems = dao.selectByDomainUserBuiltIn(con, getTargetProfileId().getDomainId(), getTargetProfileId().getUserId(), Store.BUILTIN_MYDOCUMENTS);
			if (!oitems.isEmpty()) {
				logger.debug("MyDocuments built-in store already present");
				return null;
			}
			
			Store store = new Store();
			store.setDomainId(getTargetProfileId().getDomainId());
			store.setUserId(getTargetProfileId().getUserId());
			store.setBuiltIn(Store.BUILTIN_MYDOCUMENTS);
			store.setName("");
			store.setProvider(Store.Provider.MYDOCUMENTS);
			store.setUri(Store.buildURI(URI_SCHEME_MYDOCUMENTS, "localhost", null, getTargetProfileId().toString(), null, null)); // Dummy URI
			Store ret = doStoreUpdate(true, con, store);
			
			DbUtils.commitQuietly(con);
//			if (isAuditEnabled()) {
//				auditLogWrite(AuditContext.STORE, AuditAction.CREATE, ret.getStoreId(), null);
//			}
			
			return ret;
			
		} catch(SQLException | DAOException | WTException ex) {
			DbUtils.rollbackQuietly(con);
			throw wrapException(ex);
		} catch(URISyntaxException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "Unable to generate URI");
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
			
			checkRightsOnStoreOrigin(getTargetProfileId(), "MANAGE");
			con = WT.getConnection(SERVICE_ID, false);
			
			URI uri = Store.buildURI(URI_SCHEME_DOMAINIMAGES, domainId, null, null, null, null);
			
			List<OStore> oitems = dao.selectByDomainUserBuiltInUri(con, getTargetProfileId().getDomainId(), getTargetProfileId().getUserId(), Store.BUILTIN_DOMAINIMAGES, uri.toString());
			if (!oitems.isEmpty()) {
				logger.debug("Domain's images built-in store already present");
				return null;
			}
			
			Store store = new Store();
			store.setDomainId(getTargetProfileId().getDomainId());
			store.setUserId(getTargetProfileId().getUserId());
			store.setBuiltIn(Store.BUILTIN_DOMAINIMAGES);
			store.setName("");
			store.setProvider(Store.Provider.DOMAINIMAGES);
			store.setUri(uri);
			Store ret = doStoreUpdate(true, con, store);
			
			DbUtils.commitQuietly(con);
//			if (isAuditEnabled()) {
//				auditLogWrite(AuditContext.STORE, AuditAction.CREATE, ret.getStoreId(), null);
//			}
			
			return ret;
			
		} catch(SQLException | DAOException | WTException ex) {
			DbUtils.rollbackQuietly(con);
			throw wrapException(ex);
		} catch(URISyntaxException ex) {
			DbUtils.rollbackQuietly(con);
			throw new WTException(ex, "Unable to generate URI");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public Store updateStore(Store store) throws WTException {
		Connection con = null;
		
		try {
			checkRightsOnStore(store.getStoreId(), FolderShare.FolderRight.UPDATE);
			con = WT.getConnection(SERVICE_ID, false);
			Store ret = doStoreUpdate(false, con, store);
			if (ret == null) throw new WTNotFoundException("Store not found [{}]", store.getStoreId());
			
			DbUtils.commitQuietly(con);
			removeStoreFileSystemFromCache(store.getStoreId());
			
//			if (isAuditEnabled()) {
//				auditLogWrite(AuditContext.STORE, AuditAction.UPDATE, ret.getStoreId(), null);
//			}
			
			return ret;
			
		} catch(SQLException | DAOException | WTException ex) {
			DbUtils.rollbackQuietly(con);
			throw wrapException(ex);
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public void deleteStore(int storeId) throws WTException {
		Connection con = null;
		
		try {
			checkRightsOnStore(storeId, FolderShare.FolderRight.DELETE);
			
			// Retrieve sharing configuration (for later)
			final UserProfileId sharingOwnerPid = getStoreOwner(storeId);
			final FolderSharing.Scope sharingScope = FolderSharing.Scope.folder(String.valueOf(storeId));
			Set<FolderSharing.SubjectConfiguration> configurations = getFolderShareConfigurations(sharingOwnerPid, sharingScope);
			
			con = WT.getConnection(SERVICE_ID, false);
			boolean ret = doStoreDelete(con, storeId);
			if (!ret) throw new WTNotFoundException("Store not found [{}]", storeId);
			
			// Cleanup sharing, if necessary
			if ((configurations != null) && !configurations.isEmpty()) {
				logger.debug("Removing {} active sharing [{}]", configurations.size(), sharingOwnerPid);
				configurations.clear();
				updateFolderShareConfigurations(sharingOwnerPid, sharingScope, configurations);
			}
			
			DbUtils.commitQuietly(con);
			removeStoreFileSystemFromCache(storeId);
			
//			if (isAuditEnabled()) {
//				auditLogWrite(AuditContext.STORE, AuditAction.DELETE, storeId, null);
//			}
			
		} catch (Exception ex) {
			DbUtils.rollbackQuietly(con);
			throw ExceptionUtils.wrapThrowable(ex);
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
			for (Store store : listMyStores().values()) {
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
	
	@Override
	public FileObject[] listStoreFiles(StoreFileType fileType, int storeId, String path) throws FileSystemException, WTException {
		FileObject tfo = null;
		
		try {
			tfo = getTargetFileObject(storeId, path);
			FileSelector selector = null;
			if (StoreFileType.FILE.equals(fileType)) {
				selector = new FileSelector(false, true);
			} else if(StoreFileType.FOLDER.equals(fileType)) {
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
			checkRightsOnStore(storeId, FolderShare.FolderRight.READ);
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
			checkRightsOnStore(storeId, FolderShare.ItemsRight.UPDATE);
			
			tfo = getTargetFileObject(storeId, parentPath);
			if (!tfo.isFolder()) throw new IllegalArgumentException("Please provide a valid parentPath [" + parentPath + "]");
			
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
			if (ntf != null) IOUtils.closeQuietly(ntf.tfo);
		}
	}
	
	public String addStoreFileFromTemplate(StoreFileTemplate fileTemplate, int storeId, String parentPath, String name, boolean overwrite) throws IOException, FileSystemException, WTException {
		final String RESOURCE_NAME = "/{0}/tpl/file/{1}.{2}";
		String tplName = null;
		URL tplUrl = null;
		
		// Firstly try to get template file specific for target locale, 
		// otherwise look for the template in english (en) locale.
		String fileExt = EnumUtils.toSerializedName(fileTemplate);
		tplName = MessageFormat.format(RESOURCE_NAME, LangUtils.packageToPath(SERVICE_ID), getLocale().getLanguage(), fileExt);
		tplUrl = this.getClass().getResource(tplName);
		if (tplUrl == null) {
			tplName = MessageFormat.format(RESOURCE_NAME, LangUtils.packageToPath(SERVICE_ID), "en", fileExt);
			tplUrl = this.getClass().getResource(tplName);
		}
		if (tplUrl == null) throw new WTException("Template file not found [{}]", tplName);
		String sntzName = PathUtils.sanitizeFolderName(name);
		String fileName = StringUtils.endsWithIgnoreCase(sntzName, "." + fileExt) ? sntzName : sntzName + "." + fileExt;
		
		InputStream is = null;
		try {
			is = tplUrl.openStream();
			return addStoreFileFromStream(storeId, parentPath, fileName, is, overwrite);
		} finally {
			IOUtils.closeQuietly(is);
		}
	}
	
	@Override
	public String addStoreFile(StoreFileType fileType, int storeId, String parentPath, String name) throws FileSystemException, WTException {
		FileObject tfo = null, ntfo = null;
		
		try {
			checkRightsOnStore(storeId, FolderShare.ItemsRight.UPDATE);
			
			tfo = getTargetFileObject(storeId, parentPath);
			if (!tfo.isFolder()) throw new IllegalArgumentException("Please provide a valid parentPath");
			
			String sntzName = PathUtils.sanitizeFolderName(name);
			String newPath = FilenameUtils.separatorsToUnix(FilenameUtils.concat(parentPath, UriParser.encode(sntzName)));
			ntfo = getTargetFileObject(storeId, newPath);
			logger.debug("Creating store file [{}, {}]", storeId, newPath);
			if (fileType.equals(StoreFileType.FOLDER)) {
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
	
	public String suggestStoreFileName(int storeId, String path, String newName)  throws FileSystemException, WTException {
		NewTargetFile ntf = getNewTargetFileObject(storeId, PathUtils.getFullParentPath(path), newName, false);
		return PathUtils.getFileName(ntf.path);
	}
	
	@Override
	public String renameStoreFile(int storeId, String path, String newName) throws FileSystemException, FileOverwriteException, WTException {
		return renameStoreFile(storeId, path, newName, false);
	}
	
	@Override
	public String renameStoreFile(int storeId, String path, String newName, boolean overwrite) throws FileSystemException, FileOverwriteException, WTException {
		try {
			checkRightsOnStore(storeId, FolderShare.ItemsRight.UPDATE);
			return doRenameStoreFile(storeId, path, newName, overwrite);
			
		} catch(SQLException | DAOException | WTException ex) {
			throw wrapException(ex);
		}
	}
	
	@Override
	public void deleteStoreFile(int storeId, String path) throws FileSystemException, WTException {
		deleteStoreFile(storeId, Arrays.asList(path));
	}
	
	@Override
	public void deleteStoreFile(int storeId, Collection<String> paths) throws FileSystemException, WTException {
		Connection con = null;
		
		try {
			con = WT.getConnection(SERVICE_ID, false);
			
			for (String path : paths) {
				// Commit or rollback is done inside this method below
				doDeleteStoreFile(con, storeId, path);
			}
			
		} catch(SQLException | DAOException | WTException ex) {
			throw wrapException(ex);
		} finally {
			DbUtils.closeQuietly(con);
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
			ensureUser();
			con = WT.getConnection(SERVICE_ID, false);
			
			logger.debug("path starts with {}", path);
			
			List<OSharingLink> olinks = dao.selectByProfileTypeStorePath(con, getTargetProfileId(), SharingLink.LinkType.DOWNLOAD, storeId, path);
			for(OSharingLink olink : olinks) {
				final SharingLink dl = createSharingLink(olink);
				items.put(dl.getFileHash(), createSharingLink(olink));
			}
			return items;
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw wrapException(ex);
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
			ensureUser();
			con = WT.getConnection(SERVICE_ID, false);
			
			List<OSharingLink> olinks = dao.selectByProfileTypeStorePath(con, getTargetProfileId(), SharingLink.LinkType.UPLOAD, storeId, path);
			for(OSharingLink olink : olinks) {
				final SharingLink ul = createSharingLink(olink);
				items.put(ul.getFileHash(), ul);
			}
			return items;
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw wrapException(ex);
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
			
			return createSharingLink(olink);
			
		} catch(SQLException | DAOException ex) {
			DbUtils.rollbackQuietly(con);
			throw wrapException(ex);
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public SharingLink addDownloadLink(SharingLink link) throws WTException {
		Connection con = null;
		
		try {
			checkRightsOnStore(link.getStoreId(), FolderShare.ItemsRight.CREATE);
			
			con = WT.getConnection(SERVICE_ID, false);
			link.setLinkType(SharingLink.LinkType.DOWNLOAD);
			SharingLink ret = createSharingLink(doSharingLinkUpdate(true, con, link));
			
			DbUtils.commitQuietly(con);
//			if (isAuditEnabled()) {
//				auditLogWrite(AuditContext.DOWNLOADLINK, AuditAction.CREATE, ret.getLinkId(), null);
//			}
			
			return ret;
			
		} catch(SQLException | DAOException | WTException ex) {
			DbUtils.rollbackQuietly(con);
			throw wrapException(ex);
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public SharingLink addUploadLink(SharingLink link) throws WTException {
		Connection con = null;
		
		try {
			checkRightsOnStore(link.getStoreId(), FolderShare.ItemsRight.CREATE);
			
			con = WT.getConnection(SERVICE_ID, false);
			link.setLinkType(SharingLink.LinkType.UPLOAD);
			SharingLink ret = createSharingLink(doSharingLinkUpdate(true, con, link));
			
			DbUtils.commitQuietly(con);
//			if (isAuditEnabled()) {
//				auditLogWrite(AuditContext.UPLOADLINK, AuditAction.CREATE, ret.getLinkId(), null);
//			}
			
			return ret;
			
		} catch(SQLException | DAOException | WTException ex) {
			DbUtils.rollbackQuietly(con);
			throw wrapException(ex);
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
			if (olink == null) throw new WTNotFoundException("SharingLink not found [{}]", link.getLinkId());
			
			OSharingLink ret = doSharingLinkUpdate(false, con, link);
			if (ret == null) throw new WTNotFoundException("SharingLink not found [{}]", link.getLinkId());
			
			DbUtils.commitQuietly(con);
//			if (isAuditEnabled()) {
//				auditLogWrite(AuditContext.SHARINGLINK, AuditAction.UPDATE, link.getLinkId(), null);
//			}
			
		} catch(SQLException | DAOException | WTException ex) {
			DbUtils.rollbackQuietly(con);
			throw wrapException(ex);
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
			if(olink == null) throw new WTException("SharingLink not found [{}]", linkId);
			
			boolean ret = doSharingLinkDelete(con, linkId);
			if (!ret) throw new WTNotFoundException("SharingLink not found [{}]", linkId);
			
			DbUtils.commitQuietly(con);
//			if (isAuditEnabled()) {
//				auditLogWrite(AuditContext.SHARINGLINK, AuditAction.DELETE, linkId, null);
//			}
			
		} catch(SQLException | DAOException | WTException ex) {
			DbUtils.rollbackQuietly(con);
			throw wrapException(ex);
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	@Override
	public URI[] getSharingLinkPublicURLs(SharingLink link) throws WTException {
		try {
			String servicePublicUrl = WT.getServicePublicUrl(getTargetProfileId().getDomainId(), SERVICE_ID);
			return generateLinkPublicURLs(servicePublicUrl, link);
		} catch(URISyntaxException ex) {
			throw new WTException(ex);
		}
	}
	
	@Override
	public String getSharingLinkEmbedCode(SharingLink link, Locale locale, String dateFormat) {
		String servicePublicUrl = WT.getServicePublicUrl(getTargetProfileId().getDomainId(), SERVICE_ID);
		return generateLinkEmbedCode(locale, dateFormat, servicePublicUrl, link);
	}
	
	public void notifySharingLinkUsage(String linkId, String path, String ipAddress, String userAgent) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		Connection con = null;
		
		try {
			con = WT.getConnection(SERVICE_ID);
			OSharingLink olink = dao.selectById(con, linkId);
			if (olink == null) throw new WTException("Unable to retrieve sharing link [{0}]", linkId);
			
			if (olink.getLinkType().equals(EnumUtils.toSerializedName(SharingLink.LinkType.DOWNLOAD))) {
				checkRightsOnStore(olink.getStoreId(), FolderShare.FolderRight.READ);
				sendLinkUsageEmail(olink, path, ipAddress, userAgent);
				
			} else if(olink.getLinkType().equals(EnumUtils.toSerializedName(SharingLink.LinkType.UPLOAD))) {
				checkRightsOnStore(olink.getStoreId(), FolderShare.ItemsRight.UPDATE);
				sendLinkUsageEmail(olink, path, ipAddress, userAgent);
			}
			
		} catch(SQLException | DAOException | WTException ex) {
			throw wrapException(ex);
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	private SharingLink createSharingLink(OSharingLink with) {
		return fillSharingLink(new SharingLink(), with);
	}
	
	private SharingLink fillSharingLink(SharingLink fill, OSharingLink with) {
		if ((fill != null) && (with != null)) {
			fill.setLinkId(with.getSharingLinkId());
			fill.setDomainId(with.getDomainId());
			fill.setLinkType(EnumUtils.forSerializedName(with.getLinkType(), SharingLink.LinkType.class));
			fill.setUserId(with.getUserId());
			fill.setStoreId(with.getStoreId());
			fill.setFilePath(with.getFilePath());
			fill.setFileHash(with.getFileHash());
			fill.setCreatedOn(with.getCreatedOn());
			fill.setExpiresOn(with.getExpiresOn());
			fill.setAuthMode(EnumUtils.forSerializedName(with.getAuthMode(), SharingLink.AuthMode.class));
			fill.setPassword(with.getPassword());
			fill.setNotify(with.getNotify());
		}
		return fill;
	}
	
	private void fillSharingLinkWithDefaults(SharingLink fill) {
		if (fill != null) {
			if (fill.getDomainId() == null) fill.setDomainId(getTargetProfileId().getDomainId());
			if (fill.getUserId() == null) fill.setUserId(getTargetProfileId().getUserId());
			if (fill.getNotify() == null) fill.setNotify(true);
		}
	}
	
	private OSharingLink createSharingLink(SharingLink with) {
		return fillOSharingLink(new OSharingLink(), with);
	}
	
	private OSharingLink fillOSharingLink(OSharingLink fill, SharingLink with) {
		if ((fill != null) && (with != null)) {
			fill.setSharingLinkId(with.getLinkId());
			fill.setDomainId(with.getDomainId());
			fill.setLinkType(EnumUtils.toSerializedName(with.getLinkType()));
			fill.setUserId(with.getUserId());
			fill.setStoreId(with.getStoreId());
			fill.setFilePath(with.getFilePath());
			fill.setFileHash(with.getFileHash());
			fill.setCreatedOn(with.getCreatedOn());
			fill.setExpiresOn(with.getExpiresOn());
			fill.setAuthMode(EnumUtils.toSerializedName(with.getAuthMode()));
			fill.setPassword(with.getPassword());
			fill.setNotify(with.getNotify());
		}
		return fill;
	}	
	
	private UserProfileId doStoreGetOwner(int storeId) throws WTException {
		Owner owi = doStoreGetOwnerInfo(storeId);
		return (owi == null) ? null : new UserProfileId(owi.getDomainId(), owi.getUserId());
	}
	
	private Owner doStoreGetOwnerInfo(int storeId) throws WTException {
		Connection con = null;
		
		try {
			con = WT.getConnection(SERVICE_ID);
			return doStoreGetOwnerInfo(con, storeId);
			
		} catch (Exception ex) {
			throw ExceptionUtils.wrapThrowable(ex);
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	private Owner doStoreGetOwnerInfo(Connection con, int storeId) throws DAOException {
		StoreDAO stoDao = StoreDAO.getInstance();
		return stoDao.selectOwnerById(con, storeId);
	}
	
	private void checkRightsOnStoreSchema(URI uri) {
		switch(uri.getScheme()) {
			case "file":
				RunContext.ensureIsPermitted(false, SERVICE_ID, "STORE_FILE", "CREATE");
				break;
			case "dropbox":
			case "googledrive":
				RunContext.ensureIsPermitted(false, SERVICE_ID, "STORE_CLOUD", "CREATE");
				break;
			default:
				RunContext.ensureIsPermitted(false, SERVICE_ID, "STORE_OTHER", "CREATE");
		}
	}
	
	private void checkRightsOnStoreOrigin(UserProfileId originPid, String action) throws WTException {
		UserProfileId targetPid = getTargetProfileId();
		
		if (RunContext.isWebTopAdmin()) return;
		if (originPid.equals(targetPid)) return;
		
		final StoreFSOrigin origin = shareCache.getOrigin(originPid);
		if (origin == null) throw new WTException("Origin not found [{}]", originPid);
		CoreManager coreMgr = WT.getCoreManager(targetPid);
		
		boolean result = coreMgr.evaluateFolderSharePermission(SERVICE_ID, SHARE_CONTEXT_STORE, origin.getProfileId(), FolderSharing.Scope.wildcard(), true, FolderShare.EvalTarget.FOLDER, action);
		if (result) return;
		UserProfileId runPid = RunContext.getRunProfileId();
		throw new AuthException("Action '{}' not allowed for '{}' on origin '{}' [{}, {}]", action, runPid, origin.getProfileId(), SHARE_CONTEXT_STORE, targetPid.toString());
	}
	
	private boolean quietlyCheckRightsOnStore(int storeId, BitFlagsEnum<? extends Enum> right) {
		try {
			checkRightsOnStore(storeId, right);
			return true;
		} catch (AuthException ex1) {
			return false;
		} catch (WTException ex1) {
			logger.warn("Unable to check rights [{}]", storeId);
			return false;
		}
	}
	
	private void checkRightsOnStore(int storeId, BitFlagsEnum<? extends Enum> right) throws WTException {
		UserProfileId targetPid = getTargetProfileId();
		Subject subject = RunContext.getSubject();
		UserProfileId runPid = RunContext.getRunProfileId(subject);
		
		FolderShare.EvalTarget target = null;
		if (right instanceof FolderShare.FolderRight) {
			target = FolderShare.EvalTarget.FOLDER;
		} else if (right instanceof FolderShare.ItemsRight) {
			target = FolderShare.EvalTarget.FOLDER_ITEMS;
		} else {
			throw new WTRuntimeException("Unsupported right");
		}
		
		final UserProfileId ownerPid = ownerCache.get(storeId);
		if (ownerPid == null) throw new WTException("Owner not found [{}]", storeId);
		
		if (RunContext.isWebTopAdmin(subject)) {
			// Skip checks for running wtAdmin and sysAdmin target
			if (targetPid.equals(RunContext.getSysAdminProfileId())) return;
			// Skip checks if target is the resource owner
			if (ownerPid.equals(targetPid)) return;
			// Skip checks if resource is a valid incoming folder
			if (shareCache.getFolderIds().contains(storeId)) return;
			
			String exMsg = null;
			if (FolderShare.EvalTarget.FOLDER.equals(target)) {
				exMsg = "Action '{}' not allowed for '{}' on folder '{}' [{}, {}]";
			} else if (FolderShare.EvalTarget.FOLDER_ITEMS.equals(target)) {
				exMsg = "Action '{}' not allowed for '{}' on elements of folder '{}' [{}, {}]";
			}
			
			throw new AuthException(exMsg, right.name(), runPid, storeId, SHARE_CONTEXT_STORE, targetPid.toString());
			
		} else {
			// Skip checks if target is the resource owner and it's the running profile
			if (ownerPid.equals(targetPid) && targetPid.equals(runPid)) return;
			
			StoreFSOrigin origin = shareCache.getOriginByFolderId(storeId);
			if (origin == null) throw new WTException("Origin not found [{}]", storeId);
			CoreManager coreMgr = WT.getCoreManager(targetPid);
			
			Boolean eval = null;
			// Check right at wildcard scope
			eval = coreMgr.evaluateFolderSharePermission(SERVICE_ID, SHARE_CONTEXT_STORE, ownerPid, FolderSharing.Scope.wildcard(), false, target, right.name());
			if (eval != null && eval == true) return;
			// Check right at folder scope
			eval = coreMgr.evaluateFolderSharePermission(SERVICE_ID, SHARE_CONTEXT_STORE, ownerPid, FolderSharing.Scope.folder(String.valueOf(storeId)), false, target, right.name());
			if (eval != null && eval == true) return;
			
			String exMsg = null;
			if (FolderShare.EvalTarget.FOLDER.equals(target)) {
				exMsg = "Action '{}' not allowed for '{}' on folder '{}' [{}, {}, {}]";
			} else if (FolderShare.EvalTarget.FOLDER_ITEMS.equals(target)) {
				exMsg = "Action '{}' not allowed for '{}' on elements of folder '{}' [{}, {}, {}]";
			}
			throw new AuthException(exMsg, right.name(), runPid, storeId, ownerPid, SHARE_CONTEXT_STORE, targetPid.toString());
		}
	}
	
	private String prependFileBasePath(URI uri) {
		VfsSettings.StoreFileBasepathTemplateValues tpl = new VfsSettings.StoreFileBasepathTemplateValues();
		
		tpl.SERVICE_HOME = WT.getServiceHomePath(SERVICE_ID,getTargetProfileId());
		tpl.SERVICE_ID = SERVICE_ID;
		tpl.DOMAIN_ID = getTargetProfileId().getDomain();
		
		VfsServiceSettings vus = new VfsServiceSettings(SERVICE_ID, getTargetProfileId().getDomain());
		return PathUtils.concatPaths(vus.getStoreFileBasepath(tpl), uri.getPath());
	}
	
	private Set<Integer> doListStoreIdsIn(Connection con, UserProfileId profileId, Collection<Integer> storeIds) throws WTException {
		StoreDAO stoDao = StoreDAO.getInstance();
		
		if (storeIds == null) {
			return stoDao.selectIdsByProfile(con, profileId.getDomainId(), profileId.getUserId());
		} else {
			return stoDao.selectIdsByProfileIn(con, profileId.getDomainId(), profileId.getUserId(), storeIds);
		}
	}
	
	private Store doStoreUpdate(boolean insert, Connection con, Store store) throws WTException {
		StoreDAO stoDao = StoreDAO.getInstance();
		
		OStore ostore = ManagerUtils.createOStore(store);
		if (ostore.getDomainId() == null) ostore.setDomainId(getTargetProfileId().getDomainId());
		if (ostore.getUserId() == null) ostore.setUserId(getTargetProfileId().getUserId());
		
		try {
			URI uri = store.getUri();
			if ((store.getBuiltIn() == 0) && uri.getScheme().equals("file")) {
				ostore.setUri(Store.buildURI("file", null, null, null, null, prependFileBasePath(uri)).toString());
			}
			
			int ret = -1;
			if (store.getBuiltIn()==Store.BUILTIN_VOLATILE) {
				ret = 1;
			} else {
				if (insert) {
					ostore.setStoreId(stoDao.getSequence(con).intValue());
					ret = stoDao.insert(con, ostore);
				} else {
					ret = stoDao.update(con, ostore);
				}
			}
			
			return (ret == 1) ? ManagerUtils.createStore(ostore, buildStoreName(ostore, getLocale())) : null;
			
		} catch (URISyntaxException ex) {
			throw new WTException("Provided URI is not valid", ex);
		}
	}
	
	private boolean doStoreDelete(Connection con, int storeId) throws WTException {
		StoreDAO stoDao = StoreDAO.getInstance();
		SharingLinkDAO linDao = SharingLinkDAO.getInstance();
		
		int ret = stoDao.deleteById(con, storeId);
		linDao.deleteByStore(con, storeId);
		return ret == 1;
	}
	
	private FileObject getTargetFileObject(int storeId, String path) throws FileSystemException, WTException {
		StoreFileSystem sfs = getStoreFileSystemFromCache(storeId);
		if (sfs == null) throw new WTException("Unable to get store fileSystem");
		
		FileObject tfo = null;
		if (path.equals("/")) {
			tfo = sfs.getRootFileObject();
		} else {
			tfo = sfs.getDescendantFileObject(path);
		}
		if(tfo == null) throw new WTException("Cannot resolve target path [{0}]", path);
		return tfo;
	}
	
	private NewTargetFile getNewTargetFileObject(int storeId, String parentPath, String name, boolean overwrite) throws FileSystemException, WTException {
		String newPath = FilenameUtils.separatorsToUnix(FilenameUtils.concat(parentPath, UriParser.encode(name)));
		
		if (overwrite) {
			return new NewTargetFile(newPath, getTargetFileObject(storeId, newPath));
			
		} else {
			FileObject newFo = getTargetFileObject(storeId, newPath);
			if (!newFo.exists()) {
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
	
	private String doRenameStoreFile(int storeId, String path, String newName, boolean overwrite) throws FileSystemException, SQLException, DAOException, FileOverwriteException, WTException {
		SharingLinkDAO shaDao = SharingLinkDAO.getInstance();
		FileObject tfo = null, ntfo = null;
		Connection con = null;
		
		try {
			tfo = getTargetFileObject(storeId, path);
			String sntzName = PathUtils.sanitizeFolderName(newName);
			String newPath = FilenameUtils.separatorsToUnix(FilenameUtils.concat(PathUtils.getFullParentPath(path), UriParser.encode(sntzName)));
			ntfo = getTargetFileObject(storeId, newPath);
			if (!overwrite && ntfo.exists()) throw new FileOverwriteException("A file with same name already exists [{}]", newPath);
			
			logger.debug("Renaming store file [{}, {} -> {}]", storeId, path, newPath);
			try {
				con = WT.getConnection(SERVICE_ID, false);
				
				shaDao.deleteByStorePath(con, storeId, path);
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
	
	private void doDeleteStoreFile(Connection con, int storeId, String path) throws FileSystemException, SQLException, DAOException, WTException {
		SharingLinkDAO shaDao = SharingLinkDAO.getInstance();
		FileObject tfo = null;
		
		try {
			logger.debug("Deleting store file [{}, {}]", storeId, path);
			tfo = getTargetFileObject(storeId, path);
			
			try {
				shaDao.deleteByStorePath(con, storeId, path);
				tfo.delete(Selectors.SELECT_ALL);
				DbUtils.commitQuietly(con);
				
			} catch(FileSystemException ex1) {
				DbUtils.rollbackQuietly(con);
				throw ex1;
			}
		} finally {
			IOUtils.closeQuietly(tfo);
		}
	}
	
	/*
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
	*/
	
	private String generateLinkId(UserProfileId profileId, String linkType, int storeId, String path) {
		return AlgoUtils.md5Hex(new CompositeId(profileId.getDomainId(), profileId.getUserId(), linkType, storeId, path).toString());
	}
	
	private OSharingLink doSharingLinkUpdate(boolean insert, Connection con, SharingLink sl) throws DAOException {
		SharingLinkDAO slDao = SharingLinkDAO.getInstance();
		UserProfileId pid = getTargetProfileId();
		
		fillSharingLinkWithDefaults(sl);
		sl.validate(!insert);
		
		OSharingLink o = createSharingLink(sl);
		int ret = -1;
		if (insert) {
			o.setSharingLinkId(generateLinkId(pid, EnumUtils.toSerializedName(sl.getLinkType()), o.getStoreId(), o.getFilePath()));
			o.setFileHash(VfsManagerUtils.generateStoreFileHash(o.getStoreId(), o.getFilePath()));
			o.setCreatedOn(DateTimeUtils.now());
			ret = slDao.insert(con, o);
		} else {
			ret = slDao.update(con, o);
		}
		return (ret == 1) ? o : null;
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
	
	private boolean doSharingLinkDelete(Connection con, String linkId) throws WTException {
		SharingLinkDAO dao = SharingLinkDAO.getInstance();
		
		int ret = dao.deleteById(con, linkId);
		//TODO: cancellare collegati
		return ret == 1;
	}
	
	private void sendLinkUsageEmail(OSharingLink olink, String path, String ipAddress, String userAgent) throws WTException {
		final String BHD_KEY = (olink.getLinkType().equals(EnumUtils.toSerializedName(SharingLink.LinkType.DOWNLOAD))) ? VfsLocale.TPL_EMAIL_SHARINGLINKUSAGE_BODY_HEADER_DL : VfsLocale.TPL_EMAIL_SHARINGLINKUSAGE_BODY_HEADER_UL;
		UserProfileId pid = olink.getProfileId();
		
		//TODO: rendere relativa la path del file rispetto allo Store???
		try {
			UserProfile.Data ud = WT.getUserData(olink.getProfileId());
			String bodyHeader = lookupResource(ud.getLocale(), BHD_KEY);
			String source = NotificationHelper.buildSource(ud.getLocale(), SERVICE_ID);
			String subject = TplHelper.buildLinkUsageEmailSubject(ud.getLocale(), bodyHeader);
			String customBody = TplHelper.buildLinkUsageBodyTpl(ud.getLocale(), olink.getSharingLinkId(), PathUtils.getFileName(olink.getFilePath()), path, ipAddress, userAgent);
			String html = NotificationHelper.buildCustomBodyTplForNoReplay(ud.getLocale(), source, bodyHeader, customBody);
			
			InternetAddress from = WT.getNotificationAddress(pid.getDomainId());
			if (from == null) throw new WTException("Error building sender address");
			InternetAddress to = ud.getEmail();
			if (to == null) throw new WTException("Error building destination address");
			WT.sendEmail(getMailSession(), true, from, to, subject, html);

		} catch(IOException | TemplateException ex) {
			logger.error("Unable to build email template", ex);
		} catch(Exception ex) {
			logger.error("Unable to send email", ex);
		}
	}
	
	private class OwnerCache extends AbstractMapCache<Integer, UserProfileId> {

		@Override
		protected void internalInitCache(Map<Integer, UserProfileId> mapObject) {}

		@Override
		protected void internalMissKey(Map<Integer, UserProfileId> mapObject, Integer key) {
			try {
				UserProfileId owner = doStoreGetOwner(key);
				if (owner == null) throw new WTException("Owner not found [{0}]", key);
				mapObject.put(key, owner);
			} catch(WTException ex) {
				logger.trace("OwnerCache miss", ex);
			}
		}
	}
	
	private class ShareCache extends AbstractFolderShareCache<Integer, StoreFSOrigin> {
		
		@Override
		protected void internalBuildCache() {
			final CoreManager coreMgr = WT.getCoreManager(getTargetProfileId());
			try {
				for (StoreFSOrigin origin : getOrigins(coreMgr)) {
					origins.add(origin);
					originByProfile.put(origin.getProfileId(), origin);
					
					FolderShareOriginFolders folders = null;
					folders = coreMgr.getFolderShareOriginFolders(SERVICE_ID, SHARE_CONTEXT_STORE, origin.getProfileId());
					foldersByProfile.put(origin.getProfileId(), folders);
					
					final Set<Integer> categoryIds;
					if (folders.wildcard()) {
						categoryIds = listStoreIds(origin.getProfileId());
					} else {
						Set<Integer> ids = folders.getFolderIds().stream()
							.map(value -> Integer.valueOf(value))
							.collect(Collectors.toSet());
						categoryIds = listStoreIdsIn(origin.getProfileId(), ids);
					}
					categoryIds.forEach(categoryId -> {originByFolderId.put(categoryId, origin);});
					folderIdsByProfile.putAll(origin.getProfileId(), categoryIds);
					folderIds.addAll(categoryIds);
				}
			} catch (WTException ex) {
				throw new WTRuntimeException(ex, "[ShareCache] Unable to build cache for '{}'", getTargetProfileId());
			}
		}
		
		private List<StoreFSOrigin> getOrigins(final CoreManager coreMgr) throws WTException {
			List<StoreFSOrigin> items = new ArrayList<>();
			for (ShareOrigin origin : coreMgr.listFolderShareOrigins(SERVICE_ID, SHARE_CONTEXT_STORE)) {
				// Do permissions evaluation returning NULL in case of missing share: a root origin may not be shared!
				FolderShare.Permissions permissions = coreMgr.evaluateFolderSharePermissions(SERVICE_ID, SHARE_CONTEXT_STORE, origin.getProfileId(), FolderSharing.Scope.wildcard(), false);
				if (permissions == null) {
					// If missing, simply treat it as NONE permission.
					permissions = FolderShare.Permissions.none();
				}
				items.add(new StoreFSOrigin(origin, permissions));
			}
			return items;
		}
	}
	
	private class NewTargetFile {
		public String path;
		public FileObject tfo;
		
		public NewTargetFile(String path, FileObject tfo) {
			this.path = path;
			this.tfo = tfo;
		}
	}
	
	private enum AuditContext {
		STORE, DOWNLOADLINK, UPLOADLINK, SHARINGLINK
	}
	
	private enum AuditAction {
		CREATE, UPDATE, DELETE, MOVE
	}
	
	public static byte[] generateLinkQRCode(String publicBaseUrl, SharingLink link, int size, String color) throws WTException {
		try {
			URI[] urls = VfsManager.generateLinkPublicURLs(publicBaseUrl, link);
			if (PathUtils.isFolder(link.getFilePath())) {
				return QRCode.from(urls[0].toString()).withSize(size, size).stream().toByteArray();
			} else {
				return QRCode.from(urls[2].toString()).withSize(size, size).stream().toByteArray();
			}

		} catch(URISyntaxException ex) {
			logger.error("Unable to generate QRCode for [{}]", ex, link);
			throw new WTException(ex);
		}
	}
	
	public static String generateLinkEmbedCode(Locale locale, String dateFormat, String publicBaseUrl, SharingLink link) {
		try {
			URI[] urls = VfsManager.generateLinkPublicURLs(publicBaseUrl, link);
			
			URI url = (urls[1] != null) ? urls[1] : urls[0];
			String expiration = (link.getExpiresOn() != null) ? DateTimeUtils.createFormatter(dateFormat).print(link.getExpiresOn()) : null;
			String password = (link.getAuthMode().equals(SharingLink.AuthMode.PASSWORD)) ? link.getPassword() : null;
			
			return TplHelper.buildLinkEmbedCodeTpl(locale, link.getLinkType(), url.toString(), PathUtils.getFileName(link.getFilePath()), expiration, password);
			
		} catch(IOException | TemplateException | URISyntaxException ex) {
			logger.error("Unable to build embed template", ex);
			return null;
		}
	}
	
	public static URI[] generateLinkPublicURLs(String publicBaseUrl, SharingLink link) throws URISyntaxException {
		// [0] - Preview link
		// [1] - Direct link to content stream (forced download)
		// [2] - Direct link to content stream (inline)
		
		if (link.getLinkType().equals(SharingLink.LinkType.DOWNLOAD)) {
			if (PathUtils.isFolder(link.getFilePath())) {
				//TODO: implementare nel pubblico la gestione link diretti per le cartelle
				return new URI[]{
					buildLinkPublicUrl(publicBaseUrl, link, false, false),
					buildLinkPublicUrl(publicBaseUrl, link, true, true),
					buildLinkPublicUrl(publicBaseUrl, link, true, true)
				};
			} else {
				//TODO: implementare nel pubblico l'anteprima dei file
				return new URI[]{
					null,
					buildLinkPublicUrl(publicBaseUrl, link, true, true),
					buildLinkPublicUrl(publicBaseUrl, link, true, false)
				};
			}
		} else { // Upload links have only the managed URL
			return new URI[]{
				buildLinkPublicUrl(publicBaseUrl, link, false, false),
				null,
				null
			};
		}
	}
	
	/**
	 * 
	 * @param publicBaseUrl The base URL up to the public servlet path (eg. http://localhost/webtop/public/{domain}/vfs)
	 * @param link The link being shared.
	 * @param directToStream Whether to point to file stream directly.
	 * @param forceDownload In case of direct stream, forces the download without leaving the decision to the browser.
	 * @return The generated URL.
	 * @throws URISyntaxException 
	 */
	public static URI buildLinkPublicUrl(String publicBaseUrl, SharingLink link, boolean directToStream, boolean forceDownload) throws URISyntaxException {
		URIBuilder builder = new URIBuilder(publicBaseUrl);
		//TODO: Magari spostare il metodo in un builder dedicato nel progetto api
		final String p = PublicService.PUBPATH_CONTEXT_LINK + "/" + link.getLinkId();
		URIUtils.appendPath(builder, p);
		if (directToStream) {
			if (forceDownload) {
				builder.addParameter("dl", "1");
			} else {
				builder.addParameter("dl", "2");
			}
		}
		return builder.build();
	}
}
