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
import com.sonicle.commons.URIUtils;
import com.sonicle.commons.time.DateTimeUtils;
import com.sonicle.commons.web.ServletUtils;
import com.sonicle.commons.web.json.JsonResult;
import com.sonicle.commons.web.json.MapItem;
import com.sonicle.vfs2.VfsUtils;
import com.sonicle.webtop.core.app.WT;
import com.sonicle.webtop.core.app.WebTopSession;
import com.sonicle.webtop.core.bol.js.JsWTSPublic;
import com.sonicle.webtop.core.sdk.BasePublicService;
import com.sonicle.webtop.core.sdk.UploadException;
import com.sonicle.webtop.core.sdk.WTException;
import com.sonicle.webtop.core.sdk.interfaces.IServiceUploadStreamListener;
import com.sonicle.webtop.core.app.servlet.ServletHelper;
import com.sonicle.webtop.vfs.bol.js.JsPubGridFile;
import com.sonicle.webtop.vfs.model.SharingLink;
import com.sonicle.webtop.vfs.model.StoreFileType;
import com.sonicle.webtop.vfs.sfs.StoreFileSystem;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.ZipOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;

/**
 *
 * @author malbinola
 */
public class PublicService extends BasePublicService {
	private static final Logger logger = WT.getLogger(PublicService.class);
	private static final String WTSPROP_AUTHED_LINKS = "AUTHEDLINKS";
	public static final String PUBPATH_CONTEXT_LINK = "link";
	
	private final Object lock1 = new Object();
	private VfsManager manager;
	
	@Override
	public void initialize() throws Exception {
		manager = (VfsManager)WT.getServiceManager(SERVICE_ID);
		registerUploadListener("UploadFile", new OnUploadFile());
	}

	@Override
	public void cleanup() throws Exception {
		manager = null;
	}
	
	private WebTopSession getWts() {
		return getEnv().getWebTopSession();
	}
	
	private HashSet<String> getAuthedLinks() {
		WebTopSession wts = getWts();
		synchronized(lock1) {
			if(!wts.hasProperty(SERVICE_ID, WTSPROP_AUTHED_LINKS)) {
				return (HashSet<String>)wts.setProperty(SERVICE_ID, WTSPROP_AUTHED_LINKS, new HashSet<String>());
			} else {
				return (HashSet<String>)wts.getProperty(SERVICE_ID, WTSPROP_AUTHED_LINKS);
			}
		}
	}
	
