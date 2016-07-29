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
Ext.define('Sonicle.webtop.vfs.view.DropboxWiz', {
	extend: 'WT.sdk.WizardView',
	requires: [
		'Sonicle.form.field.Link',
		'WT.ux.panel.Form'
	],
	
	dockableConfig: {
		title: '{dropboxWiz.tit}',
		iconCls: 'wtvfs-icon-storeDropbox-xs',
		width: 460,
		height: 320
	},
	useTrail: true,
	
	viewModel: {
		data: {
			profileId: null,
			url: null,
			code: null,
			name: null
		}
	},
	
	initComponent: function() {
		var me = this,
				ic = me.getInitialConfig();
		if(!Ext.isEmpty(ic.profileId)) me.getVM().set('profileId', ic.profileId);
		me.callParent(arguments);
		me.on('beforenavigate', me.onBeforeNavigate);
	},
	
	initPages: function() {
		return ['s1','s2','s3','s4'];
	},
	
	createPages: function(path) {
		var me = this;
		return [{
			itemId: 's1',
			xtype: 'wtwizardpage',
			items: [{
				xtype: 'label',
				html: me.mys.res('dropboxWiz.s1.tit'),
				cls: 'x-window-header-title-default'
			}, {
				xtype: 'sospacer'
			}, {
				xtype: 'label',
				html: me.mys.res('dropboxWiz.s1.txt')
			}]
		}, {
			itemId: 's2',
			xtype: 'wtwizardpage',
			items: [{
				xtype: 'label',
				html: me.mys.res('dropboxWiz.s2.tit'),
				cls: 'x-window-header-title-default'
			}, {
				xtype: 'sospacer'
			}, {
				xtype: 'label',
				html: me.mys.res('dropboxWiz.s2.txt')
			}, {
				xtype: 'wtform',
				items: [{
					xtype: 'solinkfield',
					bind: '{url}',
					hideLabel: true
				}, {
					xtype: 'sospacer'
				}, {
					xtype: 'textfield',
					bind: '{code}',
					allowBlank: false,
					width: 420,
					emptyText: me.mys.res('dropboxWiz.fld-code.tip'),
					fieldLabel: me.mys.res('dropboxWiz.fld-code.lbl')
				}]
			}]
		}, {
			itemId: 's3',
			xtype: 'wtwizardpage',
			items: [{
				xtype: 'label',
				html: me.mys.res('dropboxWiz.s3.tit'),
				cls: 'x-window-header-title-default'
			}, {
				xtype: 'sospacer'
			}, {
				xtype: 'label',
				html: me.mys.res('dropboxWiz.s3.txt')
			}, {
				xtype: 'sospacer'
			}, {
				xtype: 'wtform',
				defaults: {
					labelWidth: 80
				},
				items: [{
					xtype: 'textfield',
					bind: '{name}',
					allowBlank: false,
					width: 400,
					fieldLabel: me.mys.res('dropboxWiz.fld-name.lbl')
				}]
			}]
		}, {
			itemId: 's4',
			xtype: 'wtwizardpage',
			items: [{
				xtype: 'label',
				html: me.mys.res('dropboxWiz.s4.tit'),
				cls: 'x-window-header-title-default'
			}, {
				xtype: 'sospacer'
			}, {
				xtype: 'label',
				html: me.mys.res('dropboxWiz.s4.txt')
			}]
		}];
	},
	
	onBeforeNavigate: function(s, dir, np, pp) {
		if(dir === -1) return;
		var me = this,
				ret = true,
				ppcmp = me.getPageCmp(pp),
				vm = me.getVM();
		
		if(pp === 's1') {			
			WT.ajaxReq(me.mys.ID, 'SetupStoreDropbox', {
				params: {
					crud: 's1',
					profileId: vm.get('profileId')
				},
				callback: function(success, json) {
					if(success) {
						vm.set('url', json.data.authUrl);
						me.onNavigate(np);
					} else {
						WT.error(json.message);
					}
				}
			});
			return false;
			
		} else if(pp === 's2') {
			ret = ppcmp.down('wtform').isValid();
			if(!ret) return false;
			
			WT.ajaxReq(me.mys.ID, 'SetupStoreDropbox', {
				params: {
					crud: 's2',
					profileId: vm.get('profileId'),
					code: vm.get('code')
				},
				callback: function(success, json) {
					if(success) {
						vm.set('name', json.data.name);
						me.onNavigate(np);
					} else {
						WT.error(json.message);
					}
				}
			});
			return false;
			
		} else if(pp === 's3') {
			ret = ppcmp.down('wtform').isValid();
			if(!ret) return false;
			
			WT.ajaxReq(me.mys.ID, 'SetupStoreDropbox', {
				params: {
					crud: 's3',
					profileId: vm.get('profileId'),
					name: vm.get('name')
				},
				callback: function(success, json) {
					if(success) {
						me.onNavigate(np);
					} else {
						WT.error(json.message);
					}
				}
			});
			return false;
		}
	}
});
