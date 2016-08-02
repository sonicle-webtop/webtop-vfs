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
Ext.define('Sonicle.webtop.vfs.view.Store', {
	extend: 'WT.sdk.ModelView',
	requires: [
		'Sonicle.webtop.vfs.store.Scheme',
		'Sonicle.webtop.vfs.model.Store'
	],
	
	dockableConfig: {
		title: '{store.tit}',
		iconCls: 'wtvfs-icon-store-xs',
		width: 450,
		height: 260
	},
	fieldTitle: 'name',
	modelName: 'Sonicle.webtop.vfs.model.Store',
	
	initComponent: function() {
		var me = this;
		me.callParent(arguments);
		
		me.add({
			region: 'center',
			xtype: 'wtform',
			modelValidation: true,
			defaults: {
				labelWidth: 100
			},
			items: [{
				xtype: 'textfield',
				reference: 'fldname',
				bind: '{record.name}',
				fieldLabel: me.mys.res('store.fld-name.lbl'),
				anchor: '100%'
			},
			WTF.lookupCombo('id', 'desc', {
				bind: '{record.scheme}',
				allowBlank: false,
				store: Ext.create('Sonicle.webtop.vfs.store.Scheme', {
					autoLoad: true
				}),
				fieldLabel: me.mys.res('store.fld-scheme.lbl'),
				width: 350,
				disabled: true
			}), {
				xtype: 'fieldcontainer',
				layout: 'hbox',
				items: [{
					xtype: 'textfield',
					bind: '{record.host}',
					allowBlank: false,
					inputType: 'url',
					width: 160
				}, {
					xtype: 'displayfield',
					value: '&nbsp;:&nbsp;'
				}, {
					xtype: 'numberfield',
					bind: '{record.port}',
					inputType: 'number',
					hideTrigger: true,
					minValue: 1,
					maxValue: 65000,
					width: 60,
					emptyText: me.mys.res('store.fld-port.lbl')
				}],
				fieldLabel: me.mys.res('store.fld-host.lbl')
			}, {
				xtype: 'textfield',
				bind: '{record.username}',
				anchor: '80%',
				fieldLabel: me.mys.res('store.fld-username.lbl'),
				plugins: [{
					ptype: 'sonoautocomplete'
				}]
			}, {
				xtype: 'textfield',
				bind: '{record.password}',
				inputType: 'password',
				anchor: '80%',
				fieldLabel: me.mys.res('store.fld-password.lbl'),
				plugins: [{
					ptype: 'sonoautocomplete'
				}]
			}, {
				xtype: 'textfield',
				bind: '{record.path}',
				anchor: '100%',
				fieldLabel: me.mys.res('store.fld-path.lbl')
			}]
		});
		me.on('viewload', me.onViewLoad);
	},
	
	onViewLoad: function(s, success) {
		if(!success) return;
		var me = this;
		me.lref('fldname').focus(true);
	}
});