	@Override
	public void processDefaultAction(HttpServletRequest request, HttpServletResponse response) throws Exception {
		PublicPath path = new PublicPath(request.getPathInfo());
		WebTopSession wts = getWts();
		
		try {
			try {
				String domainId = WT.findDomainIdByPublicName(path.getDomainPublicName());
				if (domainId == null) throw new WTException("Invalid domain public name [{0}]", path.getDomainPublicName());
				
				if(path.getContext().equals(PUBPATH_CONTEXT_LINK)) {
					FileUrlPath fileUrlPath = new FileUrlPath(path.getRemainingPath());

					SharingLink link = null;
					if (!StringUtils.isBlank(fileUrlPath.getLinkId())) {
						link = manager.getSharingLink(fileUrlPath.getLinkId());
					}

					if (link == null) { // Link not found
						logger.trace("Link not found [{}]", fileUrlPath.getLinkId());
						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
						writeErrorPage(request, response, wts, "linknotfound");

					} else if (link.isExpired(DateTimeUtils.now())) { // Link expired
						logger.trace("Link expired [{}]", fileUrlPath.getLinkId());
						response.setStatus(HttpServletResponse.SC_NOT_FOUND);
						writeErrorPage(request, response, wts, "linkexpired");

					} else if (!isLinkAuthorized(link)) { // Link not authorized
						writeLinkPage(request, response, wts, "Authorize", link);

					} else if (link.getLinkType().equals(SharingLink.LinkType.DOWNLOAD)) {
						if (PathUtils.isFolder(link.getFilePath())) {
							Integer dl = ServletUtils.getIntParameter(request, "dl", 0);

							if (dl == 1) { // Download file request
								String fileId = ServletUtils.getStringParameter(request, "fileId", true);
								
								String outName;
								if (PathUtils.isFolder(fileId)) {
									if (PathUtils.isRootFolder(fileId)) {
										outName = StringUtils.defaultString(PathUtils.getFileName(link.getFilePath()), link.getLinkId());
									} else {
										outName = PathUtils.getFileName(fileId);
									}
									outName += ".zip";
								} else {
									outName = PathUtils.getFileName(fileId);
								}
								
								final String servicePublicUrl = WT.getServicePublicUrl(link.getDomainId(), SERVICE_ID);
								final URI url = buildPublicLinkFolderGetUrl(servicePublicUrl, link, fileId, outName);
								ServletUtils.setLocationHeader(response, url.toASCIIString());
								response.setStatus(HttpServletResponse.SC_FOUND);

							} else if (fileUrlPath.isGet()) { // Real binary stream
								String p = ServletUtils.getStringParameter(request, "p", true);
								
								String filePath = PathUtils.concatPaths(link.getFilePath(), p);
								boolean written = writeStoreFile(response, link, fileUrlPath.getOutFileName(), false);
								if (!written) {
									response.setStatus(HttpServletResponse.SC_NOT_FOUND);
									writeErrorPage(request, response, wts, "linknotfound");
								} else {
									if (link.getNotify()) {
										manager.notifySharingLinkUsage(link.getLinkId(), filePath, wts.getRemoteIP(), wts.getPlainUserAgent());
									}
								}

							} else {
								writeLinkPage(request, response, wts, "DownloadLink", link);
							}
							
						} else {
							Integer raw = ServletUtils.getIntParameter(request, "raw", 0);
							if ((raw == 1) || (raw == 2)) { // Link points directly to file raw data (no preview page)
								boolean inline = (raw == 2); // inline=2 to request an inline content-disposition
								final String servicePublicUrl = WT.getServicePublicUrl(link.getDomainId(), SERVICE_ID);
								final URI url = buildPublicLinkFileGetUrl(servicePublicUrl, link, inline);
								ServletUtils.setLocationHeader(response, url.toString());
								response.setStatus(HttpServletResponse.SC_FOUND);
								
							} else if (fileUrlPath.isGet()) { // Real binary stream
								boolean inline = ServletUtils.getBooleanParameter(request, "inline", false);
								
								boolean written = writeStoreFile(response, link, fileUrlPath.getOutFileName(), inline);
								if (!written) {
									response.setStatus(HttpServletResponse.SC_NOT_FOUND);
									writeErrorPage(request, response, wts, "linknotfound");
								} else {
									if (link.getNotify()) {
										manager.notifySharingLinkUsage(link.getLinkId(), link.getFilePath(), wts.getClientRemoteIP(), wts.getPlainUserAgent());
									}
								}
								
							} else {
								logger.trace("Invalid request");
								response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
								writeErrorPage(request, response, wts, "badrequest");
							}
						}
						
					} else if (link.getLinkType().equals(SharingLink.LinkType.UPLOAD)) {
						VfsUserSettings us = new VfsUserSettings(SERVICE_ID, link.getProfileId());
						
						JsWTSPublic.Vars vars = new JsWTSPublic.Vars();
						vars.put("publicUploadMaxFileSize", us.getPublicUploadMaxFileSize(true));
						writeLinkPage(request, response, wts, "UploadLink", link, vars);
					}

				} else {
					logger.trace("Invalid context [{}]", path.getContext());
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					writeErrorPage(request, response, wts, "badrequest");
				}
				
			} catch(Exception ex) {
				logger.trace("Error", ex);
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				writeErrorPage(request, response, wts, "badrequest");
			}
		} catch(Throwable t) {
			logger.error("Unexpected error", t);
		}
	}
	
