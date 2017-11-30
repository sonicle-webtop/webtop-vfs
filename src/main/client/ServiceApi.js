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
Ext.define('Sonicle.webtop.vfs.ServiceApi', {
	extend: 'WTA.sdk.ServiceApi',
	
	buildFileId: function(shareId, storeId, path) {
		return shareId + '|' + storeId + '|' + path;
	},
	
	/**
	 * Gets the value of the specified variable.
	 * @param {String} name The variable name.
	 * @return {Mixed} Config value.
	 */
	getVar: function(name) {
		return this.service.getVar(name);
	},
	
	/**
	 * Adds new sharing link (download).
	 * @param {Object} data An object containing link data.
	 * @param {String} [data.fileId] The file ID.
	 * @param {Object} opts An object containing configuration.
	 * @param {Function} [opts.callback] A function to be called when the link is succesfully created.
	 * Will be called with the following config parameters:
	 * 
	 * - success - True if result is not null.
	 * - result - The result object.
	 * @param {Object} [opts.scope] The callback method scope.
	 */
	addSharingLinkForDownload: function(data, opts) {
		opts = opts || {};
		this.service.setupLink('D', data.fileId, {
			callback: opts.callback,
			scope: opts.scope
		});
	},
	
	/**
	 * Adds new sharing link (upload).
	 * @param {Object} data An object containing link data.
	 * @param {String} [data.fileId] The file ID.
	 * @param {Object} opts An object containing configuration.
	 * @param {Function} [opts.callback] A function to be called when the link is succesfully created.
	 * Will be called with the following config parameters:
	 * 
	 * - success - True if result is not null.
	 * - result - The result object.
	 * @param {Object} [opts.scope] The callback method scope.
	 */
	addSharingLinkForUpload: function(data, opts) {
		opts = opts || {};
		this.service.setupLink('U', data.fileId, {
			callback: opts.callback,
			scope: opts.scope
		});
	},
	
	/**
	 * Deletes the specified sharing link.
	 * @param {Object} data An object containing link data.
	 * @param {String} [data.fileId] The file ID.
	 * @param {Object} opts An object containing configuration.
	 * @param {Function} [opts.callback] A function to be called when the link is succesfully deleted.
	 * Will be called with the following config parameters:
	 * 
	 * - success - True if successful operation.
	 * @param {Object} [opts.scope] The callback method scope.
	 */
	deleteSharingLink: function(data, opts) {
		opts = opts || {};
		this.service.deleteLink(data.linkId, {
			callback: opts.callback,
			scope: opts.scope
		});
	}
});
