/* 
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
 * display the words "Copyright (C) 2014 Sonicle S.r.l.".
 */
Ext.define('Sonicle.webtop.vfs.mixin.FileObject', {
	extend: 'Ext.Mixin',
	requires: [
		'Sonicle.String'
	],
	mixinConfig: {
		id: 'wtvfsfileobject'
	},
	
	foIdField: '',
	foNameField: '',
	foPermsFiels: '',
	foDlLinkField: '',
	foUlLinkField: '',
	
	isFileObject: Ext.emptyFn(),
	getFOType: Ext.emptyFn(),
	getFOPerms: Ext.emptyFn(),
	
	isFOTypeFolder: function() {
		return 'folder' === this.getFOType();
	},
	
	isFOTypeFile: function() {
		return 'file' === this.getFOType();
	},
	
	getFOId: function() {
		return this.getFOField(this.foIdField);
	},
	
	getFOName: function() {
		return this.getFOField(this.foNameField);
	},
	
	getFORights: function() {
		return WTA.util.FoldersTree2.toRightsObj(this.getFOPerms());
	},
	
	getFODlLink: function() {
		return this.getFOField(this.foDlLinkField);
	},
	
	setFODlLink: function(v) {
		return this.setFOField(this.foDlLinkField, v);
	},
	
	getFOUlLink: function() {
		return this.getFOField(this.foUlLinkField);
	},
	
	setFOUlLink: function(v) {
		return this.setFOField(this.foUlLinkField, v);
	},
	
	isFOEditable: function() {
		return this.isFOTypeFile();
	},
	
	isFOOpenable: function() {
		return this.isFOTypeFile();
	},
	
	privates: {
		getFOField: function(field) {
			return this.isFileObject() ? this.get(field) : undefined;
		},
		
		setFOField: function(field, value) {
			return this.isFileObject() ? this.set(field, value) : undefined;
		}
	},
	
	statics: {
		isOpenableExt: function(ext) {
			return ['pdf'].indexOf(ext) !== -1;
		}
	}
});
