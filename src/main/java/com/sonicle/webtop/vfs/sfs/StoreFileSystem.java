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
package com.sonicle.webtop.vfs.sfs;

import com.sonicle.commons.PathUtils;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.local.LocalFileName;

/**
 *
 * @author malbinola
 */
public abstract class StoreFileSystem {
	protected URI uri;
	protected String parameters;
	protected boolean autoCreateRoot = false;
	protected final FileSystemOptions fso;
	protected FileObject rootFo = null;
	protected final Object lock = new Object();
	
	public StoreFileSystem(String uri, String parameters) throws URISyntaxException {
		this(uri, parameters, false);
	}
	
	public StoreFileSystem(String uri, String parameters, boolean autoCreateRoot) throws URISyntaxException {
		this.uri = new URI(uri);
		this.parameters = parameters;
		this.autoCreateRoot = autoCreateRoot;
		fso = new FileSystemOptions();
	}
	
	protected abstract void configureOptions() throws FileSystemException;
	
	private void initRootFileObject() throws FileSystemException {
		configureOptions();
		FileSystemManager fsm = VFS.getManager();
		FileObject fo = fsm.resolveFile(uri.toString(), fso);
		if(autoCreateRoot && !fo.exists()) fo.createFolder();
		if(fo.exists()) rootFo = fo;
	}
	
	public FileObject getRootFileObject() throws FileSystemException {
		synchronized(lock) {
			if(rootFo == null) initRootFileObject();
		}
		return rootFo;
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
}