	public void processAuthorizeLink(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		
		try {
			String linkId = ServletUtils.getStringParameter(request, "linkId", true);
			String password = ServletUtils.getStringParameter(request, "password", true);
			
			SharingLink link = manager.getSharingLink(linkId);
			if(link == null) throw new WTException("Link not found [{0}]", linkId);
			
			if(isLinkAuthorized(link)) {
				new JsonResult().printTo(out);
			} else {
				boolean check = true;
				if(link.getAuthMode().equals(SharingLink.AuthMode.PASSWORD)) {
					check = StringUtils.equals(password, link.getPassword());
				}

				if(check) {
					getAuthedLinks().add(linkId);
					new JsonResult().printTo(out);
				} else {
					new JsonResult(false, null).printTo(out);
				}
			}
			
		} catch(Exception ex) {
			logger.error("Error in CheckLinkPassword", ex);
			new JsonResult(false, "Error").printTo(out);
		}
	}
	
	public void processPreviewFiles(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
		ArrayList<JsPubGridFile> items = new ArrayList<>();
		
		try {
			String linkId = ServletUtils.getStringParameter(request, "linkId", true);
			String fileId = ServletUtils.getStringParameter(request, "fileId", true);
			
			SharingLink link = manager.getSharingLink(linkId);
			if(link == null) throw new WTException("Link not found [{0}]", linkId);
			if(!isLinkAuthorized(link)) throw new WTException("Link not authorized [{0}]", linkId);
			
			String path = PathUtils.concatPaths(link.getFilePath(), fileId);
			if(!PathUtils.isFolder(path)) throw new WTException("Invalid file [{0}]", path);
			
			VfsManager vfsMgr = (VfsManager)WT.getServiceManager(SERVICE_ID, true, link.getProfileId());
			StoreFileSystem sfs = vfsMgr.getStoreFileSystem(link.getStoreId());
			for(FileObject fo : vfsMgr.listStoreFiles(StoreFileType.FILE_OR_FOLDER, link.getStoreId(), path)) {
				if(VfsUtils.isFileObjectHidden(fo)) continue;
				// Relativize path and force trailing separator if file is a folder
				String filePath = fo.isFolder() ? PathUtils.ensureTrailingSeparator(sfs.getRelativePath(fo), false) : sfs.getRelativePath(fo);
				// Relativize path to link (suitable only for folders...see before)
				filePath = link.relativizePath(filePath);
				items.add(new JsPubGridFile(filePath, fo));
			}
			new JsonResult("files", items).printTo(out);
			
		} catch(Exception ex) {
			logger.error("Error in PreviewFiles", ex);
			new JsonResult(false, "Error").printTo(out);
		}
	}
	
	private class OnUploadFile implements IServiceUploadStreamListener {
		@Override
		public void onUpload(String context, HttpServletRequest request, HashMap<String, String> multipartParams, WebTopSession.UploadedFile file, InputStream is, MapItem responseData) throws UploadException {
			//TODO: inibire upload se dimensione > publicUploadMaxFileSize
			try {
				String linkId = multipartParams.get("linkId");
				if(StringUtils.isBlank(linkId)) throw new UploadException("Missing parameter [linkId]");

				SharingLink link = manager.getSharingLink(linkId);
				if(link == null) throw new UploadException("Link not found [{0}]", linkId);
				if(!link.getLinkType().equals(SharingLink.LinkType.UPLOAD)) throw new UploadException("Wrong link type [{0}]", linkId);

				VfsManager vfsMgr = (VfsManager)WT.getServiceManager(SERVICE_ID, true, link.getProfileId());
				String newPath = vfsMgr.addStoreFileFromStream(link.getStoreId(), link.getFilePath(), file.getFilename(), is);
				if (link.getNotify()) {
					WebTopSession wts = getWts();
					vfsMgr.notifySharingLinkUsage(link.getLinkId(), link.relativizePath(newPath), wts.getRemoteIP(), wts.getPlainUserAgent());
				}
				
			} catch(UploadException ex) {
				logger.trace("Upload failure", ex);
				throw ex;
			} catch(Throwable t) {
				logger.error("Upload failure", t);
				throw new UploadException("Upload failure");
			}
		}
	}
	
	private boolean isLinkAuthorized(SharingLink link) {
		if(link.getAuthMode().equals(SharingLink.AuthMode.PASSWORD)) {
			return getAuthedLinks().contains(link.getLinkId());
		} else {
			return true;
		}
	}
	
