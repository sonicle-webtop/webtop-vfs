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
Ext.define('Sonicle.webtop.vfs.Service', {
	extend: 'WTA.sdk.Service',
	requires: [
		'Sonicle.Bytes',
		'Sonicle.Data',
		'Sonicle.Utils',
		'Sonicle.grid.column.Bytes',
		'Sonicle.grid.column.Icon',
		'Sonicle.grid.column.Link',
		'Sonicle.grid.plugin.DragDrop',
		'Sonicle.plugin.FileDrop',
		'Sonicle.toolbar.Breadcrumb',
		'Sonicle.upload.Button',
		'WTA.ux.data.EmptyModel',
		'WTA.ux.data.SimpleModel',
		'WTA.ux.UploadBar',
		'Sonicle.webtop.vfs.model.FolderNode',
		'Sonicle.webtop.vfs.model.GridFile',
		'Sonicle.webtop.vfs.model.SharingLink'
	],
	uses: [
		'Sonicle.webtop.vfs.ux.DuplicateConfirmBox',
		'Sonicle.webtop.vfs.view.FolderChooser',
		'Sonicle.webtop.vfs.view.FolderSharing',
		'Sonicle.webtop.vfs.view.SharingLinks',
		'Sonicle.webtop.vfs.view.QRCodeGen'
	],
	
	api: null,
	
	getApiInstance: function() {
		var me = this;
		if (!me.api) me.api = Ext.create('Sonicle.webtop.vfs.ServiceApi', {service: me});
		return me.api;
	},
	
	needsReload: true,
	curNode: null,
	curFile: null,
	
	setCurNode: function(nodeId) {
		this.curNode = nodeId;
		//console.log('current node: '+nodeId);
	},
	
	getCurrentFileNode: function() {
		var id = this.curNode;
		return id ? this.trStores().getStore().getNodeById(id) : null;
	},
	
	setCurFile: function(fileId) {
		var me = this, tr, node, tbu;
		//console.log('current file: '+fileId);
		
		if (fileId !== me.curFile) {
			tbu = me.tbUpload();
			me.curFile = fileId;
			if(fileId) {
				tr = me.trStores();
				node = tr.getStore().getNodeById(fileId);
				if(node) {
					tbu.setDisabled(!node.getFORights().CREATE);
					tr.getSelectionModel().select(node);
					me.bcFiles().setSelection(node);
					node.expand();
					me.reloadGridFiles();
				}
			} else {
				me.gpFiles().getStore().removeAll();
				me.bcFiles().setSelection(null);
				tbu.setDisabled(true);
			}
			me.updateDisabled('createFolder');
			me.updateDisabled('createDocument');
		}
	},
	
	isInCurFile: function(fileId) {
		var cf = this.curFile;
		return (cf && cf.indexOf(fileId) > -1);
	},
	
	init: function() {
		var me = this, ies, iitems = [];
		
		me.initActions();
		me.initCxm();
		
		me.on('activate', me.onActivate, me);
		
		me.setToolbar(Ext.create({
			xtype: 'toolbar',
			referenceHolder: true,
			items: [
				me.getAct('showSharingLinks')
			]
		}));
		
		me.setToolComponent(Ext.create({
			xtype: 'panel',
			layout: 'border',
			referenceHolder: true,
			title: me.getName(),
			items: [{
				region: 'center',
				xtype: 'treepanel',
				reference: 'trstores',
				border: false,
				useArrows: true,
				rootVisible: false,
				store: {
					autoLoad: true,
					model: 'Sonicle.webtop.vfs.model.FolderNode',
					proxy: WTF.apiProxy(me.ID, 'ManageStoresTree', 'children', {
						writer: {
							allowSingle: false // Always wraps records into an array
						}
					}),
					root: {
						id: 'root',
						expanded: true
					}
				},
				hideHeaders: true,
				listeners: {
					selectionchange: function(s, nodes) {
						var node = (nodes.length === 1) ? nodes[0] : null;
						if (node) {
							if (node.isFolder()) {
								me.setCurNode(node.getId());
							} else if (node.isFileObject()) {
								me.setCurNode(node.getId());
							} else {
								me.setCurNode(null);
							}
						} else {
							me.setCurNode(null);
						}
					},
					itemexpand: function(node, e) {
						if (node.isFolder()) { // Store node -> file system root
							me.setCurFile(node.getId());
						} else if (node.isFileObject()) { // File(folder) node -> file system folder
							me.setCurFile(node.getId());
						}
					},
					itemclick: function(s, node) {
						if (node.isFolder()) { // Store node -> file system root
							me.setCurFile(node.getId());
						} else if (node.isFileObject()) { // File(folder) node -> file system folder
							me.setCurFile(node.getId());
						}
					},
					itemcontextmenu: function(s, node, itm, i, e) {
						if (node.isOrigin() || node.isGrouper()) {
							me.setCurNode(node.getId());
							Sonicle.Utils.showContextMenu(e, me.getRef('cxmTreeOrigin'), {node: node});
						} else if (node.isFolder()) {
							me.setCurNode(node.getId());
							Sonicle.Utils.showContextMenu(e, me.getRef('cxmTreeFolder'), {node: node});
						} else if (node.isFileObject()) {
							me.setCurNode(node.getId());
							Sonicle.Utils.showContextMenu(e, me.getRef('cxmTreeFile'), {node: node});
						} else {
							me.setCurNode(null);
						}
					}
				}
			}]
		}));
		
		var sto = me.trStores().getStore(),
			gpId = Ext.id(null, 'gridpanel');
		
		me.grid = me.createGrid(gpId, sto);
		me.mainComponent = Ext.create({
			xtype: 'container',
			layout: 'border',
			referenceHolder: true,
			/*viewModel: {
				formulas: {
					selectedTask: {
						bind: {bindTo: '{gptasks.selection}'},
						get: function (val) {
							return val;
						}
					},
					selectedTaskPercentage: {
						bind: {bindTo: '{selectedTask.percentage}'},
						get: function (val) {
							return Ext.isEmpty(val) ? '' : val + '%';
						}
					},
					selectedTaskReminder: {
						bind: {bindTo: '{selectedTask.reminderDate}'},
						get: function (val) {
							return !Ext.isDate(val) ? WT.res('word.none.male') : Ext.Date.format(val, WT.getShortDateTimeFmt());
						}
					}
				}
			},*/
			items: [
				me.grid/*, {
				region: 'east',
				xtype: 'wtform',
				stateful: true,
				//stateId: me.buildStateId('gptaskspreview'),
				split: true,
				collapsible: true,
				title: WT.res('word.preview'),
				width: 200,
				defaults: {
					labelAlign: 'top',
					readOnly: true,
					anchor: '100%'
				},
				items: []
			}*/]
		});
		me.setMainComponent(me.mainComponent);
	},
	
	followGridFile: function(rec) {
		var me = this;
		if (rec.isFOTypeFolder()) {
			me.setCurFile(rec.getFOId());
			me.reloadGridFiles();
		} else if (rec.isFOEditable() && rec.getFORights().UPDATE) {
			me.editFileUI(rec, !me.isEditActionEdit());
		} else if (rec.isFOOpenable()) {
			me.openFile(rec.getFOId());
		} else {
			me.downloadFiles(false, [rec.getFOId()]);
		}
	},
	
	expandFile: function(fileId) {
		var me = this,
				tree = me.trStores(),
				node = tree.getStore().getNodeById(fileId);
		if(node) {
			node.expand();
			tree.getSelectionModel().select(node);
		}
	},
	
	trStores: function() {
		return this.getToolComponent().lookupReference('trstores');
	},
	
	gpFiles: function() {
		return this.getMainComponent().lookupReference('gpfiles');
	},
	
	bcFiles: function() {
		return this.getMainComponent().lookupReference('bcfiles');
	},
	
	tbUpload: function() {
		return this.getMainComponent().lookupReference('tbupload');
	},
	
	btnUpload: function() {
		return this.getMainComponent().lookupReference('btnupload');
	},
	
	getMyRoot: function() {
		return this.trStores().getStore().findNode('_pid', WT.getVar('profileId'), false);
	},
	
	getBuiltInStoreByRoot: function(rootNode, builtIn) {
		if (!rootNode) return null;
		return rootNode.findChildBy(function(n) {
			return (n.get('_builtIn') === builtIn);
		});
	},
	
	initActions: function() {
		var me = this,
			hdscale = WT.getHeaderScale();
		
		me.addAct('editSharing', {
			text: WT.res('sharing.tit'),
			tooltip: null,
			iconCls: 'wt-icon-sharing',
			handler: function(s, e) {
				var node = e.menuData.node;
				if (node) me.manageFolderSharingUI(node.getId());
			}
		});
		me.addAct('addStoreFtp', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				var node = me.getCurrentFileNode();
				if (node) me.addStoreUI('ftp', node);
			}
		});
		me.addAct('addStoreDropbox', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				var node = me.getCurrentFileNode();
				if (node) me.addStoreUI('dropbox', node);
			}
		});
		me.addAct('addStoreGooDrive', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				var node = me.getCurrentFileNode();
				if (node) me.addStoreUI('goodrive', node);
			}
		});
		me.addAct('addStoreNextcloud', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				var node = me.getCurrentFileNode();
				if (node) me.addStoreUI('nextcloud', node);
			}
		});
		me.addAct('addStoreFile', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				var node = me.getCurrentFileNode();
				if (node) me.addStoreUI('file', node);
			}
		});
		me.addAct('addStoreOther', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				var node = me.getCurrentFileNode();
				if (node) me.addStoreUI('other', node);
			}
		});
		me.addAct('editStore', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				var node = me.getCurrentFileNode();
				if (node) me.editStoreUI(node);
			}
		});
		me.addAct('deleteStore', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				var node = me.getCurrentFileNode();
				if (node) me.deleteStoreUI(node);
			}
		});
		me.addAct('refreshFileNode', {
			text: WT.res('act-refresh.lbl'),
			tooltip: null,
			iconCls: 'wt-icon-refresh',
			handler: function() {
				var node = me.getCurrentFileNode();
				if (node) {
					me.setCurFile(node.getId());
					me.loadTreeFileNode(node);
					me.reloadGridFiles();
				}
			}
		});
		me.addAct('createFileNode', {
			ignoreSize: true,
			text: me.res('act-createFileFolder.lbl'),
			tooltip: null,
			iconCls: me.cssIconCls('createFolder'),
			handler: function() {
				if (me.curFile) me.createFolderUI(me.curFile);
			}
		});
		me.addAct('renameFileNode', {
			ignoreSize: true,
			text: me.res('act-renameFile.lbl'),
			tooltip: null,
			iconCls: me.cssIconCls('renameFile'),
			handler: function() {
				var node = me.getCurrentFileNode();
				if (node) me.renameFileUI(node);
			}
		});
		me.addAct('deleteFileNode', {
			ignoreSize: true,
			text: me.res('act-deleteFile.lbl'),
			tooltip: null,
			iconCls: me.cssIconCls('deleteFile'),
			handler: function() {
				var node = me.getCurrentFileNode();
				if (node) me.deleteFileNodeUI(node);
			}
		});
		me.addAct('addFileNodeDlLink', {
			ignoreSize: true,
			text: me.res('act-addFileDlLink.lbl'),
			tooltip: null,
			iconCls: me.cssIconCls('addFileDlLink'),
			handler: function() {
				var node = me.getCurrentFileNode();
				if (node) me.addFileLinkUI('D', node);
			}
		});
		me.addAct('deleteFileNodeDlLink', {
			text: me.res('act-deleteFileDlLink.lbl'),
			tooltip: null,
			iconCls: null,
			handler: function() {
				var node = me.getCurrentFileNode();
				if (node) me.deleteFileLinkUI('D', node);
			}
		});
		me.addAct('sendFileNodeDlLink', {
			text: me.res('act-sendFileDlLink.lbl'),
			tooltip: null,
			handler: function() {
				var node = me.getCurrentFileNode();
				if (node) me.sendFileLinkUI('D', node);
			}
		});
		me.addAct('genFileNodeDlQRcode', {
			ignoreSize: true,
			text: me.res('act-genFileDlQRcode.lbl'),
			tooltip: null,
			iconCls: me.cssIconCls('genFileDlQRcode'),
			handler: function() {
				var sel = me.getCurrentFileNode();
				if (sel) me.genFileQRCodeUI('D', sel);
			}
		});
		me.addAct('addFileNodeUlLink', {
			ignoreSize: true,
			text: me.res('act-addFileUlLink.lbl'),
			tooltip: null,
			iconCls: me.cssIconCls('addFileUlLink'),
			handler: function() {
				var sel = me.getCurrentFileNode();
				if (sel) me.addFileLinkUI('U', sel);
			}
		});
		me.addAct('deleteFileNodeUlLink', {
			text: me.res('act-deleteFileUlLink.lbl'),
			tooltip: null,
			iconCls: null,
			handler: function() {
				var sel = me.getCurrentFileNode();
				if (sel) me.deleteFileLinkUI('U', sel);
			}
		});
		me.addAct('sendFileNodeUlLink', {
			text: me.res('act-sendFileUlLink.lbl'),
			tooltip: null,
			handler: function() {
				var node = me.getCurrentFileNode();
				if (node) me.sendFileLinkUI('U', node);
			}
		});
		me.addAct('goUp', {
			ignoreSize: true,
			text: null,
			handler: function() {
				var bc = me.bcFiles(),
						node = bc.getSelectionParent();
				if (node) bc.setSelection(node);
			}
		});
		me.addAct('refresh', {
			text: WT.res('act-refresh.lbl'),
			tooltip: null,
			iconCls: 'wt-icon-refresh',
			handler: function() {
				if (me.curFile) {
					me.loadTreeFileNode(me.curFile);
					me.reloadGridFiles();
				}
			}
		});
		me.addAct('openFile', {
			tooltip: null,
			handler: function() {
				var sel = me.getSelectedFile();
				if (sel) me.openFileUI(sel);
			}
		});
		me.addAct('editFile', {
			text: me.res('act-openFile.lbl'),
			tooltip: null,
			handler: function() {
				var sel = me.getSelectedFile();
				if (sel) me.editFileUI(sel, !me.isEditActionEdit());
			}
		});
		me.addAct('editFileView', {
			tooltip: null,
			handler: function() {
				var sel = me.getSelectedFile();
				if (sel) me.editFileUI(sel, true);
			}
		});
		me.addAct('editFileEdit', {
			tooltip: null,
			handler: function() {
				var sel = me.getSelectedFile();
				if (sel) me.editFileUI(sel, false);
			}
		});
		me.addAct('createFolder', {
			ignoreSize: true,
			text: me.res('act-createFileFolder.lbl'),
			tooltip: null,
			disabled: true,
			handler: function() {
				if (me.curFile) me.createFolderUI(me.curFile);
			}
		});
		me.addAct('createDocument', {
			ignoreSize: true,
			text: me.res('act-createDocument.lbl'),
			tooltip: null,
			iconCls: 'wtvfs-icon-createFile',
			disabled: true
		});
		me.addAct('createFileTxt', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				if (me.curFile) me.createFileUI(me.curFile, 'txt', true);
			}
		});
		me.addAct('createFileHtml', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				if (me.curFile) me.createFileUI(me.curFile, 'html', true);
			}
		});
		me.addAct('createFileDocx', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				if (me.curFile) me.createFileUI(me.curFile, 'docx', true);
			}
		});
		me.addAct('createFileXlsx', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				if (me.curFile) me.createFileUI(me.curFile, 'xlsx', true);
			}
		});
		me.addAct('createFilePptx', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				if (me.curFile) me.createFileUI(me.curFile, 'pptx', true);
			}
		});
		me.addAct('createFileOdt', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				if (me.curFile) me.createFileUI(me.curFile, 'odt', true);
			}
		});
		me.addAct('createFileOds', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				if (me.curFile) me.createFileUI(me.curFile, 'ods', true);
			}
		});
		me.addAct('createFileOdp', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				if (me.curFile) me.createFileUI(me.curFile, 'odp', true);
			}
		});
		me.addAct('downloadFile', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				var sel = me.getSelectedFiles();
				if (sel.length > 0) me.downloadFilesUI(sel);
			}
		});
		me.addAct('renameFile', {
			ignoreSize: true,
			tooltip: null,
			disabled: true,
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.renameFileUI(sel);
			}
		});
		me.addAct('deleteFile', {
			ignoreSize: true,
			tooltip: null,
			disabled: true,
			handler: function() {
				var sel = me.getSelectedFiles();
				if(sel.length > 0) me.deleteFilesUI(sel);
			}
		});
		me.addAct('addFileDlLink', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.addFileLinkUI('D', sel);
			}
		});
		me.addAct('deleteFileDlLink', {
			tooltip: null,
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.deleteFileLinkUI('D', sel);
			}
		});
		me.addAct('sendFileDlLink', {
			tooltip: null,
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.sendFileLinkUI('D', sel);
			}
		});
		me.addAct('genFileDlQRcode', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				var sel = me.getSelectedFile();
				if (sel) me.genFileQRCodeUI('D', sel);
			}
		});
		me.addAct('addFileUlLink', {
			ignoreSize: true,
			tooltip: null,
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.addFileLinkUI('U', sel);
			}
		});
		me.addAct('deleteFileUlLink', {
			tooltip: null,
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.deleteFileLinkUI('U', sel);
			}
		});
		me.addAct('sendFileUlLink', {
			tooltip: null,
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.sendFileLinkUI('U', sel);
			}
		});
		me.addAct('showSharingLinks', {
			scale: hdscale,
			tooltip: null,
			iconCls: me.cssIconCls('sharingLinks'),
			handler: function() {
				me.showSharingLinks({
					listeners: {
						linkupdate: function(s, type, linkId, paFileId) {
							var trsto = me.trStores().getStore(),
									node = trsto.getNodeById(paFileId);
							if(node) trsto.load({node: node});
							me.reloadGridFilesIf(paFileId);
						}
					}
				});
			}
		});
		me.addAct('downloadFileNode', {
			ignoreSize: true,
			text: me.res('act-downloadFile.lbl'),
			tooltip: null,
			iconCls: 'wtvfs-icon-downloadFile',
			handler: function() {
				var node = me.getCurrentFileNode();
				if (node) me.downloadFilesUI([node]);
			}
		});
	},
	
	initCxm: function() {
		var me = this;
		me.addRef('cxmTreeOrigin', Ext.create({
			xtype: 'menu',
			items: [
				{
					text: me.res('act-addStore.lbl'),
					iconCls: me.cssIconCls('addStore'),
					menu: [
						me.getAct('addStoreFtp'),
						//me.getAct('addStoreDropbox'),
						me.getAct('addStoreGooDrive'),
						me.getAct('addStoreNextcloud'),
						me.getAct('addStoreFile'),
						me.getAct('addStoreOther')
					]
				},
				'-',
				me.getAct('editSharing')
			],
			listeners: {
				beforeshow: function(s) {
					var node = s.menuData.node,
						mine = node.isPersonalNode(),
						or = node.getOriginRights(),
						fileAllowed = me.isPermitted('STORE_FILE', 'CREATE'),
						cloudAllowed = me.isPermitted('STORE_CLOUD', 'CREATE'),
						otherAllowed = me.isPermitted('STORE_OTHER', 'CREATE');
					
					me.getAct('addStoreFtp').setDisabled(!or.MANAGE || !otherAllowed);
					//me.getAct('addStoreDropbox').setDisabled(!or.MANAGE || !cloudAllowed);
					me.getAct('addStoreGooDrive').setDisabled(!or.MANAGE || !cloudAllowed);
					me.getAct('addStoreNextcloud').setDisabled(!or.MANAGE || !cloudAllowed);
					me.getAct('addStoreFile').setDisabled(!or.MANAGE || !fileAllowed);
					me.getAct('addStoreOther').setDisabled(!or.MANAGE || !otherAllowed);
					me.getAct('editSharing').setDisabled(!or.MANAGE);
				}
			}
		}));
		me.addRef('cxmTreeFolder', Ext.create({
			xtype: 'menu',
			items: [
				me.getAct('createFileNode'),
				'-',
				me.getAct('editStore'),
				me.getAct('deleteStore'),
				'-',
				me.getAct('editSharing'),
				'-',
				me.getAct('refreshFileNode')
			],
			listeners: {
				beforeshow: function(s) {
					var node = s.menuData.node,
						fr = node.getFolderRights();
					
					me.getAct('editStore').setDisabled(!fr.UPDATE || node.isBuiltInFolder());
					me.getAct('deleteStore').setDisabled(!fr.DELETE || node.isBuiltInFolder());
					me.getAct('editSharing').setDisabled(!fr.MANAGE);
					me.updateDisabled('createFileNode');
				}
			}
		}));
		me.addRef('cxmTreeFile', Ext.create({
			xtype: 'menu',
			items: [
				me.getAct('createFileNode'),
				me.getAct('downloadFileNode'),
				'-',
				me.getAct('refreshFileNode'),
				'-',
				me.getAct('addFileNodeDlLink'),
				me.getAct('deleteFileNodeDlLink'),
				me.getAct('sendFileNodeDlLink'),
				me.getAct('genFileNodeDlQRcode'),
				'-',
				me.getAct('addFileNodeUlLink'),
				me.getAct('deleteFileNodeUlLink'),
				me.getAct('sendFileNodeUlLink'),
				'-',
				me.getAct('renameFileNode'),
				me.getAct('deleteFileNode')
			],
			listeners: {
				beforeshow: function(s) {
					me.updateDisabled('renameFileNode');
					me.updateDisabled('deleteFileNode');
					me.updateDisabled('createFileNode');
					me.updateDisabled('addFileNodeDlLink');
					me.updateDisabled('deleteFileNodeDlLink');
					me.updateDisabled('sendFileNodeDlLink');
					me.updateDisabled('genFileNodeDlQRcode');
					me.updateDisabled('addFileNodeUlLink');
					me.updateDisabled('deleteFileNodeUlLink');
					me.updateDisabled('sendFileNodeUlLink');
				}
			}
		}));
		me.addRef('cxmGridFile0', Ext.create({
			xtype: 'menu',
			items: [
				me.getAct('createFolder'),
				me.getActAs('createDocument', 'menuitem', {
					hidden: !WT.getVar('docServerEnabled'),
					menu: {
						items: [
							me.getAct('createFileDocx'),
							me.getAct('createFileXlsx'),
							me.getAct('createFilePptx'),
							me.getAct('createFileTxt'),
							me.getAct('createFileHtml')
							//me.getAct('createFileOdt'),
							//me.getAct('createFileOds'),
							//me.getAct('createFileOdp')
						]
					}
				}),
				'-',
				me.getAct('refresh')
			]
		}));
		me.addRef('cxmGridFile', Ext.create({
			xtype: 'menu',
			items: [
				me.getAct('downloadFile'),
				me.getAct('openFile'),
				me.getActAs('editFile', 'menuitem', {
					menu: {
						items: [
							me.getAct(me.isEditActionEdit() ? 'editFileView' : 'editFileEdit'),
							me.getAct(me.isEditActionEdit() ? 'editFileEdit' : 'editFileView')
						]
					}
				}),
				'-',
				me.getAct('createFolder'),
				me.getActAs('createDocument', 'menuitem', {
					hidden: !WT.getVar('docServerEnabled'),
					menu: {
						items: [
							me.getAct('createFileDocx'),
							me.getAct('createFileXlsx'),
							me.getAct('createFilePptx'),
							me.getAct('createFileTxt'),
							me.getAct('createFileHtml')
							//me.getAct('createFileOdt'),
							//me.getAct('createFileOds'),
							//me.getAct('createFileOdp')
						]
					}
				}),
				'-',
				me.getAct('addFileDlLink'),
				me.getAct('deleteFileDlLink'),
				me.getAct('sendFileDlLink'),
				me.getAct('genFileDlQRcode'),
				'-',
				me.getAct('addFileUlLink'),
				me.getAct('deleteFileUlLink'),
				me.getAct('sendFileUlLink'),
				'-',
				me.getAct('renameFile'),
				me.getAct('deleteFile')
			],
			listeners: {
				beforeshow: function(s) {
					me.updateDisabled('openFile');
					me.setActHiddenIfDisabled('openFile');
					me.updateDisabled('editFile');
					me.setActHiddenIfDisabled('editFile');
					me.updateDisabled('editFileView');
					me.updateDisabled('editFileEdit');
					me.updateDisabled('downloadFile');
					me.updateDisabled('renameFile');
					me.updateDisabled('addFileDlLink');
					me.updateDisabled('deleteFileDlLink');
					me.updateDisabled('sendFileDlLink');
					me.updateDisabled('genFileDlQRcode');
					me.updateDisabled('addFileUlLink');
					me.updateDisabled('deleteFileUlLink');
					me.updateDisabled('sendFileUlLink');
				}
			}
		}));
	},
	
	manageFolderSharingUI: function(nodeId) {
		var me = this,
			vw = WT.createView(me.ID, 'view.FolderSharing', {swapReturn: true});
		
		vw.showView(function() {
			vw.begin('edit', {
				data: {
					id: nodeId
				}
			});
		});
	},
	
	addStoreUI: function(type, node) {
		var me = this, pid = node.getOwnerPid();
		if (type === 'ftp') {
			me.setupStoreFtp(pid, {
				callback: function(success) {
					if (success) me.loadOriginNode(pid);
				}
			});
		} else if (type === 'dropbox') {
			me.setupStoreDropbox(pid, {
				callback: function(success) {
					if (success) me.loadOriginNode(pid);
				}
			});
		} else if(type === 'goodrive') {
			me.setupStoreGoogleDrive(pid, {
				callback: function(success) {
					if (success) me.loadOriginNode(pid);
				}
			});
		} else if (type === 'nextcloud') {
			me.setupStoreNextcloud(pid, {
				callback: function(success) {
					if (success) me.loadOriginNode(pid);
				}
			});
		} else if (type === 'file') {
			me.setupStoreFile(pid, {
				callback: function(success) {
					if (success) me.loadOriginNode(pid);
				}
			});
		} else if (type === 'other') {
			me.setupStoreOther(pid, {
				callback: function(success) {
					if (success) me.loadOriginNode(pid);
				}
			});
		}
	},
	
	editStoreUI: function(node) {
		var me = this;
		me.editStore(node.get('_storeId'), {
			callback: function(success) {
				if (success) me.loadOriginNode(node.getOwnerPid());
			}
		});
	},
	
	deleteStoreUI: function(node) {
		var me = this;
		WT.confirm(me.res('store.confirm.delete', Ext.String.ellipsis(node.get('text'), 40)), function(bid) {
			if(bid === 'yes') {
				me.deleteStore(node.get('_storeId'), {
					callback: function(success) {
						if (success) {
							node.remove();
							if (me.isInCurFile(node.getFOId())) me.setCurFile(null);
						}
					}
				});
			}
		});
	},
	
	openFileUI: function(sel) {
		var me = this;
		me.openFile(sel.getFOId());
	},
	
	downloadFilesUI: function(sel) {
		var me = this;
		if (sel[0].isFOTypeFolder()) {
			WT.confirm(me.res('gpfiles.confirm.download.recursive'), function(bid) {
				if (bid !== 'cancel') me.downloadFiles(bid === 'yes', [sel[0].getFOId()]);
			});
		} else {
			me.downloadFiles(false, me.selectionIds(sel, 'file'));
		}
	},
	
	editFileUI: function(sel, view) {
		var me = this;
		if (!view && sel.get('editable') === 2) {
			WT.confirm(me.res('gpfiles.confirm.edit.remote'), function(bid) {
				if (bid !== 'cancel') {
					if (bid === 'no') view = true;
					me.editFile(sel.getFOId(), {
						callback: function(success, data) {
							if (success) me.showDocEditingView(view, data);
						}
					});
				}
			}, me, {
				config: {
					buttonText: {
						yes: me.res('gpfiles.confirm.edit.remote.yes'),
						no: me.res('gpfiles.confirm.edit.remote.no')
					}
				}
			});
			
		} else {
			me.editFile(sel.getFOId(), {
				callback: function(success, data) {
					if (success) me.showDocEditingView(view, data);
				}
			});
		}
	},
	
	createFolderUI: function(pfile) {
		var me = this,
				fid = pfile.isModel ? pfile.getFOId() : pfile;
		
		WT.prompt(me.res('gpfiles.name.lbl'), {
			title: me.res('act-createFileFolder.lbl'),
			value: '',
			fn: function(bid, name) {
				if (bid === 'ok') {
					me.createFile(fid, 'folder', name, {
						callback: function(success) {
							if (success) {
								me.loadTreeFileNode(fid);
								me.reloadGridFiles();
							}
						}
					});
				}
			}
		});
	},
	
	/**
	 * @param {String|FileInterface} file 
	 * @param {String} type
	 * @param {Boolean} editImmediately
	 */
	createFileUI: function(file, type, editImmediately) {
		var me = this,
			fid = Ext.isString(file) ? file : (file.isModel ? file.getFOId() : null);
		
		if (!fid) Ext.raise('Parameter should be a valid String or FileInterface object');
		WT.prompt(me.res('gpfiles.name.lbl'), {
			title: '.' + type + ' - ' + me.res('act-createDocument.lbl'),
			value: '',
			fn: function(bid, name) {
				if (bid === 'ok') {
					me.createFile(fid, type, name, {
						callback: function(success, data) {
							if (success) {
								me.loadTreeFileNode(fid);
								me.reloadGridFiles();
								if (editImmediately && WT.getVar('docServerEnabled')) {
									me.editFile(data.fileId, {
										callback: function(success, data) {
											if (success) me.showDocEditingView(false, data);
										}
									});
								}
							}
						}
					});
				}
			}
		});
	},
	
	/**
	 * @param {FileInterface} file
	 */
	renameFileUI: function(file) {
		if (!file.isModel) Ext.raise('Parameter should be a valid FileInterface object');
		var me = this, pfid;
		WT.prompt(me.res('gpfiles.name.lbl'), {
			title: me.res('act-renameFile.lbl'),
			value: file.getFOName(),
			fn: function(bid, name) {
				if(bid === 'ok') {
					if(name !== file.getFOName()) {
						me.renameFile(file.getFOId(), name, {
							callback: function(success, data, js) {
								if (success) {
									if (file.isNode) {
										pfid = file.parentNode.getId(); // Keep here before load
										me.loadTreeFileNode(file.parentNode);
										if (me.isInCurFile(file.getFOId())) {
											me.setCurFile(pfid);
											me.reloadGridFiles();
										} else {
											me.reloadGridFiles();
										}
									} else {
										me.loadTreeFileNode();
										me.reloadGridFiles();
									}
								} else {
									WT.error(js.message);
								}
							}
						});
					}
				}
			}
		});
	},
	
	deleteFileNodeUI: function(node) {
		var me = this,
			sto2, rec2;
		WT.confirm(me.res('gpfiles.confirm.folder.delete', Ext.String.ellipsis(node.getFOName(), 40)), function(bid) {
			if(bid === 'yes') {
				me.deleteFiles([node.getFOId()], {
					callback: function(success) {
						if (success) {
							if (me.curFile && me.curFile.indexOf(node.getFOId()) > -1) {
								// Files grid is currently displaying node being deleted
								me.setCurFile(node.parentNode.getId());
								me.reloadGridFiles();
							} else {
								sto2 = me.gpFiles().getStore();
								rec2 = sto2.getById(node.getFOId());
								if (rec2) sto2.remove(rec2);
							}
							node.remove();
						}
					}
				});
			}
		});
	},
	
	deleteFilesUI: function(sel) {
		var me = this,
			sto = me.gpFiles().getStore(),
			ids = me.selectionIds(sel),
			msg;
		
		if (sel.length === 1) {
			msg = me.res('gpfiles.confirm.'+sel[0].getFOType()+'.delete', Ext.String.ellipsis(sel[0].getFOName(), 40));
		} else {
			msg = me.res('gpfiles.confirm.delete.selection');
		}
		WT.confirm(msg, function(bid) {
			if (bid === 'yes') {
				me.deleteFiles(ids, {
					callback: function(success) {
						if (success) {
							sto.remove(sel);
							me.loadTreeFileNode();
						}
					}
				});
			}
		});
	},
	
	genFileQRCodeUI: function(type, sel) {
		var me = this,
			linkId = (type === 'D') ? sel.getFODlLink() : sel.getFOUlLink();
		
		if (Ext.isEmpty(linkId)) {
			WT.confirm(me.res('sharingLink.confirm.autocreate.'+type), function(bid) {
				if (bid === 'yes') {
					me.setupAutoLink(type, sel.getFOId(), {
						callback: function(success, json) {
							if (success) {
								if (sel.isNode) {
									me.loadTreeFileNode(sel.parentNode);
									me.reloadGridFiles();
								} else {
									me.loadTreeFileNode();
									me.reloadGridFiles();
								}
								me.showQRCode(sel.getFOName(), json.id);
							}
						}
					});
				}
			});
		} else {
			me.showQRCode(sel.getFOName(), linkId);
		}	
	},
	
	addFileLinkUI: function(type, sel) {
		var me = this;
		me.setupLink(type, sel.getFOId(), {
			callback: function(success) {
				if (success) {
					if (sel.isNode) {
						me.loadTreeFileNode(sel.parentNode);
						me.reloadGridFiles();
					} else {
						me.loadTreeFileNode();
						me.reloadGridFiles();
					}
				}
			}
		});
	},
	
	deleteFileLinkUI: function(type, sel) {
		var me = this,
				linkId = (type === 'D') ? sel.getFODlLink() : sel.getFOUlLink(),
				rec2;
		WT.confirm(me.res('sharingLink.confirm.delete'), function(bid) {
			if (bid === 'yes') {
				me.deleteLink(linkId, {
					callback: function(success) {
						if (success) {
							if (sel.isNode) {
								rec2 = me.gpFiles().getStore().getById(sel.getFOId());
							} else {
								rec2 = me.trStores().getStore().getNodeById(sel.getFOId());
							}
							if (type === 'D') {
								sel.setFODlLink(null);
								if (rec2) rec2.setFODlLink(null);
							} else if (type === 'U') {
								sel.setFOUlLink(null);
								if (rec2) rec2.setFOUlLink(null);
							}
						}
					}
				});
			}
		});
	},
	
	sendFileLinkUI: function(type, sel) {
		var me = this,
			linkId = (type === 'D') ? sel.getFODlLink() : sel.getFOUlLink();
		me.getLinkEmbedCode(linkId, {
			callback: function(success, json) {
				if (success) {
					var html = '<br>' + json + '<br>',
							mapi = WT.getServiceApi('com.sonicle.webtop.mail');
					if (mapi) {
						mapi.newMessage({
							format: 'html',
							content: html
						}, {
							dirty: true,
							contentReady: false,
							appendContent: false
						});
					} else {
						WT.rawMessage(html, {title: me.res('sharingLink.embedcode.tit')});
					}
				}
			}
		});
	},
	
	editStore: function(storeId, opts) {
		opts = opts || {};
		var me = this,
				vw = WT.createView(me.ID, 'view.Store', {swapReturn: true});
		
		vw.on('viewsave', function(s, success, model) {
			Ext.callback(opts.callback, opts.scope || me, [success, model]);
		});
		vw.showView(function() {
			vw.begin('edit', {
				data: {
					storeId: storeId
				}
			});
		});
	},
	
	deleteStore: function(storeId, opts) {
		opts = opts || {};
		var me = this;
		WT.ajaxReq(me.ID, 'ManageStores', {
			params: {
				crud: 'delete',
				storeId: storeId
			},
			callback: function(success, json) {
				Ext.callback(opts.callback, opts.scope || me, [success, json.data, json]);
			}
		});
	},
	
	openFile: function(fileId) {
		Sonicle.URLMgr.openFile(WTF.processBinUrl(this.ID, 'DownloadFiles', {
			fileIds: Sonicle.Utils.toJSONArray(fileId),
			inline: true
		}));
	},
	
	downloadFiles: function(recursive, fileIds) {
		Sonicle.URLMgr.download(WTF.processBinUrl(this.ID, 'DownloadFiles', {
			recursive: recursive,
			fileIds: Sonicle.Utils.toJSONArray(fileIds)
		}));
	},
	
	createFile: function(fileId, type, name, opts) {
		opts = opts || {};
		var me = this;
		WT.ajaxReq(me.ID, 'ManageFiles', {
			params: {
				crud: 'create',
				fileId: fileId,
				type: type,
				name: name
			},
			callback: function(success, json) {
				Ext.callback(opts.callback, opts.scope || me, [success, json.data, json]);
			}
		});
	},
	
	renameFile: function(fileId, name, opts) {
		opts = opts || {};
		var me = this;
		WT.ajaxReq(me.ID, 'ManageFiles', {
			params: {
				crud: 'rename',
				fileId: fileId,
				name: name
			},
			callback: function(success, json) {
				Ext.callback(opts.callback, opts.scope || me, [success, json.data, json]);
			}
		});
	},
	
	editFile: function(fileId, opts) {
		opts = opts || {};
		var me = this;
		WT.ajaxReq(me.ID, 'ManageFiles', {
			params: {
				crud: 'edit',
				fileId: fileId
			},
			callback: function(success, json) {
				Ext.callback(opts.callback, opts.scope || me, [success, json.data, json]);
			}
		});
	},
	
	deleteFiles: function(fileIds, opts) {
		opts = opts || {};
		var me = this;
		WT.ajaxReq(me.ID, 'ManageFiles', {
			params: {
				crud: 'delete',
				fileIds: Sonicle.Utils.toJSONArray(fileIds)
			},
			callback: function(success, json) {
				Ext.callback(opts.callback, opts.scope || me, [success, json.data, json]);
			}
		});
	},
	
	showQRCode: function(fileName, linkId, opts) {
		opts = opts || {};
		var me = this,
				vw = WT.createView(me.ID, 'view.QRCodeGen', {
					swapReturn: true,
					viewCfg: {
						title: fileName,
						data: {
							linkId: linkId
						}
					}
				});
		
		vw.on('viewok', function(s, data) {
			Ext.callback(opts.callback, opts.scope || me, [data]);
		});
		vw.showView();
	},
	
	setupAutoLink: function(type, fileId, opts) {
		opts = opts || {};
		var me = this,
				action = (type === 'D') ? 'WizardDownloadLink' : 'WizardUploadLink';
		
		WT.ajaxReq(me.ID, action, {
			params: {
				crud: 'auto',
				fileId: fileId
			},
			callback: function(success, json) {
				Ext.callback(opts.callback, opts.scope || me, [success, json.data, json]);
			}
		});
	},
	
	setupLink: function(type, fileId, opts) {
		opts = opts || {};
		var me = this, vw;
		
		if(type === 'D') {
			vw = WT.createView(me.ID, 'view.DownloadLinkWiz', {
				swapReturn: true,
				viewCfg: {
					fileId: fileId
				}
			});
		} else if(type === 'U') {
			vw = WT.createView(me.ID, 'view.UploadLinkWiz', {
				swapReturn: true,
				viewCfg: {
					fileId: fileId
				}
			});
		}
		vw.on('viewclose', function(s) {
			var result = s.getVM().get('result');
			Ext.callback(opts.callback, opts.scope || me, [result !== null, result]);
		});
		vw.showView();
	},
	
	editLink: function(linkId, opts) {
		opts = opts || {};
		var me = this,
				vw = WT.createView(me.ID, 'view.SharingLink', {swapReturn: true});
		
		vw.on('viewsave', function(s, success, model) {
			Ext.callback(opts.callback, opts.scope || me, [success, model]);
		});
		vw.showView(function() {
			vw.begin('edit', {
				data: {
					linkId: linkId
				}
			});
		});
	},
	
	deleteLink: function(linkId, opts) {
		opts = opts || {};
		var me = this;
		
		WT.ajaxReq(me.ID, 'ManageSharingLink', {
			params: {
				crud: 'delete',
				id: linkId
			},
			callback: function(success, json) {
				Ext.callback(opts.callback, opts.scope || me, [success, json.data, json]);
			}
		});
	},
	
	getLinkEmbedCode: function(linkId, opts) {
		opts = opts || {};
		var me = this;
		WT.ajaxReq(me.ID, 'GetLinkEmbedCode', {
			params: {
				linkId: linkId
			},
			callback: function(success, json) {
				Ext.callback(opts.callback, opts.scope || me, [success, json.data, json]);
			}
		});
	},
	
	setupStoreFtp: function(profileId, opts) {
		opts = opts || {};
		var me = this,
			vw = WT.createView(me.ID, 'view.FtpWiz', {
				swapReturn: true,
				viewCfg: {
					profileId: profileId
				}
			});
		
			vw.on('viewclose', function(s) {
				Ext.callback(opts.callback, opts.scope || me, [true, s.getVMData()]);
			});
		if (opts.listeners) vw.on(opts.listeners);
		vw.showView();
	},
	
	setupStoreDropbox: function(profileId, opts) {
		opts = opts || {};
		var me = this,
			vw = WT.createView(me.ID, 'view.DropboxWiz', {
				swapReturn: true,
				viewCfg: {
					profileId: profileId
				}
			});
		
		if (opts.callback) {
			vw.on('viewclose', function(s) {
				Ext.callback(opts.callback, opts.scope || me, [true, s.getVMData(), s]);
			});
		}
		if (opts.listeners) vw.on(opts.listeners);
		vw.showView();
		return vw;
	},
	
	setupStoreGoogleDrive: function(profileId, opts) {
		opts = opts || {};
		var me = this,
			vw = WT.createView(me.ID, 'view.GooDriveWiz', {
				swapReturn: true,
				viewCfg: {
					profileId: profileId
				}
			});
		
		if (opts.callback) {
			vw.on('viewclose', function(s) {
				Ext.callback(opts.callback, opts.scope || me, [true, s.getVMData(), s]);
			});
		}
		if (opts.listeners) vw.on(opts.listeners);
		vw.showView();
		return vw;
	},
	
	setupStoreNextcloud: function(profileId, opts) {
		opts = opts || {};
		var me = this,
			vw = WT.createView(me.ID, 'view.NextcloudWiz', {
				swapReturn: true,
				viewCfg: {
					profileId: profileId,
					scheme: 'webdavs',
					host: me.getVar('nextcloudDefaultHost'),
					path: me.getVar('nextcloudDefaultPath')
				}
			});
		
		if (opts.callback) {
			vw.on('viewclose', function(s) {
				Ext.callback(opts.callback, opts.scope || me, [true, s.getVMData(), s]);
			});
		}
		if (opts.listeners) vw.on(opts.listeners);
		vw.showView();
		return vw;
	},
	
	setupStoreFile: function(profileId, opts) {
		opts = opts || {};
		var me = this,
			vw = WT.createView(me.ID, 'view.FileWiz', {
				swapReturn: true,
				viewCfg: {
					profileId: profileId
				}
			});
		
		if (opts.callback) {
			vw.on('viewclose', function(s) {
				Ext.callback(opts.callback, opts.scope || me, [true, s.getVMData(), s]);
			});
		}
		if (opts.listeners) vw.on(opts.listeners);
		vw.showView();
		return vw;
	},
	
	setupStoreOther: function(profileId, opts) {
		opts = opts || {};
		var me = this,
			vw = WT.createView(me.ID, 'view.OtherWiz', {
				swapReturn: true,
				viewCfg: {
					profileId: profileId
				}
			});
		
		if (opts.callback) {
			vw.on('viewclose', function(s) {
				Ext.callback(opts.callback, opts.scope || me, [true, s.getVMData(), s]);
			});
		}
		if (opts.listeners) vw.on(opts.listeners);
		vw.showView();
		return vw;
	},
	
	showSharingLinks: function(opts) {
		opts = opts || {};
		var me = this,
			vw = WT.createView(me.ID, 'view.SharingLinks', {swapReturn: true});
		
		if (opts.callback) {
			vw.on('viewclose', function(s) {
				Ext.callback(opts.callback, opts.scope || me, [true, s]);
			});
		}
		if (opts.listeners) vw.on(opts.listeners);
		vw.showView();
		return vw;
	},
	
	showDocEditingView: function(viewMode, editingCfg, opts) {
		opts = opts || {};
		var me = this,
				vw = WT.createView(WT.ID, 'view.DocEditor', {
					swapReturn: true,
					viewCfg: {
						editingId: editingCfg.editingId,
						editorConfig: {
							editable: editingCfg.writeSupported,
							token: editingCfg.token,
							docType: editingCfg.docType,
							docExtension: editingCfg.docExtension,
							docKey: editingCfg.docKey,
							docTitle: editingCfg.docName,
							docUrl: editingCfg.docUrl,
							//autosave: false,
							callbackUrl: editingCfg.callbackUrl
						}
					}
				});
		
		if (opts.callback) {
			vw.on('viewclose', function(s) {
				Ext.callback(opts.callback, opts.scope || me, [true, s]);
			});
		}
		if (opts.listeners) vw.on(opts.listeners);
		vw.showView(function() {
			vw.begin(viewMode === true ? 'view' : 'edit');
		});
		return vw;
	},
	
	showFolderChooser: function(opts) {
		opts = opts || {};
		var me = this,
			vw = WT.createView(me.ID, 'view.FolderChooser', {
				swapReturn: true
			});
		
		if (opts.callback) {
			vw.on('viewok', function(s, file) {
				Ext.callback(opts.callback, opts.scope || me, [true, file, s]);
			});
			vw.on('viewclose', function(s) {
				Ext.callback(opts.callback, opts.scope || me, [false, null, s]);
			});
		}
		if (opts.listeners) vw.on(opts.listeners);
		vw.showView();
		return vw;
	},
	
	saveMessageAttachment: function(targetFileId, targetFilename, mailAccount, mailFolder, mailMsgId, mailAttachId) {
		var me = this;
		WT.ajaxReq(me.ID, 'SaveMessageAttachment', {
			params: {
				fileId: targetFileId,
				name: targetFilename,
				mailAccount: mailAccount,
				mailFolder: mailFolder,
				mailMsgId: mailMsgId,
				mailAttachId: mailAttachId
			},
			callback: function(success, json) {
				if (success) {
					me.reloadFiles();
				} else {
					WT.error(json.message);
				}
			}
		});
	},	
	
	reloadFiles: function() {
		this.reloadGridFiles();
		this.reloadTreeFile();
	},
	
	reloadGridFilesIf: function(fileId) {
		var me = this;
		if (me.curFile === fileId) me.reloadGridFiles();
	},
	
	reloadGridFiles: function() {
		var me = this, tr, node;
		if (me.isActive()) {
			tr = me.trStores();
			node = tr.getStore().getNodeById(me.curFile);
			if (node) me.bcFiles().setSelection(node);
			Sonicle.Data.loadWithExtraParams(me.gpFiles().getStore(), {fileId: me.curFile});
		} else {
			me.needsReload = true;
		}
	},
	
	reloadTreeFile: function() {
		var me = this, sto, node;
		if (me.isActive()) {
			sto = me.trStores().getStore();
			node = sto.getNodeById(me.curFile);
			if (node) sto.load({node: node});
		} else {
			me.needsReload = true;
		}
	},
	
	loadTreeFileNode: function(node) {
		var me = this, sto, no;
		if (me.isActive()) {
			sto = me.trStores().getStore();
			if (node && node.isNode) {
				no = node;
			} else {
				no = sto.getNodeById(node || me.curFile);
			}
			if (no) sto.load({node: no});
		} else {
			me.needsReload = true;
		}
	},
	
	loadOriginNode: function(originPid) {
		var me = this,
			FT = WTA.util.FoldersTree2,
			tree = me.trStores(),
			node = FT.getOrigin(tree, originPid);
		
		if (node) {
			tree.getStore().load({node: node});
		}
	},
	
	getSelectedFile: function(forceSingle) {
		if (forceSingle === undefined) forceSingle = true;
		var sel = this.getSelectedFiles();
		if (forceSingle && sel.length !== 1) return null;
		return (sel.length > 0) ? sel[0] : null;
	},
	
	getSelectedFiles: function() {
		return this.gpFiles().getSelection();
	},
	
	selectionIds: function(sel, filterByType) {
		var ids = [];
		Ext.iterate(sel, function(rec) {
			if (!Ext.isEmpty(filterByType)) {
				if ((filterByType === 'file') && !rec.isFOTypeFile()) return;
				if ((filterByType === 'folder') && !rec.isFOTypeFolder()) return;
			}
			ids.push(rec.getId());
		});
		return ids;
	},
	
	/**
	 * @private
	 */
	updateDisabled: function(action) {
		var me = this,
			dis = me.isDisabled(action);
		me.setActDisabled(action, dis);
	},
	
	/**
	 * @private
	 */
	isDisabled: function(action) {
		var me = this, node, sel;
		switch(action) {
			case 'createFileNode':
				node = me.getCurrentFileNode();
				if (node) {
					return !node.getFORights().CREATE;
				} else {
					return true;
				}
			case 'renameFileNode':
				node = me.getCurrentFileNode();
				if (node) {
					return !node.getFORights().UPDATE;
				} else {
					return true;
				}
			case 'deleteFileNode':
				node = me.getCurrentFileNode();
				if (node) {
					return !node.getFORights().DELETE;
				} else {
					return true;
				}
			case 'addFileNodeDlLink':
				node = me.getCurrentFileNode();
				if (node) {
					return !Ext.isEmpty(node.getFODlLink());
				} else {
					return true;
				}
			case 'deleteFileNodeDlLink':
			case 'sendFileNodeDlLink':
				node = me.getCurrentFileNode();
				if (node) {
					return Ext.isEmpty(node.getFODlLink());
				} else {
					return true;
				}
			case 'addFileNodeUlLink':
				node = me.getCurrentFileNode();
				if (node) {
					return !Ext.isEmpty(node.getFOUlLink());
				} else {
					return true;
				}
			case 'deleteFileNodeUlLink':
			case 'sendFileNodeUlLink':
				node = me.getCurrentFileNode();
				if (node) {
					return Ext.isEmpty(node.getFOUlLink());
				} else {
					return true;
				}
			case 'openFile':
				sel = me.getSelectedFiles();
				if (sel.length === 1) {
					return !sel[0].isFOOpenable();
				} else {
					return true;
				}
			case 'editFile':
			case 'editFileView':
				sel = me.getSelectedFiles();
				if (sel.length === 1) {
					return sel[0].isFOEditable() ? !sel[0].getFORights().UPDATE : true;
				} else {
					return true;
				}
			case 'editFileEdit':
				sel = me.getSelectedFiles();
				if (sel.length === 1) {
					return sel[0].isFOEditable() ? false : true;
				} else {
					return true;
				}
			case 'createFolder':
			case 'createDocument':
			case 'createFileTxt':
			case 'createFileDocx':
			case 'createFileXlsx':
			case 'createFilePptx':
			case 'createFileOdt':
			case 'createFileOds':
			case 'createFileOdp':
			case 'createFileHtml':
				node = me.getCurrentFileNode();
				if (node) {
					return !(node.getFORights().CREATE && node.getFORights().UPDATE);
				} else {
					return true;
				}
			case 'downloadFile':
				sel = me.getSelectedFiles();
				if (sel.length === 1) {
					return false;
				} else {
					return true;
				}
			case 'createFile':
				sel = me.getSelectedFiles();
				if (sel.length === 1) {
					return sel[0].isFOTypeFolder() ? true : !sel[0].getFORights().CREATE;
				} else {
					return true;
				}
			case 'renameFile':
				sel = me.getSelectedFiles();
				if (sel.length === 1) {
					return !sel[0].getFORights().UPDATE;
				} else {
					return true;
				}
			case 'deleteFile':
				sel = me.getSelectedFiles();
				if (sel.length > 0) {
					return !sel[0].getFORights().DELETE;
				} else {
					return true;
				}
			case 'addFileDlLink':
				sel = me.getSelectedFiles();
				if (sel.length === 1) {
					return !Ext.isEmpty(sel[0].getFODlLink());
				} else {
					return true;
				}
			case 'deleteFileDlLink':
			case 'sendFileDlLink':
				sel = me.getSelectedFiles();
				if (sel.length === 1) {
					return Ext.isEmpty(sel[0].getFODlLink());
				} else {
					return true;
				}
			case 'addFileUlLink':
				sel = me.getSelectedFiles();
				if (sel.length === 1) {
					if (!sel[0].getFORights().CREATE) return true;
					return (!sel[0].isFOTypeFolder()) ? true : !Ext.isEmpty(sel[0].getFOUlLink());
				} else {
					return true;
				}
			case 'deleteFileUlLink':
			case 'sendFileUlLink':
				sel = me.getSelectedFiles();
				if (sel.length === 1) {
					if (!sel[0].getFORights().CREATE) return true;
					return Ext.isEmpty(sel[0].getFOUlLink());
				} else {
					return true;
				}
		}
	},
	
	privates: {
		onActivate: function() {
			var me = this,
				tree = me.trStores(),
				node;

			if (me.getActivationCount() === 1) {
				node = me.findMyDocsBuiltInFolder(tree);
				if (node) {
					me.setCurFile(node.getId());
				} else {
					tree.getStore().on('load', me.onTreeLoadMyDocsFolder, me);
				}
			}
		},

		onTreeLoadMyDocsFolder: function(s) {
			var me = this,
				node = me.findMyDocsBuiltInFolder(me.trStores());
			if (node) {
				me.setCurFile(node.getId());
				s.un('load', me.onTreeLoadMyDocsFolder, me);
			}
		},
		
		findMyDocsBuiltInFolder: function(tree) {
			return WTA.util.FoldersTree2.getBuiltInFolder(tree, function(n) {
				return (n.get('_builtIn') === 100);
			});
		},
		
		isEditActionEdit: function() {
			return this.getVar('fileEditAction') === 'edit';
		},
		
		isFilenameDuplicated: function(name) {
			var me = this, found = false;
			me.gpFiles().getStore().each(function(rec) {
				if (rec.isFOTypeFile() && name === rec.getFOName()) {
					found = true;
					return false;
				}
			}, me, true);
			return found;
		},
		
		confirmDuplicateMode: function(cb, scope) {
			var me = this;
			WT.confirm(me.res('confirm.duplicate.msg'), cb, scope, {
				buttons: Ext.Msg.OKCANCEL,
				instClass: 'Sonicle.webtop.vfs.ux.DuplicateConfirmBox',
				instConfig: {
					renameText: me.res('confirm.duplicate.rename'),
					ignoreText: me.res('confirm.duplicate.ignore'),
					overwriteText: me.res('confirm.duplicate.overwrite')
				},
				config: {
					value: 'rename'
				}
			});
		}
	},
	
	createGrid: function(gridId, store) {
		var me = this;
			var grid = Ext.create('Ext.grid.Panel', {
				region: 'center',
				id: gridId,
				reference: 'gpfiles',
				cls: 'wtvfs-gpfiles',
				stateful: true,
				stateId: me.buildStateId('gpfiles'),
				store: {
					model: 'Sonicle.webtop.vfs.model.GridFile',
					proxy: WTF.apiProxy(me.ID, 'ManageGridFiles', 'files', {
						extraParams: {
							fileId: null
						}
					})
				},
				viewConfig: {
					deferEmptyText: false,
					emptyText: me.res('gpfiles.emp'),
					plugins: [{
						ptype: 'sogridviewdragdrop',
						dragGroup: 'wtvfs-storefile',
						//TODO: maybe format group names in a better way!
						dropGroup: 'attachment',
						isDragAllowed: function(view, rec) {
							var sel = view.getSelection();
							if (sel && (sel.length > 1)) return false;
							if (rec && rec.isFOTypeFolder()) return false;
							return true;
						},
						getDragData: function(view, data) {
							var rec = data.itemRecord;
							if (rec) {
								return {
									storeFile: {
										storeId: rec.get('storeId'),
										path: rec.get('path'),
										type: rec.get('type'),
										name: rec.getFOName(),
										mediaType: rec.get('mtype'),
										size: rec.get('size')
									}
								};
							} else {
								return {};
							}
						},
						getDragText: function(view, data) {
							var rec = data.itemRecord;
							return rec ? Sonicle.String.htmlEncodeWhitespaces(Ext.String.htmlEncode(rec.getFOName())) : null;
						}, 
						handleNodeDrop: function(data) {
							var att = data.msgAttachment;
							me.saveMessageAttachment(me.curNode, att.attachName, att.accountId, att.folder, att.msgId, att.attachId);
						}
					}]
				},
				selModel: {
					type: 'checkboxmodel',
					mode : 'MULTI'
				},
				columns: [{
					xtype: 'soiconcolumn',
					dataIndex: 'type',
					header: WTF.headerWithGlyphIcon('far fa-file'),
					getIconCls: function(v,rec) {
						return rec.isFOTypeFolder() ? 'wt-ftype-folder' : WTF.fileTypeCssIconCls(rec.get('ext'));
					},
					iconSize: 16,
					width: 40
				}, {
					xtype: 'solinkcolumn',
					dataIndex: 'name',
					header: me.res('gpfiles.name.lbl'),
					tdCls: 'wt-theme-text-hyperlink',
					preserveWhitespaces: true,
					listeners: {
						linkclick: function(s,idx,rec) {
							me.followGridFile(rec);
						}
					},
					flex: 1
				}, {
					xtype: 'sobytescolumn',
					dataIndex: 'size',
					header: me.res('gpfiles.size.lbl'),
					width: 110
				}, {
					dataIndex: 'lastModified',
					header: me.res('gpfiles.lastModified.lbl'),
					xtype: 'datecolumn',
					format: WT.getShortDateTimeFmt(),
					width: 140
				}, {
					xtype: 'soiconcolumn',
					dataIndex: 'dlLink',
					header: WTF.headerWithGlyphIcon('fas fa-cloud-download-alt'),
					getIconCls: function(v,rec) {
						if (Ext.isEmpty(v)) return '';
						var exp = rec.get('dlLinkExp') ? 'Exp' : '';
						return me.cssIconCls('downloadLink'+exp);
					},
					getTip: function(v,rec) {
						if (Ext.isEmpty(v)) return '';
						var exp = rec.get('dlLinkExp') ? '.exp' : '';
						return me.res('gpfiles.dlLink'+exp);
					},
					iconSize: 16,
					width: 40
				}, {
					xtype: 'soiconcolumn',
					dataIndex: 'ulLink',
					header: WTF.headerWithGlyphIcon('fas fa-cloud-upload-alt'),
					getIconCls: function(v,rec) {
						if (Ext.isEmpty(v)) return '';
						var exp = rec.get('ulLinkExp') ? 'Exp' : '';
						return me.cssIconCls('uploadLink'+exp);
					},
					getTip: function(v,rec) {
						if (Ext.isEmpty(v)) return '';
						var exp = rec.get('ulLinkExp') ? '.exp' : '';
						return me.res('gpfiles.ulLink'+exp);
					},
					iconSize: 16,
					width: 40
				}],
				tbar: [
					me.getAct('goUp'),
					{
						xtype: 'sobreadcrumb',
						reference: 'bcfiles',
						store: store,
						overflowHandler: 'scroller',
						minDepth: 2,
						listeners: {
							change: function(s, node) {
								if (node) {
									me.setCurFile(node.getId());
									me.reloadGridFiles();
								}
							}
						},
						flex: 1
					},
					me.getActAs('renameFile', 'button', {text: null, tooltip: me.res('act-renameFile.lbl')}),
					me.getActAs('deleteFile', 'button', {text: null, tooltip: me.res('act-deleteFile.lbl')}),
					'-',
					me.getActAs('createFolder', 'button', {text: null, tooltip: me.res('act-createFileFolder.lbl')}),
					me.getActAs('createDocument', 'button', {
						text: null,
						tooltip: me.res('act-createDocument.lbl'),
						hidden: !WT.getVar('docServerEnabled'),
						menu: {
							items: [
								me.getAct('createFileDocx'),
								me.getAct('createFileXlsx'),
								me.getAct('createFilePptx'),
								me.getAct('createFileTxt'),
								me.getAct('createFileHtml')
								//me.getAct('createFileOdt'),
								//me.getAct('createFileOds'),
								//me.getAct('createFileOdp')
							]
						}
					})
				],
				bbar: {
					xtype: 'wtuploadbar',
					reference: 'tbupload',
					sid: me.ID,
					uploadContext: 'UploadStoreFile',
					autoStart: false,
					maxFileSize: me.getVar('privateUploadMaxFileSize'),
					buttonIconCls: me.cssIconCls('uploadFile'),
					dropElement: gridId,
					fileExtraParams: function() {
						return {
							fileId: me.curFile
						};
					},
					listeners: {
						filesadded: function(s, files) {
							var prompt = false,
									uploader = s.getUploader();
							
							Ext.iterate(files, function(file) {
								if (me.isFilenameDuplicated(file.name)) {
									prompt = true;
									return false;
								}
							}, me);
							
							if (prompt) {
								me.confirmDuplicateMode(function(bid, value) {
									if (bid === 'ok') {
										Ext.iterate(files, function(file) {
											file._extraParams['dupl'] = value;
										}, me);
										uploader.start();
									} else {
										Ext.iterate(files, function(file) {
											uploader.removeFile(file.id);
										}, me);
									}
								});
							} else {
								uploader.start();
							}	
						},
						fileuploaded: function(s, file) {
							if (file._extraParams) {
								me.reloadGridFilesIf(file._extraParams['fileId']);
							}
						}
					},
					disabled: true
				},
				plugins: [{
					ptype: 'sofiledrop',
					text: WT.res('sofiledrop.text')
				}],
				listeners: {
					selectionchange: function() {
						me.updateDisabled('renameFile');
						me.updateDisabled('deleteFile');
					},
					containercontextmenu: function(s, e) {
						Sonicle.Utils.showContextMenu(e, me.getRef('cxmGridFile0'));
					},
					rowdblclick: function(s, rec, el, ridx, e) {
						if (e.position.colIdx === 0) return; // Skip dbl-click on selection cleckbox
						me.followGridFile(rec);
					},
					rowcontextmenu: function(s, rec, itm, i, e) {
						var sm = s.getSelectionModel();
						if(sm.getSelection().length <= 1) sm.select(rec);
						Sonicle.Utils.showContextMenu(e, me.getRef('cxmGridFile'), {
							file: rec,
							files: s.getSelection()
						});
					}
				}
			});
			return grid;
	}
});
