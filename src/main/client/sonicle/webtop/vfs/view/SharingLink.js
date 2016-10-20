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
Ext.define('Sonicle.webtop.vfs.view.SharingLink', {
	extend: 'WT.sdk.ModelView',
	requires: [
		'Sonicle.FakeInput',
		'Sonicle.form.Separator',
		'Sonicle.form.Spacer',
		'Sonicle.form.field.Password',
		'Sonicle.webtop.vfs.store.SharingLinkType',
		'Sonicle.webtop.vfs.model.SharingLink'
	],
	
	dockableConfig: {
		title: '{sharingLink.tit}',
		iconCls: 'wtvfs-icon-sharingLink-xs',
		width: 480,
		height: 290
	},
	fieldTitle: 'fileName',
	modelName: 'Sonicle.webtop.vfs.model.SharingLink',
	
	constructor: function(cfg) {
		var me = this;
		me.callParent([cfg]);
		
		WTU.applyFormulas(me.getVM(), {
			foIsUrlEmpty: WTF.isEmptyFormula('record', 'publicUrl'),
			foIsRawUrlEmpty: WTF.isEmptyFormula('record', 'rawPublicUrl'),
			foAuthModeIsP: WTF.equalsFormula('record', 'authMode', 'P')
		});
	},
	
	initComponent: function() {
		var me = this;
		me.callParent(arguments);
		
		me.add({
			region: 'center',
			xtype: 'wtform',
			modelValidation: true,
			defaults: {
				labelWidth: 130
			},
			items: [
			WTF.lookupCombo('id', 'desc', {
				bind: '{record.type}',
				allowBlank: false,
				store: Ext.create('Sonicle.webtop.vfs.store.SharingLinkType', {
					autoLoad: true
				}),
				fieldLabel: me.mys.res('sharingLink.fld-type.lbl'),
				anchor: '100%',
				disabled: true
			}), {
				xtype: 'datefield',
				bind: '{record.expirationDate}',
				startDay: WT.getStartDay(),
				format: WT.getShortDateFmt(),
				triggers: {
					clear: WTF.clearTrigger()
				},
				fieldLabel: me.mys.res('sharingLink.fld-expirationDate.lbl'),
				emptyText: WT.res('word.none.female'),
				width: 280
			}, 
			WTF.lookupCombo('id', 'desc', {
				bind: '{record.authMode}',
				allowBlank: false,
				store: Ext.create('Sonicle.webtop.vfs.store.SharingLinkAuthMode', {
					autoLoad: true
				}),
				fieldLabel: me.mys.res('sharingLink.fld-authMode.lbl'),
				anchor: '100%'
			}), {
				xtype: 'sofakeinput', // Disable Chrome autofill
				type: 'password'
			}, {
				xtype: 'sopasswordfield',
				reference: 'fldpassword',
				bind: {
					value: '{record.password}',
					disabled: '{!foAuthModeIsP}'
				},
				fieldLabel: me.mys.res('sharingLink.fld-password.lbl'),
				anchor: '100%'
			}, {
				xtype: 'sospacer',
				mult: 2
			}, {
				xtype: 'soformseparator'
			}, {
				xtype: 'textfield',
				bind: {
					value: '{record.publicUrl}',
					hidden: '{foIsUrlEmpty}'
				},
				editable: false,
				selectOnFocus: true,
				hidden: true,
				fieldLabel: me.mys.res('sharingLink.fld-publicUrl.lbl'),
				anchor: '100%'
			}, {
				xtype: 'textfield',
				bind: {
					value: '{record.rawPublicUrl}',
					hidden: '{foIsRawUrlEmpty}'
				},
				editable: false,
				selectOnFocus: true,
				hidden: true,
				fieldLabel: me.mys.res('sharingLink.fld-rawPublicUrl.lbl'),
				anchor: '100%'
			}]
		});
	}
});
