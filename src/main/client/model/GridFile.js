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
Ext.define('Sonicle.webtop.vfs.model.GridFile', {
	extend: 'WTA.ux.data.BaseModel',
	mixins: [
		'Sonicle.webtop.vfs.mixin.FileObject'
	],
	
	foIdField: 'fileId',
	foNameField: 'name',
	foDlLinkField: 'dlLink',
	foUlLinkField: 'ulLink',
	
	idProperty: 'fileId',
	fields: [
		WTF.roField('fileId', 'string'),
		WTF.roField('type', 'string'),
		WTF.roField('mtype', 'string'),
		WTF.roField('name', 'string'),
		WTF.roField('ext', 'string'),
		WTF.roField('size', 'int'),
		WTF.roField('lastModified', 'date', {dateFormat: 'Y-m-d H:i:s'}),
		WTF.roField('editable', 'int'),
		WTF.roField('dlLink', 'string'),
		WTF.roField('dlLinkExp', 'boolean'),
		WTF.roField('ulLink', 'string'),
		WTF.roField('ulLinkExp', 'boolean'),
		WTF.roField('itPerms', 'string'),
		WTF.roField('storeId', 'string'),
		WTF.roField('path', 'string')
	],
	
	isFileObject: function() {
		return true;
	},
	
	getFOType: function() {
		return this.get('type');
	},
	
	getFOPerms: function() {
		return this.get('itPerms');
	},
	
	isFOEditable: function() {
		var me = this;
		return me.mixins.wtvfsfileobject.isFOEditable.call(me) && me.get('editable') > 0;
	},
	
	isFOOpenable: function() {
		var me = this;
		return me.mixins.wtvfsfileobject.isFOOpenable.call(me) && Sonicle.webtop.vfs.mixin.FileObject.isOpenableExt(me.get('ext'));
	}
});
