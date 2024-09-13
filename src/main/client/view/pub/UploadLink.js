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
Ext.define('Sonicle.webtop.vfs.view.pub.UploadLink', {
	extend: 'WTA.ux.panel.Panel',
	requires: [
		'Sonicle.plugin.DropMask',
		'Sonicle.upload.Button',
		'WTA.ux.UploadBar',
		'WTA.ux.panel.Panel',
		'WTA.ux.panel.UploadedGrid'
	],
	
	layout: 'center',
	referenceHolder: true,
	
	mys: null,
	
	initComponent: function() {
		var me = this,
				gpuploadsId = Ext.id(null, 'gridpanel'),
				btnupl;
		me.callParent(arguments);
		
		btnupl = Ext.create({
			xtype: 'souploadbutton',
			reference: 'btnupl',
			text: me.mys.res('pub.uploadLink.act-uploadFiles.lbl'),
			iconCls: me.mys.cssIconCls('uploadFile'),
			uploaderConfig: WTF.uploader(me.mys.ID, 'UploadFile', {
				dropElement: gpuploadsId,
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
		});
		
		me.add({
			xtype: 'panel',
			layout: 'border',
			border: true,
			title: me.mys.res('pub.uploadLink.tit', me.mys.getVar('linkName')),
			iconCls: me.mys.cssIconCls('uploadLink'),
			items: [
				{
					region: 'center',
					xtype: 'wtuploadedgrid',
					id: gpuploadsId,
					reference: 'gpuploads',
					store: btnupl.uploader.getStore(),
					plugins: [
						{
							ptype: 'sodropmask',
							text: WT.res('sofiledrop.text'),
							monitorExtDrag: false,
							shouldSkipMasking: function(dragOp) {
								return !Sonicle.plugin.DropMask.isBrowserFileDrag(dragOp);
							}
						}
					],
					tbar: [
						btnupl,
						{
							xtype: 'progressbar',
							reference: 'pboverall',
							hidden: true,
							flex: 1
						},
						' '
					]
				}
			],
			listeners: {
				afterlayout: function() {
					var me = this,
							height = Ext.getBody().getWidth() > 800 ? 600 : Ext.getBody().getHeight();
					me.setHeight(height);
					me.setMaxHeight(height);
				}
			},
			width: '100%',
			maxWidth: 800,
			height: '100%',
			maxHeight: Ext.getBody().getWidth() > 800 ? 600 : Ext.getBody().getHeight()
		});
	},
	
	pbOverall: function() {
		return this.lref('pboverall');
	}
});
