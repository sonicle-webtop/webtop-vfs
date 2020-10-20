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
package com.sonicle.webtop.vfs.sfs;

import com.sonicle.commons.PathUtils;
import com.sonicle.commons.db.DbUtils;
import com.sonicle.webtop.core.app.WT;
import com.sonicle.webtop.core.dal.DAOException;
import com.sonicle.webtop.core.sdk.WTException;
import com.sonicle.webtop.vfs.dal.StoreDAO;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;

/**
 *
 * @author malbinola
 */
public abstract class StoreFileSystem {
	protected int storeId;
	protected URI uri;
	protected String parameters;
	protected boolean autoCreateRoot = false;
	protected final FileSystemOptions fso;
	protected FileObject rootFo = null;
	protected final Object lock = new Object();
	
	public StoreFileSystem(int storeId, URI uri, String parameters) {
		this(storeId, uri, parameters, false);
	}
	
	public StoreFileSystem(int storeId, URI uri, String parameters, boolean autoCreateRoot) {
		this.storeId = storeId;
		this.uri = uri;
		this.parameters = parameters;
		this.autoCreateRoot = autoCreateRoot;
		fso = new FileSystemOptions();
	}
	
	protected abstract void configureOptions() throws FileSystemException;
	
	public FileObject getRootFileObject() throws FileSystemException {
		synchronized(lock) {
			if(rootFo == null) {
				configureOptions();
				rootFo = resolveRoot();
			}
			return rootFo;
		}
	}
	
	public FileObject getDescendantFileObject(String relativePath) throws FileSystemException {
		FileObject rfo = getRootFileObject();
		String path = PathUtils.concatPaths(rfo.getName().getPath(), relativePath);
		return getRootFileObject().resolveFile(path);
	}
	
	public String getRelativePath(FileObject fo) throws FileSystemException {
		FileName rootName = getRootFileObject().getName();
		return PathUtils.ensureBeginningSeparator(rootName.getRelativeName(fo.getName()));
	}
	
	protected FileObject resolveRoot() throws FileSystemException {
		FileObject fo = VFS.getManager().resolveFile(uri.toString(), fso);
		if (autoCreateRoot && !fo.exists()) fo.createFolder();
		if (!fo.exists()) throw new FileSystemException("Root not exist");
		return fo;
	}
	
	protected int updateStore() throws WTException {
		StoreDAO dao = StoreDAO.getInstance();
		Connection con = null;
		
		try {
			con = WT.getConnection("com.sonicle.webtop.vfs");
			return dao.updateUriParameters(con, storeId, uri.toString(), parameters);
			
		} catch(SQLException | DAOException ex) {
			throw new WTException(ex, "DB error");
		} finally {
			DbUtils.closeQuietly(con);
		}
	}
	
	
	
	
	/*
	private void initRootFileObject() throws FileSystemException {
		configureOptions();
		FileSystemManager fsm = VFS.getManager();
		FileObject fo = fsm.resolveFile(uri.toString(), fso);
		if(autoCreateRoot && !fo.exists()) fo.createFolder();
		if(fo.exists()) rootFo = fo;
	}
	*/
}
