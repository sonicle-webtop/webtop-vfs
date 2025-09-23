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
Ext.define('Sonicle.webtop.vfs.view-public.Authenticate', {
	extend: 'WTA.sdk.BaseViewPublic',
	requires: [
		'Sonicle.FakeInput',
		'Sonicle.form.Label',
		'Sonicle.form.Spacer',
		'Sonicle.form.field.Password'
	],
	
	updateLayoutHack: true,
	layout: 'center',
	
	initComponent: function() {
		var me = this;
		me.callParent(arguments);
		
		me.add({
			xtype: 'wtfieldspanel',
			title: me.res('pub.authenticate.tit'),
			frame: true,
			paddingTop: true,
			paddingBottom: true,
			paddingSides: true,
			defaults: {
				labelWidth: 90
			},
			items: [
				{
					xtype: 'solabel',
					cls: 'wt-form-body-title wt-theme-text-color-title',
					html: me.res('pub.authenticate.body.tit')
				}, {
					xtype: 'displayfield',
					hideEmptyLabel: true,
					value: me.res('pub.authenticate.body.txt')
				}, {
					xtype: 'sospacer'
				}, {
					xtype: 'sofakeinput', // Disable Chrome autofill
					type: 'password'
				}, {
					xtype: 'sopasswordfield',
					reference: 'fldpassword',
					allowBlank: false,
					listeners: {
						specialkey: function(s, e) {
							if (e.getKey() === e.ENTER) {
								me.lref('btnconfirm').click(e);
							}
						}
					},
					fieldLabel: me.res('pub.authenticate.fld-password.lbl'),
					emptyText: me.res('pub.authenticate.fld-password.emp'),
					anchor: '100%'
				}/*, {
					xtype: 'sospacer'
				}, {
					xtype: 'container',
					layout: {
						type: 'hbox',
						pack: 'end'
					},
					items: [
						{
							
						}
					],
					anchor: '100%'
				}*/
			],
			buttons: [
				{
					xtype: 'button',
					ui: '{primary|default}',
					reference: 'btnconfirm',
					text: WT.res('act-confirm.lbl'),
					handler: function() {
						var pass = me.lref('fldpassword');
						if (pass.isValid()) {
							WT.ajaxReq(me.mys.ID, 'AuthorizeLink', {
								params: {
									domainId: me.mys.getVar('domainId'),
									linkId: me.mys.getVar('linkId'),
									password: pass.getValue()
								},
								callback: function(success, json) {
									if (success) {
										WT.reload();
									} else {
										WT.error(me.res('pub.authenticate.error.badpassword'));
									}
								}
							});
						}
					}
				}
			],
			width: '100%',
			maxWidth: 450,
			minWidth: 250
		});
	},
	
	afterRender: function() {
		var me = this, cmp;
		me.callParent(arguments);
		
		cmp = me.lref('fldpassword');
		if (cmp) cmp.focus(true, 200);
	}
});