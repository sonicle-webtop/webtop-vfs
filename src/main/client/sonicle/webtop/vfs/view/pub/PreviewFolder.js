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
Ext.define('Sonicle.webtop.vfs.view.pub.PreviewFolder', {
	extend: 'WT.ux.panel.Panel',
	requires: [
		'Sonicle.grid.column.Icon',
		'Sonicle.grid.column.Bytes',
		'Sonicle.webtop.vfs.model.pub.GridFile'
		
		//'Sonicle.FakeInput',
		//'Sonicle.form.Label',
		//'Sonicle.form.Spacer',
		//'Sonicle.form.field.Password'
	],
	
	layout: 'border',
	referenceHolder: true,
	
	mys: null,
	
	initComponent: function() {
		var me = this;
		me.callParent(arguments);
		
		me.add({
			region: 'center',
			xtype: 'grid',
			reference: 'gpfiles',
			store: {
				model: 'Sonicle.webtop.vfs.model.pub.GridFile',
				proxy: WTF.proxy(me.mys.ID, 'PreviewFiles', 'files', {
					extraParams: {
						linkId: me.mys.getVar('linkId'),
						fileId: null
					}
				})
			},
			viewConfig: {
				deferEmptyText: false,
				emptyText: me.mys.res('gpfiles.emptyText')
			},
			selModel: {
				type: 'rowmodel'
				//mode : 'MULTI'
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
							//me.setCurFile(rec.get('fileId'));
							//me.reloadGridFiles();
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
				width: 140
			}]/*,
			tbar: [{
				iconCls: me.mys.cssIconCls('goUp', 'xs'),
				tooltip: me.mys.res('act-goUp.tip')
			}]
		*/
		});
	}
});
