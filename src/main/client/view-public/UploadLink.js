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
Ext.define('Sonicle.webtop.vfs.view-public.UploadLink', {
	extend: 'WTA.sdk.BaseViewPublic',
	requires: [
		'Sonicle.Bytes',
		'Sonicle.plugin.DropMask',
		'Sonicle.upload.Button',
		'WTA.ux.UploadBar',
		'WTA.ux.panel.Panel',
		'WTA.ux.panel.UploadedGrid'
	],
	
	layout: 'vbox',
	defaults: {
		width: '100%'
	},
	
	initComponent: function() {
		var me = this,
			gpuploadsId = Ext.id(null, 'gridpanel'),
			btnupl;
		
		me.callParent(arguments);
		
		btnupl = Ext.create(me.createUploadButtonCfg(gpuploadsId));
		me.add([
			{
				xtype: 'toolbar',
				cls: 'wttasks-public-main-files-grid-toolbar',
				border: false,
				items: [
					{
						xtype: 'tbtext',
						text: me.res('pub.uploadLink.tit', me.mys.getVar('linkName')),
						cls: 'wt-form-toolbar-title wttasks-public-main-files-grid-title'
					},
					'->',
					{
						xtype: 'progressbar',
						reference: 'pboverall',
						hidden: true,
						maxWidth: 200,
						width: '10%'
					},
					btnupl
				]
			},
			me.createGridCfg(btnupl.uploader.getStore(), {
				id: gpuploadsId,
				reference: 'gpuploads',
				componentCls: 'wtvfs-public-main-files-grid',
				border: true,
				flex: 1
			})
		]);
	},
	
	pbOverall: function() {
		return this.lref('pboverall');
	},
	
	privates: {
		createUploadButtonCfg: function(dropElementId, cfg) {
			var me = this;
			return Ext.merge({
				xtype: 'souploadbutton',
				reference: 'btnupl',
				ui: '{primary}',
				text: me.res('pub.uploadLink.act-uploadFiles.lbl'),
				iconCls: me.mys.cssIconCls('uploadFile'),
				uploaderConfig: WTF.uploader(me.mys.ID, 'UploadFile', {
					dropElement: dropElementId,
					maxFileSize: me.mys.getVar('publicUploadMaxFileSize'),
					autoRemoveUploaded: false,
					fileExtraParams: function() {
						return {
							domainId: me.mys.getVar('domainId'),
							linkId: me.mys.getVar('linkId')
						};
					},
					listeners: {
						uploaderror: function(s, file, cause) {
							WTA.ux.UploadBar.handleUploadError(s, file, cause);
						},
						overallprogress: function(s, perc, tot, succ, fail, pend, speed) {
							var pro = me.pbOverall();
							if (pend > 0) {
								pro.updateProgress(perc*0.01, WT.res('wtuploadbar.progress.lbl', perc, pend, Sonicle.Bytes.format(speed || 0)));
								pro.setHidden(false);
							} else {
								pro.reset();
								pro.setHidden(true);
							}
						}
					}
				})
			}, cfg);
		},
		
		createGridCfg: function(store, cfg) {
			return Ext.merge({
				xtype: 'wtuploadedgrid',
				store: store,
				plugins: [
					{
						ptype: 'sodropmask',
						text: WT.res('sofiledrop.text'),
						monitorExtDrag: false,
						shouldSkipMasking: function(dragOp) {
							return !Sonicle.plugin.DropMask.isBrowserFileDrag(dragOp);
						}
					}
				]
			}, cfg);
		}
	}
});