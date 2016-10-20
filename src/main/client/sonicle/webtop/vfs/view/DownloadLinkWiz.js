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
Ext.define('Sonicle.webtop.vfs.view.DownloadLinkWiz', {
	extend: 'WT.sdk.WizardView',
	requires: [
		'Sonicle.form.field.Link',
		'WT.ux.panel.Form'
	],
	
	dockableConfig: {
		title: '{downloadLinkWiz.tit}',
		iconCls: 'wtvfs-icon-downloadLink-xs',
		width: 420,
		height: 220
	},
	useTrail: true,
	
	viewModel: {
		data: {
			profileId: null,
			fileId: null,
			expire: true,
			expirationDate: null,
			authMode: 'P',
			password: null,
			url: null,
			rawUrl: null,
			result: null
		}
	},
	
	constructor: function(cfg) {
		var me = this;
		me.callParent([cfg]);
		
		WTU.applyFormulas(me.getVM(), {
			foExpire: WTF.radioGroupBind('', 'expire', me.sufId('expire')),
			foAuthMode: WTF.radioGroupBind('', 'authMode', me.sufId('authMode')),
			foAuthModeIsP: WTF.equalsFormula('', 'authMode', 'P'),
			foIsUrlEmpty: WTF.isEmptyFormula('', 'url'),
			foIsRawUrlEmpty: WTF.isEmptyFormula('', 'rawUrl')
		});
	},
	
	initComponent: function() {
		var me = this,
				ic = me.getInitialConfig(),
				vm = me.getVM();
		
		if(Ext.isEmpty(ic.fileId)) Ext.Error.raise('Provide a value for fileId');
		vm.set('fileId', ic.fileId);
		vm.set('expirationDate', Sonicle.Date.add(new Date(), {days: me.mys.getVar('downloadLinkExpiration')}));
		
		me.callParent(arguments);
		me.on('beforenavigate', me.onBeforeNavigate);
		vm.bind('{expire}', me.onExpireChanged, me);
		vm.bind('{authMode}', me.onAuthModeChanged, me);
	},
	
	onExpireChanged: function(v) {
		this.lref('s1.fldexpirationdate').allowBlank = !v;
	},
	
	onAuthModeChanged: function(v) {
		this.lref('s2.fldpassword').allowBlank = !(v === 'P');
	},
	
	initPages: function() {
		return ['s1','s2','s3'];
	},
	
	createPages: function(path) {
		var me = this;
		return [{
			itemId: 's1',
			xtype: 'wtwizardpage',
			reference: 's1',
			defaults: {
				labelWidth: 120
			},
			items: [{
				xtype: 'label',
				html: me.mys.res('downloadLinkWiz.s1.tit'),
				cls: 'x-window-header-title-default'
			}, {
				xtype: 'sospacer'
			}, {
				xtype: 'wtform',
				items: [{
					xtype: 'radiogroup',
					layout: 'vbox',
					bind: {
						value: '{foExpire}'
					},
					items: [{
						xtype: 'radio',
						name: me.sufId('expire'),
						inputValue: false,
						boxLabel: me.mys.res('downloadLinkWiz.fld-expire.false')
					}, {
						xtype: 'fieldcontainer',
						layout: 'hbox',
						items: [{
							xtype: 'radio',
							name: me.sufId('expire'),
							inputValue: true,
							boxLabel: me.mys.res('downloadLinkWiz.fld-expire.true')
						}, {
							xtype: 'sospacer',
							vertical: false
						}, {
							xtype: 'datefield',
							reference: 'fldexpirationdate',
							bind: {
								value: '{expirationDate}',
								disabled: '{!expire}'
							},
							startDay: WT.getStartDay(),
							format: WT.getShortDateFmt(),
							width: 120
						}]
					}]
				}]
			}]
		}, {
			itemId: 's2',
			xtype: 'wtwizardpage',
			reference: 's2',
			defaults: {
				labelWidth: 120
			},
			items: [{
				xtype: 'label',
				html: me.mys.res('downloadLinkWiz.s2.tit'),
				cls: 'x-window-header-title-default'
			}, {
				xtype: 'sospacer'
			}, {
				xtype: 'wtform',
				defaults: {
					labelWidth: 80
				},
				items: [{
					xtype: 'radiogroup',
					layout: 'vbox',
					bind: {
						value: '{foAuthMode}'
					},
					items: [{
						xtype: 'radio',
						name: me.sufId('authMode'),
						inputValue: 'N',
						boxLabel: me.mys.res('store.sharingLinkAuthMode.N')
					}, {
						xtype: 'fieldcontainer',
						layout: 'hbox',
						items: [{
							xtype: 'radio',
							name: me.sufId('authMode'),
							inputValue: 'P',
							boxLabel: me.mys.res('store.sharingLinkAuthMode.P')
						}, {
							xtype: 'sospacer',
							vertical: false
						}, {
							xtype: 'textfield',
							reference: 'fldpassword',
							bind: {
								value: '{password}',
								disabled: '{!foAuthModeIsP}'
							},
							width: 200,
							emptyText: me.mys.res('downloadLinkWiz.fld-password.lbl')
						}]
					}]
				}]
			}]
		}, {
			itemId: 's3',
			xtype: 'wtwizardpage',
			items: [{
				xtype: 'label',
				html: me.mys.res('downloadLinkWiz.s3.tit'),
				cls: 'x-window-header-title-default'
			}, {
				xtype: 'sospacer'
			}, {
				xtype: 'label',
				html: me.mys.res('downloadLinkWiz.s3.txt')
			}, {
				xtype: 'wtform',
				defaults: {
					labelWidth: 80
				},
				items: [{
					xtype: 'textfield',
					bind: {
						value: '{url}',
						hidden: '{foIsUrlEmpty}'
					},
					editable: false,
					selectOnFocus: true,
					hidden: true,
					fieldLabel: me.mys.res('downloadLinkWiz.fld-url.lbl'),
					anchor: '100%'
				}, {
					xtype: 'textfield',
					bind: {
						value: '{rawUrl}',
						hidden: '{foIsRawUrlEmpty}'
					},
					editable: false,
					selectOnFocus: true,
					hidden: true,
					fieldLabel: me.mys.res('downloadLinkWiz.fld-rawUrl.lbl'),
					anchor: '100%'
				}]
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
			ret = ppcmp.down('wtform').isValid();
			if(!ret) return false;
			
		} else if(pp === 's2') {
			ret = ppcmp.down('wtform').isValid();
			if(!ret) return false;
			
			WT.ajaxReq(me.mys.ID, 'WizardDownloadLink', {
				params: {
					crud: 's2',
					profileId: vm.get('profileId'),
					fileId: vm.get('fileId'),
					expirationDate: vm.get('expire') ? Ext.Date.format(vm.get('expirationDate'), 'Y-m-d H:i:s') : null,
					authMode: vm.get('authMode'),
					password: (vm.get('authMode') === 'P') ? vm.get('password') : null
				},
				callback: function(success, json) {
					if(success) {
						vm.set('result', json.data);
						vm.set('url', json.data.urls[0]);
						vm.set('rawUrl', json.data.urls[1]);
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
