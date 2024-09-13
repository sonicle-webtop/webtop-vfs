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
Ext.define('Sonicle.webtop.vfs.view.NextcloudWiz', {
	extend: 'WTA.sdk.WizardView',
	requires: [
		'WTA.ux.panel.Form',
		'Sonicle.FakeInput',
		'Sonicle.webtop.vfs.store.NextcloudScheme'
	],
	
	dockableConfig: {
		title: '{nextcloudWiz.tit}',
		iconCls: 'wtvfs-icon-storeNextcloud',
		width: 450,
		height: 340
	},
	useTrail: true,
	
	viewModel: {
		data: {
			profileId: null,
			scheme: null,
			host: null,
			port: null,
			username: null,
			password: null,
			path: null,
			name: null
		}
	},
	
	initComponent: function() {
		var me = this,
				ic = me.getInitialConfig();
		if (!Ext.isEmpty(ic.profileId)) me.getVM().set('profileId', ic.profileId);
		if (!Ext.isEmpty(ic.scheme)) me.getVM().set('scheme', ic.scheme);
		if (!Ext.isEmpty(ic.host)) me.getVM().set('host', ic.host);
		if (!Ext.isEmpty(ic.port)) me.getVM().set('port', ic.port);
		if (!Ext.isEmpty(ic.username)) me.getVM().set('username', ic.username);
		if (!Ext.isEmpty(ic.password)) me.getVM().set('password', ic.password);
		if (!Ext.isEmpty(ic.path)) me.getVM().set('path', ic.path);
		me.callParent(arguments);
		me.on('beforenavigate', me.onBeforeNavigate);
	},
	
	initPages: function() {
		return ['s1','s2','s3'];
	},
	
	createPages: function(path) {
		var me = this;
		return [{
			itemId: 's1',
			xtype: 'wtwizardpage',
			items: [{
				xtype: 'label',
				html: me.mys.res('nextcloudWiz.s1.tit'),
				cls: 'x-window-header-title-default'
			}, {
				xtype: 'sospacer'
			}, {
				xtype: 'label',
				html: me.mys.res('nextcloudWiz.s1.txt')
			}, {
				xtype: 'sospacer'
			}, {
				xtype: 'wtform',
				defaults: {
					labelWidth: 80
				},
				items: [
				WTF.lookupCombo('id', 'desc', {
					bind: '{scheme}',
					allowBlank: false,
					store: Ext.create('Sonicle.webtop.vfs.store.NextcloudScheme', {
						autoLoad: true
					}),
					fieldLabel: me.mys.res('nextcloudWiz.fld-scheme.lbl'),
					width: 350
				}), {
					xtype: 'fieldcontainer',
					layout: 'hbox',
					items: [{
						xtype: 'textfield',
						bind: '{host}',
						allowBlank: false,
						inputType: 'url',
						width: 160
					}, {
						xtype: 'displayfield',
						value: '&nbsp;:&nbsp;'
					}, {
						xtype: 'numberfield',
						bind: '{port}',
						//inputType: 'number',
						hideTrigger: true,
						minValue: 1,
						maxValue: 65000,
						width: 60,
						emptyText: me.mys.res('nextcloudWiz.fld-port.lbl')
					}],
					fieldLabel: me.mys.res('nextcloudWiz.fld-host.lbl')
				}, {
					xtype: 'sofakeinput' // Disable Chrome autofill
				}, {
					xtype: 'sofakeinput', // Disable Chrome autofill
					type: 'password'
				}, {
					xtype: 'textfield',
					bind: '{username}',
					width: 280,
					emptyText: me.mys.res('nextcloudWiz.fld-username.emp'),
					fieldLabel: me.mys.res('nextcloudWiz.fld-username.lbl'),
					plugins: 'sonoautocomplete'
				}, {
					xtype: 'textfield',
					bind: '{password}',
					inputType: 'password',
					width: 280,
					emptyText: me.mys.res('nextcloudWiz.fld-password.emp'),
					fieldLabel: me.mys.res('nextcloudWiz.fld-password.lbl'),
					plugins: 'sonoautocomplete'
				}, {
					xtype: 'textfield',
					bind: '{path}',
					allowBlank: false,
					fieldLabel: me.mys.res('nextcloudWiz.fld-path.lbl'),
					emptyText: '/nextcloud/remote.php/dav/files/USERNAME/',
					anchor: '100%'
				}]
			}]
		}, {
			itemId: 's2',
			xtype: 'wtwizardpage',
			items: [{
				xtype: 'label',
				html: me.mys.res('nextcloudWiz.s2.tit'),
				cls: 'x-window-header-title-default'
			}, {
				xtype: 'sospacer'
			}, {
				xtype: 'label',
				html: me.mys.res('nextcloudWiz.s2.txt')
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
					fieldLabel: me.mys.res('nextcloudWiz.fld-name.lbl')
				}]
			}]
		}, {
			itemId: 's3',
			xtype: 'wtwizardpage',
			items: [{
				xtype: 'label',
				html: me.mys.res('nextcloudWiz.s3.tit'),
				cls: 'x-window-header-title-default'
			}, {
				xtype: 'sospacer'
			}, {
				xtype: 'label',
				html: me.mys.res('nextcloudWiz.s3.txt')
			}]
		}];
	},
	
	onBeforeNavigate: function(s, dir, np, pp) {
		if (dir === -1) return;
		var me = this,
				ret = true,
				ppcmp = me.getPageCmp(pp),
				vm = me.getVM();
		
		if (pp === 's1') {
			ret = ppcmp.down('wtform').isValid();
			if (!ret) return false;
			
			WT.ajaxReq(me.mys.ID, 'SetupStoreNextcloud', {
				params: {
					crud: 's1',
					profileId: vm.get('profileId'),
					scheme: vm.get('scheme'),
					host: vm.get('host'),
					port: vm.get('port'),
					username: vm.get('username'),
					password: vm.get('password'),
					path: vm.get('path')
				},
				callback: function(success, json) {
					if (success) {
						vm.set('name', json.data.name);
						me.onNavigate(np);
					} else {
						WT.error(json.message);
					}
				}
			});
			return false;
			
		} else if(pp === 's2') {
			ret = ppcmp.down('wtform').isValid();
			if (!ret) return false;
			
			WT.ajaxReq(me.mys.ID, 'SetupStoreNextcloud', {
				params: {
					crud: 's2',
					profileId: vm.get('profileId'),
					name: vm.get('name')
				},
				callback: function(success, json) {
					if (success) {
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
