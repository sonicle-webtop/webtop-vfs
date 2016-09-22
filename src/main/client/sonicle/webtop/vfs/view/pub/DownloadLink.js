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
Ext.define('Sonicle.webtop.vfs.view.pub.DownloadLink', {
	extend: 'WT.ux.panel.Panel',
	requires: [
		'Sonicle.grid.column.Bytes',
		'Sonicle.grid.column.Icon',
		'Sonicle.grid.column.Link',
		'Sonicle.toolbar.PathBreadcrumb',
		'Sonicle.webtop.vfs.model.pub.GridFile'
	],
	
	layout: 'center',
	referenceHolder: true,
	
	mys: null,
	
	initComponent: function() {
		var me = this;
		me.callParent(arguments);
		
		me.add({
			xtype: 'panel',
			layout: 'border',
			border: false,
			width: '100%',
			maxWidth: 900,
			height: '100%',
			maxHeight: 500,
			title: me.mys.res('pub.downloadLink.tit', me.mys.getVar('linkName')),
			iconCls: me.mys.cssIconCls('downloadLink', 'xs'),
			items: [{
				region: 'center',
				xtype: 'grid',
				reference: 'gpfiles',
				cls: 'wtvfs-gpfiles',
				border: true,
				store: {
					model: 'Sonicle.webtop.vfs.model.pub.GridFile',
					proxy: WTF.proxy(me.mys.ID, 'PreviewFiles', 'files', {
						extraParams: {
							linkId: me.mys.getVar('linkId'),
							fileId: null
						}
					}),
					listeners: {
						load: function(s,recs,success) {
							if(success) {
								var ep = s.getProxy().getExtraParams();
								me.lref('bcpath').setPath(ep.fileId);
							}
						}
					}
				},
				viewConfig: {
					deferEmptyText: false,
					emptyText: me.mys.res('gpfiles.emptyText')
				},
				selModel: {
					type: 'rowmodel'
				},
				columns: [{
					xtype: 'soiconcolumn',
					dataIndex: 'type',
					header: WTF.headerWithGlyphIcon('fa fa-file-o'),
					getIconCls: function(v,rec) {
						return (v === 'folder') ? 'wt-ftype-folder-xs' : WTF.fileTypeCssIconCls(rec.get('ext'), 'xs');
					},
					iconSize: WTU.imgSizeToPx('xs'),
					width: 40
				}, {
					xtype: 'solinkcolumn',
					dataIndex: 'name',
					header: me.mys.res('gpfiles.name.lbl'),
					flex: 1,
					listeners: {
						linkclick: function(s,idx,rec) {
							if(rec.get('type') === 'folder') {
								me.reloadFiles(rec.get('fileId'));
							}
						}
					},
					maxWidth: 500
				}, {
					xtype: 'sobytescolumn',
					dataIndex: 'size',
					header: me.mys.res('gpfiles.size.lbl'),
					width: 110
				}, {
					dataIndex: 'lastModified',
					header: me.mys.res('gpfiles.lastModified.lbl'),
					xtype: 'datecolumn',
					format: 'D, d M Y H:i:s',
					width: 200
				}, {
					xtype: 'actioncolumn',
					header: '&nbsp;',
					flex: 1,
					items: [{
						iconCls: me.mys.cssIconCls('downloadFile', 'xs'),
						tooltip: me.mys.res('act-downloadFile.lbl'),
						handler: function(s, rIndex) {
							var rec = s.getStore().getAt(rIndex), qsp;
							me.openDlUrl(rec.get('fileId'));
						}
					}]
				}],
				tbar: [{
					xtype: 'button',
					iconCls: me.mys.cssIconCls('goUp', 'xs'),
					tooltip: me.mys.res('act-goUp.tip'),
					handler: function() {
						var ppath = me.lref('bcpath').getParentPath();
						if(ppath) me.reloadFiles(ppath);
					}
				}, {
					xtype: 'sopathbreadcrumb',
					reference: 'bcpath',
					overflowHandler: 'scroller',
					rootFolderIconCls: 'wt-ftype-folderroot-xs',
					folderIconCls: 'wt-ftype-folder-xs',
					path: '/',
					listeners: {
						pathclick: function(s, path) {
							me.reloadFiles(path);
						}
					},
					flex: 1
				}]
			}],
			tbar: [
				'->', {
				xtype: 'button',
				text: me.mys.res('pub.downloadLink.act-downloadAll.lbl'),
				iconCls: me.mys.cssIconCls('downloadFile', 'xs'),
				handler: function() {
					me.openDlUrl('/');
				}
			}]
		});
		
		me.on('afterrender', function() {
			me.reloadFiles('/');
		});
	},
	
	reloadFiles: function(path) {
		var me = this,
				gp = me.lref('gpfiles'),
				sto = gp.getStore(),
				ep = sto.getProxy().getExtraParams();
		
		if(ep.fileId !== path) {
			WTU.loadWithExtraParams(sto, {fileId: path});
		}
	},
	
	openDlUrl: function(fileId) {
		var url = Ext.String.urlAppend(window.location.href, Ext.Object.toQueryString({fileId: fileId, dl:1}));
		Sonicle.URLMgr.open(url);
	}
});
