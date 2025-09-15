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
Ext.define('Sonicle.webtop.vfs.view.SharingLinks', {
	extend: 'WTA.sdk.DockableView',
	requires: [
		'Sonicle.grid.column.Icon',
		'Sonicle.webtop.vfs.model.GridSharingLink'
	],
	
	dockableConfig: {
		title: '{sharingLinks.tit}',
		iconCls: 'wtvfs-icon-sharingLinks',
		width: 1000,
		height: 450
	},
	promptConfirm: false,
	
	initComponent: function() {
		var me = this;
		me.callParent(arguments);
		
		me.add({
			region: 'center',
			xtype: 'grid',
			reference: 'gp',
			border: false,
			store: {
				autoLoad: true,
				model: 'Sonicle.webtop.vfs.model.GridSharingLink',
				proxy: WTF.apiProxy(me.mys.ID, 'ManageSharingLink', 'sharingLinks')
			},
			viewConfig: {
				getRowClass: function (rec) {
					return rec.get('expired') ? 'wtvfs-gplinks-row-expired' : '';
				}
			},
			columns: [
				{
					xtype: 'rownumberer'	
				}, {
					xtype: 'soiconcolumn',
					dataIndex: 'linkType',
					getIconCls: function(v,rec) {
						var exp = rec.get('expired') ? '-expired' : '-active';
						return me.mys.cssIconCls((v === 'D') ? 'downloadLink'+exp : 'uploadLink'+exp);
					},
					getTip: function(v,rec) {
						var exp = rec.get('expired') ? '.exp' : '';
						return me.res('sharingLinks.gp.linkType.'+v+exp);
					},
					iconSize: WTU.imgSizeToPx('xs'),
					header: WTF.headerWithGlyphIcon('wt-glyph-link'),
					width: 40
				}, {
					xtype: 'soiconcolumn',
					dataIndex: 'storeName',
					getIconCls: function(v,rec) {
						return me.mys.cssIconCls(rec.get('storeIcon'));
					},
					iconSize: WTU.imgSizeToPx('xs'),
					hideText: false,
					text: me.res('sharingLinks.gp.store.lbl'),
					width: 200
				}, {
					xtype: 'soiconcolumn',
					dataIndex: 'fileName',
					getIconCls: function(v,rec) {
						var ext = rec.get('fileExt');
						return Ext.isEmpty(ext) ? 'wt-ftype-folder' : WTF.fileTypeCssIconCls(ext);
					},
					iconSize: WTU.imgSizeToPx('xs'),
					hideText: false,
					text: me.res('sharingLinks.gp.fileName.lbl'),
					flex: 1
				}, {
					dataIndex: 'parentFilePath',
					header: me.res('sharingLinks.gp.parentPath.lbl'),
					flex: 1
				}, {
					xtype: 'datecolumn',
					dataIndex: 'expireOn',
					renderer: function(val, meta, rec, rIdx, colIdx, sto) {
						var s = '';
						if (Ext.isDate(val)) {
							s += Ext.Date.format(val, WT.getShortDateFmt());
						}
						if (rec.get('expired') === true) {
							s += '&nbsp;' + me.res('sharingLinks.gp.expireOn.expired');
						}
						return s;
					},
					text: me.res('sharingLinks.gp.expireOn.lbl'),
					format: WT.getShortDateFmt(),
					width: 150
				}, {
					xtype: 'templatecolumn',
					header: me.res('sharingLinks.gp.user.lbl'),
					tpl: '{userDescription} ({userId})',
					flex: 1
				}, {
					xtype: 'soactioncolumn',
					menuText: WT.res('grid.actions.lbl'),
					items: [
						{
							iconCls: 'wt-glyph-menu-kebab',
							handler: function(view, ridx, cidx, itm, e, rec) {
								view.setSelection(rec);
								Sonicle.Utils.showContextMenu(e, me.getRef('cxmGrid'));
							}
						}
					],
					draggable: true,
					hideable: true
				}
			],
			tbar: [
				'->',
				me.addAct('refresh', {
					text: '',
					tooltip: WT.res('act-refresh.lbl'),
					iconCls: 'wt-icon-refresh',
					handler: function() {
						me.lref('gp').getStore().load();
					}
				})
			],
			listeners: {
				rowdblclick: function(s, rec) {
					me.editLinkUI(rec);
				},
				rowcontextmenu: function(s, rec, itm, i, e) {
					Sonicle.Utils.showContextMenu(e, me.getRef('cxmGrid'));
				}
			}
		});
		
		me.initCxm();
	},
	
	initCxm: function() {
		var me = this;
		me.addRef('cxmGrid', Ext.create({
			xtype: 'menu',
			items: [
				me.addAct('adit', {
					text: WT.res('act-edit.lbl'),
					tooltip: null,
					iconCls: 'wt-icon-edit',
					handler: function() {
						var sel = me.getSelectedLinks();
						if (sel.length === 1) me.editLinkUI(sel[0]);
					}
				}),
				'-',
				me.addAct('remove', {
					text: WT.res('act-remove.lbl'),
					tooltip: null,
					iconCls: 'wt-icon-delete',
					userCls: 'wt-dangerzone',
					handler: function() {
						var sel = me.getSelectedLinks();
						if (sel.length === 1) me.deleteLinkUI(sel[0]);
					}
				})
			]
		}));
	},
	
	editLinkUI: function(rec) {
		var me = this,
			linkId = rec.get('linkId');
		me.mys.editLink(linkId, {
			callback: function(success) {
				if(success) {
					me.lref('gp').getStore().load();
					me.fireEvent('linkupdate', me, rec.get('linkType'), linkId, rec.get('parentPath'));
				}
			}
		});
	},
	
	deleteLinkUI: function(rec) {
		var me = this,
			linkId = rec.get('linkId'),
			type = rec.get('linkType');
		WT.confirmOk(me.res('sharingLink.confirm.delete.'+type), function(bid) {
			if (bid === 'ok') {
				me.mys.deleteLink(linkId, {
					callback: function(success) {
						if (success) {
							me.lref('gp').getStore().remove(rec);
							me.fireEvent('linkupdate', me, type, linkId, rec.get('parentPath'));
						}
					}
				});
			}
		}, me, {
			title: me.res('sharingLink.confirm.delete.'+type+'.tit'),
			okText: me.res('sharingLink.confirm.delete.ok')
		});
	},
	
	privates: {
		getSelectedLinks: function() {
			return this.lref('gp').getSelection();
		}
	}
});
