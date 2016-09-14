/*
 * webtop-tasks is a WebTop Service developed by Sonicle S.r.l.
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
Ext.define('Sonicle.webtop.vfs.ux.UploadToolbar', {
	extend: 'Ext.toolbar.Toolbar',
	alias: 'widget.wtvfsuploadtoolbar',
	requires: [
		'Sonicle.Bytes',
		'Sonicle.upload.Button'
	],
	
	/**
	 * @property {WT.sdk.Service} mys
	 * Reference to service instance.
	 */
	mys: null,
	
	/**
	 * @cfg {String} dropElement
	 * The ID of DOM element to be used as the dropzone for the files.
	 */
	dropElement: null,
	
	fileExtraParams: null,
	
	initComponent: function() {
		var me = this,
				SoByt = Sonicle.Bytes,
				maxUpSize = me.mys.getVar('privateUploadMaxFileSize'),
				de = me.dropElement;
		me.callParent(arguments);
		me.add([{
			xtype: 'souploadbutton',
			itemId: 'btnupload',
			text: me.mys.res('tbupload.btn-upload.lbl'),
			tooltip: me.mys.res('tbupload.btn-upload.tip'),
			iconCls: me.mys.cssIconCls('uploadFile', 'xs'),
			uploaderConfig: WTF.uploader(me.mys.ID, 'UploadStoreFile', {
				maxFileSize: maxUpSize,
				dropElement: de ? de : undefined,
				fileExtraParams: function() {
					if(Ext.isFunction(me.fileExtraParams)) {
						return me.fileExtraParams.apply(me);
					} else {
						return null;
					}
				},
				listeners: {
					invalidfilesize: function() {
						WT.warn(WT.res(WT.ID, 'error.upload.sizeexceeded', SoByt.format(maxUpSize)));
					},
					fileuploaded: function(s, file) {
						me.fireEvent('fileuploaded', me, file);
					},
					uploaderror: function(s) {
						WT.error('Upload error');
					},
					overallprogress: function(s, percent, total, succeeded, failed, queued, speed) {
						var pro = me.getProgress(),
								drh = me.geDropHere();
						if(queued > 0) {
							pro.updateProgress(percent*0.01, me.mys.res('tbupload.progress.lbl', percent, queued-1, SoByt.format(speed || 0)));
							pro.setHidden(false);
							drh.setHidden(true);
						} else {
							pro.reset();
							pro.setHidden(true);
							drh.setHidden(false);
						}
					}
				}
			})
		}, {
			xtype: 'progressbar',
			itemId: 'progress',
			hidden: true,
			flex: 1
		}, {
			xtype: 'toolbar',
			itemId: 'drophere',
			cls: 'wtvfs-drophere',
			layout: {
				type: 'hbox',
				pack: 'center'
			},
			border: 1,
			style: {
				borderStyle: 'dashed',
				borderRadius: '4px'
			},
			items: [{
					xtype: 'tbtext',
					cls: 'x-unselectable wtvfs-drophere-text',
					text: me.mys.res('tbupload.drophere.lbl')
			}],
			flex: 1
		}]);
	},
	
	getUploadButton: function() {
		return this.getComponent('btnupload');
	},
	
	getProgress: function() {
		return this.getComponent('progress');
	},
	
	geDropHere: function() {
		return this.getComponent('drophere');
	}
});
