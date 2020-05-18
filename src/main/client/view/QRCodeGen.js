/* 
 * Copyright (C) 2020 Sonicle S.r.l.
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
 * display the words "Copyright (C) 2020 Sonicle S.r.l.".
 */
Ext.define('Sonicle.webtop.vfs.view.QRCodeGen', {
	extend: 'WTA.sdk.UIView',
	requires: [
		'Sonicle.form.field.DisplayImage'
	],
	uses: [
		'Sonicle.PrintMgr'
	],
	
	dockableConfig: {
		iconCls: 'wtvfs-icon-qrCodeGen',
		width: 320,
		height: 430
	},
	
	viewModel: {
		data: {
			data: {
				linkId: null,
				size: null,
				color: null
			}
		}
	},
	
	initComponent: function() {
		var me = this,
				ic = me.getInitialConfig(),
				vm = me.getVM();
		
		if (ic.data) {
			vm.set('data', Ext.apply({}, ic.data, {
				size: 250,
				color: '000000'
			}));
		}
		
		Ext.apply(me, {
			buttons: [
				{
					text: WT.res('act-print.lbl'),
					handler: function() {
						var el = me.lref('fldqrcode').inputWrap;
						if (el) Sonicle.PrintMgr.print(el.dom.innerHTML, 'raw', {verticalAlign:'center', horizontalAlign:'center'});
					}
				}, {
					text: WT.res('act-ok.lbl'),
					handler: function() {
						me.closeView(false);
					}
				}
			],
			dockedItems: [
				{
					xtype: 'toolbar',
					dock: 'top',
					items: [
						'->',
						{
							xtype: 'tbtext',
							html: Ext.String.htmlEncode(me.mys.res('qrCodeGen.fld-size.lbl')) + ':'
						}, {
							xtype: 'sliderfield',
							bind: '{data.size}',
							increment: 10,
							minValue: 50,
							maxValue: 400,
							width: 110
						}, /*' ', {
							xtype: 'tbtext',
							html: Ext.String.htmlEncode(me.mys.res('qrCodeGen.fld-color.lbl')) + ':'
						}, {
							xtype: 'colorbutton',
							bind: '{data.color}'
						},
						*/
						'->'
					]
				}
			]
		});
		me.callParent(arguments);
		me.add({
			region: 'center',
			xtype: 'wtpanel',
			layout: 'center',
			items: [{
				xtype: 'sodisplayimagefield',
				reference: 'fldqrcode',
				bind: '{data.linkId}',
				baseImageUrl: WTF.processBinUrl(me.mys.ID, 'GetLinkQRCode'),
				urlExtraParams: {
					size: vm.get('data.size'),
					color: vm.get('data.color')
				},
				imageWidth: vm.get('data.size'),
				imageHeight: vm.get('data.size')
			}]
		});
		vm.bind('{data.size}', me.onSizeChange, me);
		vm.bind('{data.color}', me.onColorChange, me);
	},
	
	onSizeChange: function(nv, ov) {
		if (ov === undefined) return;
		var me = this,
				fldqr = me.lref('fldqrcode'),
				vm = me.getVM(),
				color = vm.get('data.color'),
				delta = nv - ov,
				osize = fldqr.getSize().width,
				nsize = osize + delta;
				
		me.updateQRCodeConfigs(nv, color);
		fldqr.setFieldStyle(fldqr.getFieldStyle());
		fldqr.setSize(nsize, nsize);
	},
	
	onColorChange: function(nv, ov) {
		if (ov === undefined) return;
		var me = this,
				vm = me.getVM(),
				size = vm.get('data.size');
		me.updateQRCodeConfigs(size, nv);
	},
	
	updateQRCodeConfigs: function(size, color) {
		var fldqr = this.lref('fldqrcode');
		fldqr.imageWidth = fldqr.imageHeight = size;
		fldqr.setUrlExtraParams({
			size: size,
			color: color
		});
	}
});