	private boolean writeStoreFile(HttpServletResponse response, SharingLink link, String outFileName, boolean inline) {
		try {
			FileObject fo = null;
			try {
				VfsManager vfsMgr = (VfsManager)WT.getServiceManager(SERVICE_ID, true, link.getProfileId());
				fo = vfsMgr.getStoreFile(link.getStoreId(), link.getFilePath());
				if (!fo.exists()) throw new WTException("File does not exist [{0}, {1}]", link.getStoreId(), link.getFilePath());
				
				if (fo.isFile()) {
					//String mediaType = ServletHelper.guessMediaType(fo.getName().getBaseName(), true);
					if (inline) {
						ServletUtils.setFileStreamHeaders(response, outFileName);
					} else {
						ServletUtils.setFileStreamHeadersForceDownload(response, outFileName);
					}
					ServletUtils.setContentLengthHeader(response, fo.getContent().getSize());
					IOUtils.copy(fo.getContent().getInputStream(), response.getOutputStream());

				} else if(fo.isFolder()) {
					OutputStream os = response.getOutputStream();
					ServletUtils.setFileStreamHeadersForceDownload(response, outFileName); // Filename already contains .zip extension!
					ZipOutputStream zos = new ZipOutputStream(os);
					try {
						VfsUtils.zipFileObject(fo, zos, true);
						zos.flush();
					} finally {
						IOUtils.closeQuietly(zos);
					}
				}
			} finally {
				IOUtils.closeQuietly(fo);
			}
			return true;
			
		} catch(Exception ex) {
			logger.error("Unable to write file", ex);
			return false;
		}
	}
	
	private void writeLinkPage(HttpServletRequest request, HttpServletResponse response, WebTopSession wts, String view, SharingLink link) throws IOException, TemplateException {
		writeLinkPage(request, response, wts, view, link, new JsWTSPublic.Vars());
	}
	
	private void writeLinkPage(HttpServletRequest request, HttpServletResponse response, WebTopSession wts, String view, SharingLink link, JsWTSPublic.Vars vars) throws IOException, TemplateException {
		vars.put("view", view);
		vars.put("linkId", link.getLinkId());
		vars.put("linkName", PathUtils.getFileName(link.getFilePath()));
		writePage(response, wts, vars, ServletHelper.getBaseUrl(request));
	}
	
	private void writeErrorPage(HttpServletRequest request, HttpServletResponse response, WebTopSession wts, String reskey) throws IOException, TemplateException {
		JsWTSPublic.Vars vars = new JsWTSPublic.Vars();
		vars.put("view", "Error");
		vars.put("reskey", reskey);
		writePage(response, wts, vars, ServletHelper.getBaseUrl(request));
	}
	
	private URI buildPublicLinkFileGetUrl(String publicBaseUrl, SharingLink link, boolean inline) throws URISyntaxException {
		URIBuilder builder = new URIBuilder(publicBaseUrl);
		final String p = PublicService.PUBPATH_CONTEXT_LINK + "/" + link.getLinkId() + "/" + FileUrlPath.TOKEN_GET + "/" + PathUtils.getFileName(link.getFilePath());
		URIUtils.appendPath(builder, p);
		if (inline) builder.addParameter("inline", "true");
		return builder.build();
	}
	
	private URI buildPublicLinkFolderGetUrl(String publicBaseUrl, SharingLink link, String path, String outFileName) throws URISyntaxException {
		URIBuilder builder = new URIBuilder(publicBaseUrl);
		final String p = FileUrlPath.TOKEN_LINK + "/" + link.getLinkId() + "/" + FileUrlPath.TOKEN_GET + "/" + outFileName;
		URIUtils.appendPath(builder, p);
		builder.addParameter("p", path);
		return builder.build();
	}
	
	public static class FileUrlPath extends UrlPathTokens {
		public final static String TOKEN_LINK = "link";
		public final static String TOKEN_GET = "get";
			
		public FileUrlPath(String remainingPath) {
			super(StringUtils.split(remainingPath, "/", 3));
		}
		
		public String getLinkId() {
			return getTokenAt(0);
		}
		
		public boolean isGet() {
			return StringUtils.equals(getTokenAt(1), FileUrlPath.TOKEN_GET);
		}
		
		public String getOutFileName() {
			return getTokenAt(2);
		}
	}
}
