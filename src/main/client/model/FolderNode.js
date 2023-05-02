/* 
 * Copyright (C) 2023 Sonicle S.r.l.
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
Ext.define('Sonicle.webtop.vfs.model.FolderNode', {
	extend: 'Ext.data.Model',
	mixins: [
		'WTA.sdk.mixin.FolderNodeInterface',
		'Sonicle.webtop.vfs.mixin.FileObject'
	],
	
	//colorField: '_color',
	//defaultField: '_default',
	builtInField: '_builtIn',
	//activeField: '_active',
	originPermsField: '_orPerms',
	folderPermsField: '_foPerms',
	itemsPermsField: '_itPerms',
	
	foIdField: 'id',
	foNameField: 'text',
	foDlLinkField: '_dlLink',
	foUlLinkField: '_ulLink',
	
	fields: [
		WTF.roField('_builtIn', 'int'),
		WTF.roField('_scheme', 'string'),
		WTF.roField('_orPerms', 'string'),
		WTF.roField('_foPerms', 'string'),
		WTF.roField('_itPerms', 'string'),
		
		WTF.roField('_dlLink', 'string'),
		WTF.roField('_ulLink', 'string')
	],
	
	isBuiltInFolder: function() {
		var me = this;
		return me.isFolder() ? me.get(me.builtInField) > 0 : false;
	},
	
	getScheme: function() {
		return this.isFolder() ? this.get('_scheme') : undefined;
	},
	
	isFileObject: function() {
		return 'L' === this.parseId().type;
	},
	
	getFOType: function() {
		return 'folder';
	},
	
	getFOPerms: function() {
		return this.get(this.itemsPermsField);
	},
	
	getFOId: function() {
		var me = this, fid;
		if (me.isFolder()) fid = me.getId();
		if (me.isFileObject()) fid =  me.mixins.wtvfsfileobject.getFOId.call(me);
		return fid;
	},
	
	getFOStoreId: function() {
		var me = this;
		return (me.isFolder() || me.isFileObject()) ? this.parseId().folder : undefined;
	},
	
	getFOPath: function() {
		var me = this;
		return me.isFolder() ? '/' : this.parseId().filePath;
	},
	
	privates: {
		parseId: function() {
			var tokens = Sonicle.String.split(this.getId(), '|', 4);
			return {
				type: tokens[0],
				origin: tokens[1],
				folder: tokens[2],
				filePath: tokens[3]
			};
		}
	}
});
