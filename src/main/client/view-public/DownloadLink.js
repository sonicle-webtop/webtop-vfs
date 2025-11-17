/* 
 * Copyright (C) 2025 Sonicle S.r.l.
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
 * display the words "Copyright (C) 2025 Sonicle S.r.l.".
 */
Ext.define('Sonicle.webtop.vfs.view-public.DownloadLink', {
	extend: 'WTA.sdk.BaseViewPublic',
	requires: [
		'Sonicle.Data',
		'Sonicle.grid.column.Action',
		'Sonicle.grid.column.Bytes',
		'Sonicle.grid.column.Icon',
		'Sonicle.grid.column.Link',
		'Sonicle.toolbar.PathBreadcrumb',
		'Sonicle.webtop.vfs.model-public.GridFile'
	],
	
	updateLayoutHack: true,
	layout: 'vbox',
	defaults: {
		width: '100%'
	},
	
	initComponent: function() {
		var me = this;
		me.callParent(arguments);
		
		me.add([
			{
				xtype: 'toolbar',
				border: false,
				items: [
					{
						xtype: 'tbtext',
						text: me.res('pub.downloadLink.tit', me.mys.getVar('linkName')),
						cls: 'wt-form-toolbar-title wttasks-public-main-files-grid-title'
					}
				]
			}, {
				xtype: 'toolbar',
				cls: 'wttasks-public-main-files-grid-toolbar',
				border: false,
				items: [
					{
						xtype: 'button',
						ui: '{secondary}',
						iconCls: me.mys.cssIconCls('goUp'),
						tooltip: me.res('act-goUp.tip'),
						handler: function() {
							var ppath = me.lref('bcpath').getParentPath();
							if (ppath) me.reloadFiles(ppath);
						}
					}, {
						xtype: 'sopathbreadcrumb',
						reference: 'bcpath',
						overflowHandler: 'scroller',
						rootFolderIconCls: 'wtvfs-icon-rootfolder',
						folderIconCls: 'wt-ftype-folder',
						path: '/',
						listeners: {
							pathclick: function(s, path) {
								me.reloadFiles(path);
							}
						},
						flex: 1
					},
					'->', 
					{
						xtype: 'button',
						ui: '{primary}',
						text: me.res('pub.downloadLink.act-downloadAll.lbl'),
						iconCls: me.mys.cssIconCls('downloadFile'),
						handler: function() {
							me.openDlUrl('/');
						}
					}
				]
			},
			me.createGridCfg({
				reference: 'gpfiles',
				componentCls: 'wtvfs-public-main-files-grid',
				border: true,
				flex: 1
			})
		]);
		
		me.on('afterrender', function() {
			me.reloadFiles('/');
		});
	},
	
	reloadFiles: function(path) {
		var me = this,
			gp = me.lref('gpfiles'),
			sto = gp.getStore(),
			ep = sto.getProxy().getExtraParams();
		
		if (ep.fileId !== path) {
			Sonicle.Data.loadWithExtraParams(sto, {fileId: path});
		}
	},
	
	openDlUrl: function(fileId) {
		var url = Ext.String.urlAppend(window.location.href, Ext.Object.toQueryString({fileId: fileId, dl:1}));
		Sonicle.URLMgr.open(url);
	},
	
	privates: {
		
		createGridCfg: function(cfg) {
			var me = this;
			return Ext.merge({
				xtype: 'grid',
				store: {
					model: 'Sonicle.webtop.vfs.model-public.GridFile',
					proxy: WTF.proxy(me.mys.ID, 'PreviewFiles', 'files', {
						extraParams: {
							domainId: me.mys.getVar('domainId'),
							linkId: me.mys.getVar('linkId'),
							fileId: null
						}
					}),
					listeners: {
						load: function(s,recs,success) {
							if (success) {
								var ep = s.getProxy().getExtraParams();
								me.lref('bcpath').setPath(ep.fileId);
							}
						}
					}
				},
				viewConfig: {
					deferEmptyText: false,
					emptyText: me.res('gpfiles.emp')
				},
				selModel: {
					type: 'rowmodel'
				},
				columns: [
					{
						xtype: 'soiconcolumn',
						dataIndex: 'type',
						getIconCls: function(v,rec) {
							return (v === 'folder') ? 'wt-ftype-folder' : WTF.fileTypeCssIconCls(rec.get('ext'));
						},
						iconSize: WTU.imgSizeToPx('xs'),
						text: WTF.headerWithGlyphIcon('wt-glyph-file'),
						sortable: false,
						hideable: false,
						width: 35
					}, {
						xtype: 'solinkcolumn',
						dataIndex: 'name',
						tdCls: 'wt-color-hyperlink',
						listeners: {
							linkclick: function(s,idx,rec) {
								if (rec.get('type') === 'folder') {
									me.reloadFiles(rec.get('fileId'));
								}
							}
						},
						text: me.res('gpfiles.name.lbl'),
						flex: 1
					}, {
						xtype: 'sobytescolumn',
						dataIndex: 'size',
						text: me.res('gpfiles.size.lbl'),
						minWidth: 80,
						maxWidth: 130,
						flex: 1
					}, {
						xtype: 'datecolumn',
						dataIndex: 'lastModified',
						text: me.res('gpfiles.lastModified.lbl'),
						format: 'D, d M Y H:i:s',
						minWidth: 150,
						maxWidth: 200,
						flex: 1
					}, {
						xtype: 'soactioncolumn',
						items: [
							{
								iconCls: me.mys.cssIconCls('downloadFile'),
								tooltip: me.res('act-downloadFile.lbl'),
								handler: function(g, ridx) {
									var rec = g.getStore().getAt(ridx);
									me.openDlUrl(rec.get('fileId'));
								}
							}
						],
						menuText: WT.res('grid.actions.lbl')
					}
				],
				listeners: {
					afterlayout: function() {
						/*
						var me = this,
								panel = me.up('panel'),
								headerCt = me.getView().getHeaderCt(),
								curWidth = Ext.getBody().getWidth(),
								height = curWidth > 1025 ? 600 : Ext.getBody().getHeight();

						if (curWidth <= 768) {
							headerCt.getHeaderAtIndex(2).hide();
							headerCt.getHeaderAtIndex(3).hide();
						} else {
							headerCt.getHeaderAtIndex(2).show();
							headerCt.getHeaderAtIndex(3).show();
						}
						panel.setHeight(height);
						panel.setMaxHeight(height);
						*/
					}
				}
			}, cfg);
		}
	}
});