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
Ext.define('Sonicle.webtop.vfs.view.pub.Authorize', {
	extend: 'WT.ux.panel.Panel',
	requires: [
		'Sonicle.FakeInput',
		'Sonicle.form.Label',
		'Sonicle.form.Spacer',
		'Sonicle.form.field.Password'
	],
	
	layout: 'border',
	referenceHolder: true,
	width: 380,
	height: 150,
	
	initComponent: function() {
		var me = this;
		me.callParent(arguments);
		
		me.add({
			region: 'center',
			xtype: 'wtform',
			defaults: {
				labelWidth: 90
			},
			items: [{
				xtype: 'solabel',
				appearance: 'title',
				html: me.mys.res('pub.authorize.tit')
			}, {
				xtype: 'displayfield',
				hideEmptyLabel: true,
				value: me.mys.res('pub.authorize.sub.tit')
			}, {
				xtype: 'sospacer'
			}, {
				xtype: 'sofakeinput', // Disable Chrome autofill
				type: 'password'
			}, {
				xtype: 'sopasswordfield',
				reference: 'fldpassword',
				allowBlank: false,
				fieldLabel: me.mys.res('pub.authorize.fld-password.lbl'),
				anchor: '-20'
			}, {
				xtype: 'sospacer'
			}, {
				xtype: 'container',
				layout: {
					type: 'hbox',
					pack: 'end'
				},
				items: [{
					xtype: 'button',
					text: WT.res('act-confirm.lbl'),
					handler: function() {
						var pass = me.lref('fldpassword');
						if(pass.isValid()) {
							WT.ajaxReq(me.mys.ID, 'AuthorizeLink', {
								params: {
									linkId: me.mys.getVar('linkId'),
									password: pass.getValue()
								},
								callback: function(success, json) {
									if(success) {
										WT.info('ooooookkkkkk');
									} else WT.error('password errata');
								}
							});
						}
					}
				}],
				anchor: '-20'
			}]
		});
	}
});
