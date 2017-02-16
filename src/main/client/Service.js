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
		'Sonicle.grid.column.Bytes',
		'Sonicle.grid.column.Icon',
		'Sonicle.grid.column.Link',
		'Sonicle.toolbar.Breadcrumb',
		'Sonicle.upload.Button',
		'WTA.ux.data.EmptyModel',
		'WTA.ux.data.SimpleModel',
		'Sonicle.webtop.vfs.model.StoreNode',
		'Sonicle.webtop.vfs.model.GridFile',
		'Sonicle.webtop.vfs.view.Sharing',
		'Sonicle.webtop.vfs.view.SharingLinks',
		
		'Sonicle.plugin.FileDrop',
		'Sonicle.webtop.vfs.ux.UploadToolbar',
		'Sonicle.webtop.vfs.model.SharingLink'
	],
	mixins: [
		//'WTA.mixin.FoldersTree'
	],
	
	needsReload: true,
	curNode: null,
	curFile: null,
	
	setCurNode: function(nodeId) {
		this.curNode = nodeId;
		console.log('current node: '+nodeId);
	},
	
	getCurrentFileNode: function() {
		var id = this.curNode;
		return id ? this.trStores().getStore().getNodeById(id) : null;
	},
	
	setCurFile: function(fileId) {
		var me = this, tr, node, tbu;
		console.log('current file: '+fileId);
		
		if(fileId !== me.curFile) {
			tbu = me.tbUpload();
			me.curFile = fileId;
			if(fileId) {
				tr = me.trStores();
				node = tr.getStore().getNodeById(fileId);
				if(node) {
					tbu.setDisabled(!node.getEPerms().CREATE);
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
				me.getAction('showSharingLinks')
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
					autoSync: true,
					model: 'Sonicle.webtop.vfs.model.StoreNode',
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
					selectionchange: function(s, rec) {
						//console.log('selectionchange');
						var type = (rec.length === 1) ? rec[0].get('_type') : null;
						if(type === 'folder') {
							me.setCurNode(rec[0].getId());
						} else if(type === 'file') {
							me.setCurNode(rec[0].getId());
						} else {
							me.setCurNode(null);
						}
					},
					itemclick: function(s, rec, itm, i, e) {
						//console.log('itemclick');
						var type = rec.get('_type');
						if(type === 'folder') { // Store node
							me.setCurFile(rec.getId());
						} else if(type === 'file') { // File(folder) node
							me.setCurFile(rec.getId());
						}
					},
					itemcontextmenu: function(s, rec, itm, i, e) {
						//console.log('itemcontextmenu');
						var type = rec.get('_type');
						if(type === 'root') {
							me.setCurNode(rec.get('id'));
							WT.showContextMenu(e, me.getRef('cxmRootStore'), {node: rec});
						} else if(type === 'folder') {
							me.setCurNode(rec.get('id'));
							WT.showContextMenu(e, me.getRef('cxmStore'), {node: rec});
						} else if(type === 'file') {
							me.setCurNode(rec.get('id'));
							WT.showContextMenu(e, me.getRef('cxmFile'), {node: rec});
						} else {
							me.setCurNode(null);
						}
					}
				}
			}]
		}));
		
		var sto = me.trStores().getStore(),
			gpId = Ext.id(null, 'gridpanel');
		
		me.setMainComponent(Ext.create({
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
			items: [{
				region: 'center',
				xtype: 'grid',
				id: gpId,
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
					emptyText: me.res('gpfiles.emp')
				},
				selModel: {
					type: 'checkboxmodel',
					mode : 'MULTI'
				},
				columns: [{
					xtype: 'soiconcolumn',
					dataIndex: 'type',
					header: WTF.headerWithGlyphIcon('fa fa-file-o'),
					getIconCls: function(v,rec) {
						return (v === 'folder') ? 'wt-ftype-folder-xs' : WTF.fileTypeCssIconCls(rec.get('ext'), 'xs');
					},
					iconSize: WTU.imgSizeToPx('xs'),
					width: 40
				}, {
					xtype: 'solinkcolumn',
					dataIndex: 'name',
					header: me.res('gpfiles.name.lbl'),
					flex: 1,
					listeners: {
						linkclick: function(s,idx,rec) {
							if(rec.get('type') === 'folder') {
								me.setCurFile(rec.get('fileId'));
								me.reloadGridFiles();
							} else if(rec.get('type') === 'file') {
								me.downloadFiles([rec.get('fileId')]);
							}
						}
					},
					maxWidth: 500
				}, {
					xtype: 'sobytescolumn',
					dataIndex: 'size',
					header: me.res('gpfiles.size.lbl'),
					width: 110
				}, {
					dataIndex: 'lastModified',
					header: me.res('gpfiles.lastModified.lbl'),
					xtype: 'datecolumn',
					format: WT.getShortDateFmt() + ' ' + WT.getShortTimeFmt(),
					width: 140
				}, {
					xtype: 'soiconcolumn',
					dataIndex: 'dlLink',
					header: WTF.headerWithGlyphIcon('fa fa-cloud-download'),
					getIconCls: function(v,rec) {
						var exp = rec.get('dlLinkExp') ? 'Exp' : '';
						return Ext.isEmpty(v) ? '' : me.cssIconCls('downloadLink'+exp, 'xs');
					},
					getTip: function(v,rec) {
						var exp = rec.get('dlLinkExp') ? '.exp' : '';
						return me.res('gpfiles.dlLink'+exp);
					},
					iconSize: WTU.imgSizeToPx('xs'),
					width: 40
				}, {
					xtype: 'soiconcolumn',
					dataIndex: 'ulLink',
					header: WTF.headerWithGlyphIcon('fa fa-cloud-upload'),
					getIconCls: function(v,rec) {
						var exp = rec.get('ulLinkExp') ? 'Exp' : '';
						return Ext.isEmpty(v) ? '' : me.cssIconCls('uploadLink'+exp, 'xs');
					},
					getTip: function(v,rec) {
						var exp = rec.get('dlLinkExp') ? '.exp' : '';
						return me.res('gpfiles.ulLink'+exp);
					},
					iconSize: WTU.imgSizeToPx('xs'),
					width: 40
				}],
				tbar: [
					me.getAction('goUp'),
					{
						xtype: 'sobreadcrumb',
						reference: 'bcfiles',
						store: sto,
						overflowHandler: 'scroller',
						minDepth: 2,
						listeners: {
							change: function(s, node) {
								console.log('breadchange');
								if(node) {
									me.setCurFile(node.getId());
									me.reloadGridFiles();
								}
							}
						},
						flex: 1
					}
				],
				bbar: {
					xtype: 'wtvfsuploadtoolbar',
					reference: 'tbupload',
					mys: me,
					dropElement: gpId,
					fileExtraParams: function() {
						return {
							fileId: me.curFile
						};
					},
					listeners: {
						fileuploaded: function(s, file) {
							if(file._extraParams) {
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
					/*
					selectionchange: function() {
						//me.updateCxmGridFile();
					},
					rowdblclick: function(s, rec) {
						//var er = me.toPermsObj(rec.get('_erights'));
						//me.showTask(er.UPDATE, rec.get('taskId'));
					},
					*/
					containercontextmenu: function(s, e) {
						WT.showContextMenu(e, me.getRef('cxmGridFile0'));
					},
					rowcontextmenu: function(s, rec, itm, i, e) {
						var sm = s.getSelectionModel();
						if(sm.getSelection().length <= 1) sm.select(rec);
						WT.showContextMenu(e, me.getRef('cxmGridFile'), {
							file: rec,
							files: s.getSelection()
						});
					}
				}
			}/*, {
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
		}));
	},
	
	expandFile: function(fileId) {
		var me = this,
				tree = me.trStores(),
				node = tree.getStore().getNodeById(fileId);
		console.log('expanding file');
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
	
	initActions: function() {
		var me = this;
		me.addAction('showSharingLinks', {
			iconCls: me.cssIconCls('sharingLink', 'xs'),
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
		me.addAction('editSharing', {
			text: WT.res('sharing.tit'),
			iconCls: WTF.cssIconCls(WT.XID, 'sharing', 'xs'),
			handler: function() {
				var node = me.getCurrentFileNode();
				if(node) me.editShare(node.getId());
			}
		});
		me.addAction('addStoreFtp', {
			handler: function() {
				var node = me.getCurrentFileNode();
				if(node) me.addStoreUI('ftp', node);
			}
		});
		me.addAction('addStoreDropbox', {
			handler: function() {
				var node = me.getCurrentFileNode();
				if(node) me.addStoreUI('dropbox', node);
			}
		});
		me.addAction('addStoreGooDrive', {
			handler: function() {
				var node = me.getCurrentFileNode();
				if(node) me.addStoreUI('goodrive', node);
			}
		});
		me.addAction('addStoreFile', {
			handler: function() {
				var node = me.getCurrentFileNode();
				if(node) me.addStoreUI('file', node);
			}
		});
		me.addAction('addStoreOther', {
			handler: function() {
				var node = me.getCurrentFileNode();
				if(node) me.addStoreUI('other', node);
			}
		});
		me.addAction('editStore', {
			handler: function() {
				var node = me.getCurrentFileNode();
				if(node) me.editStoreUI(node);
			}
		});
		me.addAction('deleteStore', {
			handler: function() {
				var node = me.getCurrentFileNode();
				if(node) me.deleteStoreUI(node);
			}
		});
		me.addAction('createFileNode', {
			text: me.res('act-createFileFolder.lbl'),
			iconCls: me.cssIconCls('createFileFolder', 'xs'),
			handler: function() {
				var node = me.getCurrentFileNode();
				if(node) me.createFileUI(node);
			}
		});
		me.addAction('renameFileNode', {
			text: me.res('act-renameFile.lbl'),
			iconCls: me.cssIconCls('renameFile', 'xs'),
			handler: function() {
				var node = me.getCurrentFileNode();
				if(node) me.renameFileUI(node);
			}
		});
		me.addAction('deleteFileNode', {
			text: me.res('act-deleteFile.lbl'),
			iconCls: me.cssIconCls('deleteFile', 'xs'),
			handler: function() {
				var node = me.getCurrentFileNode();
				if(node) me.deleteFileNodeUI(node);
			}
		});
		me.addAction('addFileNodeDlLink', {
			text: me.res('act-addFileDlLink.lbl'),
			iconCls: me.cssIconCls('addFileDlLink', 'xs'),
			handler: function() {
				var node = me.getCurrentFileNode();
				if(node) me.addFileLinkUI('D', node);
			}
		});
		me.addAction('deleteFileNodeDlLink', {
			text: me.res('act-deleteFileDlLink.lbl'),
			iconCls: me.cssIconCls('deleteFileDlLink', 'xs'),
			handler: function() {
				var node = me.getCurrentFileNode();
				if(node) me.deleteFileLinkUI('D', node);
			}
		});
		me.addAction('sendFileNodeDlLink', {
			text: me.res('act-sendFileDlLink.lbl'),
			handler: function() {
				var node = me.getCurrentFileNode();
				if(node) me.sendFileLinkUI('D', node);
			}
		});
		me.addAction('addFileNodeUlLink', {
			text: me.res('act-addFileUlLink.lbl'),
			iconCls: me.cssIconCls('addFileUlLink', 'xs'),
			handler: function() {
				var sel = me.getCurrentFileNode();
				if(sel) me.addFileLinkUI('U', sel);
			}
		});
		me.addAction('deleteFileNodeUlLink', {
			text: me.res('act-deleteFileUlLink.lbl'),
			iconCls: me.cssIconCls('deleteFileUlLink', 'xs'),
			handler: function() {
				var sel = me.getCurrentFileNode();
				if(sel) me.deleteFileLinkUI('U', sel);
			}
		});
		me.addAction('sendFileNodeUlLink', {
			text: me.res('act-sendFileUlLink.lbl'),
			handler: function() {
				var node = me.getCurrentFileNode();
				if(node) me.sendFileLinkUI('U', node);
			}
		});
		me.addAction('goUp', {
			text: null,
			handler: function() {
				var bc = me.bcFiles(),
						node = bc.getSelectionParent();
				if(node) bc.setSelection(node);
			}
		});
		me.addAction('openFile', {
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.openFileUI(sel);
			}
		});
		me.addAction('downloadFile', {
			handler: function() {
				var sel = me.getSelectedFiles();
				if(sel.length > 0) me.downloadFilesUI(sel);
			}
		});
		me.addAction('renameFile', {
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.renameFileUI(sel);
			}
		});
		me.addAction('deleteFile', {
			handler: function() {
				var sel = me.getSelectedFiles();
				if(sel.length > 0) me.deleteFilesUI(sel);
			}
		});
		me.addAction('addFileDlLink', {
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.addFileLinkUI('D', sel);
			}
		});
		me.addAction('deleteFileDlLink', {
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.deleteFileLinkUI('D', sel);
			}
		});
		me.addAction('sendFileDlLink', {
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.sendFileLinkUI('D', sel);
			}
		});
		me.addAction('addFileUlLink', {
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.addFileLinkUI('U', sel);
			}
		});
		me.addAction('deleteFileUlLink', {
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.deleteFileLinkUI('U', sel);
			}
		});
		me.addAction('sendFileUlLink', {
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.sendFileLinkUI('U', sel);
			}
		});
	},
	
	/*
	updateCxmFile: function() {
		var me = this;
		me.updateDisabled('renameFileNode');
		me.updateDisabled('deleteFileNode');
		me.updateDisabled('createFileNode');
		me.updateDisabled('addFileNodeDlLink');
		me.updateDisabled('deleteFileNodeDlLink');
		//me.updateDisabled('sendFolderDlLink');
		me.updateDisabled('addFileNodeUlLink');
		me.updateDisabled('deleteFileNodeUlLink');
		//me.updateDisabled('sendFolderUlLink');
	},
	
	updateCxmGridFile: function() {
		var me = this;
		me.updateDisabled('openFile');
		me.updateDisabled('downloadFile');
		me.updateDisabled('renameFile');
		me.updateDisabled('addFileDlLink');
		me.updateDisabled('deleteFileDlLink');
		//me.updateDisabled('sendDownloadLink');
		me.updateDisabled('addFileUlLink');
		me.updateDisabled('deleteFileUlLink');
		//me.updateDisabled('sendUploadLink');
	},
	*/
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	addStoreUI: function(type, node) {
		var me = this, pid = node.get('_pid');
		if(type === 'ftp') {
			me.setupStoreFtp(pid, {
				callback: function(success) {
					if(success) me.loadTreeRootNode(pid);
				}
			});
		} else if(type === 'dropbox') {
			me.setupStoreDropbox(pid, {
				callback: function(success) {
					if(success) me.loadTreeRootNode(pid);
				}
			});
		} else if(type === 'goodrive') {
			me.setupStoreGoogleDrive(pid, {
				callback: function(success) {
					if(success) me.loadTreeRootNode(pid);
				}
			});
		} else if(type === 'file') {
			me.setupStoreFile(pid, {
				callback: function(success) {
					if(success) me.loadTreeRootNode(pid);
				}
			});
		} else if(type === 'other') {
			me.setupStoreOther(pid, {
				callback: function(success) {
					if(success) me.loadTreeRootNode(pid);
				}
			});
		}
	},
	
	editStoreUI: function(node) {
		var me = this;
		me.editStore(node.get('_storeId'), {
			callback: function(success) {
				if(success) me.loadTreeRootNode(node.get('_pid'));
			}
		});
	},
	
	deleteStoreUI: function(node) {
		var me = this,
				sto = me.trStores().getStore();
		WT.confirm(me.res('store.confirm.delete', Ext.String.ellipsis(node.get('text'), 40)), function(bid) {
			if(bid === 'yes') {
				me.deleteStore(node.get('_storeId'), {
					callback: function(success) {
						if(success) {
							node.remove();
							//sto.remove(node);
							if(me.isInCurFile(node.getFId())) me.setCurFile(null);
						}
					}
				});
			}
		});
	},
	
	openFileUI: function(sel) {
		
	},
	
	downloadFilesUI: function(sel) {
		var me = this,
			ids = me.selectionIds(sel);
		me.downloadFiles(ids);
	},
	
	createFileUI: function(sel) {
		var me = this;
		WT.prompt(me.res('gpfiles.name.lbl'), {
			title: me.res('act-createFileFolder.lbl'),
			value: '',
			fn: function(bid, name) {
				if(bid === 'ok') {
					me.createFile(sel.getFId(), name, {
						callback: function(success) {
							if(success) {
								if(sel.isNode) {
									me.loadTreeFileNode(sel);
									me.reloadGridFiles();
								} else {
									me.loadTreeFileNode(sel.getFId());
									me.reloadGridFiles();
								}
							}
						}
					});
				}
			}
		});
	},
	
	renameFileUI: function(sel) {
		var me = this, pfid;
		WT.prompt(me.res('gpfiles.name.lbl'), {
			title: me.res('act-renameFile.lbl'),
			value: sel.getFName(),
			fn: function(bid, name) {
				if(bid === 'ok') {
					if(name !== sel.getFName()) {
						me.renameFile(sel.getFId(), name, {
							callback: function(success) {
								if(success) {
									if(sel.isNode) {
										pfid = sel.parentNode.getFId(); // Keep here before load
										me.loadTreeFileNode(sel.parentNode);
										if(me.isInCurFile(sel.getFId())) {
											me.setCurFile(pfid);
											me.reloadGridFiles();
										} else {
											me.reloadGridFiles();
										}
									} else {
										me.loadTreeFileNode();
										me.reloadGridFiles();
									}
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
		
		WT.confirm(me.res('gpfiles.confirm.folder.delete', Ext.String.ellipsis(node.getFName(), 40)), function(bid) {
			if(bid === 'yes') {
				me.deleteFiles([node.getFId()], {
					callback: function(success) {
						if(success) {
							if(me.curFile && me.curFile.indexOf(node.getFId()) > -1) {
								// Files grid is currently displaying node being deleted
								me.setCurFile(node.parentNode.getFId());
								me.reloadGridFiles();
							} else {
								sto2 = me.gpFiles().getStore();
								rec2 = sto2.getById(node.getFId());
								if(rec2) sto2.remove(rec2);
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
		
		if(sel.length === 1) {
			msg = me.res('gpfiles.confirm.'+sel[0].getFType()+'.delete', Ext.String.ellipsis(sel[0].getFName(), 40));
		} else {
			msg = me.res('gpfiles.confirm.delete.selection');
		}
		WT.confirm(msg, function(bid) {
			if(bid === 'yes') {
				me.deleteFiles(ids, {
					callback: function(success) {
						if(success) {
							sto.remove(sel);
							me.loadTreeFileNode();
						}
					}
				});
			}
		});
	},
	
	addFileLinkUI: function(type, sel) {
		var me = this;
		me.setupLink(type, sel.getFId(), {
			callback: function(success) {
				if(success) {
					if(sel.isNode) {
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
				linkId = (type === 'D') ? sel.getFDlLink() : sel.getFUlLink(),
				rec2;
		WT.confirm(me.res('sharingLink.confirm.delete'), function(bid) {
			if(bid === 'yes') {
				me.deleteLink(linkId, {
					callback: function(success) {
						if(success) {
							if(sel.isNode) {
								rec2 = me.gpFiles().getStore().getById(sel.getFId());
							} else {
								rec2 = me.trStores().getStore().getNodeById(sel.getFId());
							}
							if(type === 'D') {
								sel.setFDlLink(null);
								if(rec2) rec2.setFDlLink(null);
							} else if(type === 'U') {
								sel.setFUlLink(null);
								if(rec2) rec2.setFUlLink(null);
							}
						}
					}
				});
			}
		});
	},
	
	sendFileLinkUI: function(type, sel) {
		WT.warn(WT.res('warn.todo'));
	},
	
	editShare: function(id) {
		var me = this,
				vct = WT.createView(me.ID, 'view.Sharing');
		
		vct.show(false, function() {
			vct.getView().begin('edit', {
				data: {
					id: id
				}
			});
		});
	},
	
	editStore: function(storeId, opts) {
		opts = opts || {};
		var me = this,
				vct = WT.createView(me.ID, 'view.Store');
		
		vct.getView().on('viewsave', function(s, success, model) {
			Ext.callback(opts.callback, opts.scope || me, [success, model]);
		});
		vct.show(false, function() {
			vct.getView().begin('edit', {
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
	
	openfile: function(fileId) {
		
	},
	
	downloadFiles: function(fileIds) {
		Sonicle.URLMgr.download(WTF.processBinUrl(this.ID, 'DownloadFiles', {
			fileIds: WTU.arrayAsParam(fileIds)
		}));
	},
	
	createFile: function(fileId, name, opts) {
		opts = opts || {};
		var me = this;
		WT.ajaxReq(me.ID, 'ManageFiles', {
			params: {
				crud: 'create',
				fileId: fileId,
				type: 'folder',
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
	
	deleteFiles: function(fileIds, opts) {
		opts = opts || {};
		var me = this;
		WT.ajaxReq(me.ID, 'ManageFiles', {
			params: {
				crud: 'delete',
				fileIds: WTU.arrayAsParam(fileIds)
			},
			callback: function(success, json) {
				Ext.callback(opts.callback, opts.scope || me, [success, json.data, json]);
			}
		});
	},
	
	setupLink: function(type, fileId, opts) {
		opts = opts || {};
		var me = this, vct;
		
		if(type === 'D') {
			vct = WT.createView(me.ID, 'view.DownloadLinkWiz', {
				viewCfg: {
					fileId: fileId
				}
			});
		} else if(type === 'U') {
			vct = WT.createView(me.ID, 'view.UploadLinkWiz', {
				viewCfg: {
					fileId: fileId
				}
			});
		}
		vct.getView().on('viewclose', function(s) {
			var result = s.getVM().get('result');
			Ext.callback(opts.callback, opts.scope || me, [result !== null, result]);
		});
		vct.show();
	},
	
	editLink: function(linkId, opts) {
		opts = opts || {};
		var me = this,
				vct = WT.createView(me.ID, 'view.SharingLink');
		
		vct.getView().on('viewsave', function(s, success, model) {
			Ext.callback(opts.callback, opts.scope || me, [success, model]);
		});
		vct.show(false, function() {
			vct.getView().begin('edit', {
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
	
	setupStoreFtp: function(profileId, opts) {
		opts = opts || {};
		var me = this,
			vct = WT.createView(me.ID, 'view.FtpWiz', {
				viewCfg: {
					profileId: profileId
				}
			});
		
		vct.getView().on('viewclose', function(s) {
			Ext.callback(opts.callback, opts.scope || me, [true, s.getVMData()]);
		});
		vct.show();
	},
	
	setupStoreDropbox: function(profileId, opts) {
		opts = opts || {};
		var me = this,
			vct = WT.createView(me.ID, 'view.DropboxWiz', {
				viewCfg: {
					profileId: profileId
				}
			});
		
		vct.getView().on('viewclose', function(s) {
			Ext.callback(opts.callback, opts.scope || me, [true, s.getVMData()]);
		});
		vct.show();
	},
	
	setupStoreGoogleDrive: function(profileId, opts) {
		opts = opts || {};
		var me = this,
			vct = WT.createView(me.ID, 'view.GooDriveWiz', {
				viewCfg: {
					profileId: profileId
				}
			});
		
		vct.getView().on('viewclose', function(s) {
			Ext.callback(opts.callback, opts.scope || me, [true, s.getVMData()]);
		});
		vct.show();
	},
	
	setupStoreFile: function(profileId, opts) {
		opts = opts || {};
		var me = this,
			vct = WT.createView(me.ID, 'view.FileWiz', {
				viewCfg: {
					profileId: profileId
				}
			});
		
		vct.getView().on('viewclose', function(s) {
			Ext.callback(opts.callback, opts.scope || me, [true, s.getVMData()]);
		});
		vct.show();
	},
	
	setupStoreOther: function(profileId, opts) {
		opts = opts || {};
		var me = this,
			vct = WT.createView(me.ID, 'view.OtherWiz', {
				viewCfg: {
					profileId: profileId
				}
			});
		
		vct.getView().on('viewclose', function(s) {
			Ext.callback(opts.callback, opts.scope || me, [true, s.getVMData()]);
		});
		vct.show();
	},
	
	showSharingLinks: function(opts) {
		opts = opts || {};
		var me = this,
			vct = WT.createView(me.ID, 'view.SharingLinks');
		
		vct.getView().on('viewclose', function(s) {
			Ext.callback(opts.callback, opts.scope || me, [true]);
		});
		if(opts.listeners) vct.getView().on(opts.listeners);
		vct.show();
	},
	
	initCxm: function() {
		var me = this;
		me.addRef('cxmRootStore', Ext.create({
			xtype: 'menu',
			items: [
				{
					text: me.res('act-addStore.lbl'),
					iconCls: me.cssIconCls('addStore', 'xs'),
					menu: [
						me.getAction('addStoreFtp'),
						me.getAction('addStoreDropbox'),
						me.getAction('addStoreGooDrive'),
						me.getAction('addStoreFile'),
						me.getAction('addStoreOther')
					]
				},
				'-',
				me.getAction('editSharing')
			],
			listeners: {
				beforeshow: function(s) {
					me.updateDisabled('editSharing');
					me.updateDisabled('addStoreFtp');
					me.updateDisabled('addStoreDropbox');
					me.updateDisabled('addStoreGooDrive');
					me.updateDisabled('addStoreFile'),
					me.updateDisabled('addStoreOther');
				}
			}
		}));
		me.addRef('cxmStore', Ext.create({
			xtype: 'menu',
			items: [
				me.getAction('editStore'),
				me.getAction('deleteStore'),
				{
					text: me.res('act-addStore.lbl'),
					iconCls: me.cssIconCls('addStore', 'xs'),
					menu: [
						me.getAction('addStoreFtp'),
						me.getAction('addStoreDropbox'),
						me.getAction('addStoreGooDrive'),
						me.getAction('addStoreFile'),
						me.getAction('addStoreOther')
					]
				},
				'-',
				me.getAction('editSharing'),
				'-',
				me.getAction('createFileNode')
			],
			listeners: {
				beforeshow: function(s) {
					me.updateDisabled('editSharing');
					me.updateDisabled('addStoreFtp');
					me.updateDisabled('addStoreDropbox');
					me.updateDisabled('addStoreGooDrive');
					me.updateDisabled('addStoreFile'),
					me.updateDisabled('addStoreOther');
					me.updateDisabled('editStore');
					me.updateDisabled('deleteStore');
					me.updateDisabled('createFileNode');
				}
			}
		}));
		me.addRef('cxmFile', Ext.create({
			xtype: 'menu',
			items: [
				me.getAction('renameFileNode'),
				me.getAction('deleteFileNode'),
				'-',
				me.getAction('createFileNode'),
				'-',
				me.getAction('addFileNodeDlLink'),
				me.getAction('deleteFileNodeDlLink'),
				me.getAction('sendFileNodeDlLink'),
				'-',
				me.getAction('addFileNodeUlLink'),
				me.getAction('deleteFileNodeUlLink'),
				me.getAction('sendFileNodeUlLink')
			],
			listeners: {
				beforeshow: function(s) {
					me.updateDisabled('renameFileNode');
					me.updateDisabled('deleteFileNode');
					me.updateDisabled('createFileNode');
					me.updateDisabled('addFileNodeDlLink');
					me.updateDisabled('deleteFileNodeDlLink');
					me.updateDisabled('sendFileNodeDlLink');
					me.updateDisabled('addFileNodeUlLink');
					me.updateDisabled('deleteFileNodeUlLink');
					me.updateDisabled('sendFileNodeUlLink');
				}
			}
		}));
		me.addRef('cxmGridFile0', Ext.create({
			xtype: 'menu',
			items: [
				me.getAction('createFileNode')
			],
			listeners: {
				beforeshow: function(s) {
					me.updateDisabled('createFileNode');
				}
			}
		}));
		me.addRef('cxmGridFile', Ext.create({
			xtype: 'menu',
			items: [
				me.getAction('openFile'),
				me.getAction('downloadFile'),
				'-',
				me.getAction('renameFile'),
				me.getAction('deleteFile'),
				'-',
				me.getAction('addFileDlLink'),
				me.getAction('deleteFileDlLink'),
				me.getAction('sendFileDlLink'),
				'-',
				me.getAction('addFileUlLink'),
				me.getAction('deleteFileUlLink'),
				me.getAction('sendFileUlLink')
			],
			listeners: {
				beforeshow: function(s) {
					me.updateDisabled('openFile');
					me.updateDisabled('downloadFile');
					me.updateDisabled('renameFile');
					me.updateDisabled('addFileDlLink');
					me.updateDisabled('deleteFileDlLink');
					me.updateDisabled('sendFileDlLink');
					me.updateDisabled('addFileUlLink');
					me.updateDisabled('deleteFileUlLink');
					me.updateDisabled('sendFileUlLink');
				}
			}
		}));
	},
	
	onActivate: function() {
		/*
		var me = this,
				gp = me.gpTasks();
		
		if(me.needsReload) {
			me.needsReload = false;
			me.reloadTasks();
		}
		
		me.updateDisabled('showTask');
		me.updateDisabled('printTask');
		me.updateDisabled('copyTask');
		me.updateDisabled('moveTask');
		me.updateDisabled('deleteTask');
		*/
	},
	
	reloadFiles: function() {
		this.reloadGridFiles();
		this.reloadTreeFile();
	},
	
	reloadGridFilesIf: function(fileId) {
		var me = this;
		if(me.curFile === fileId) me.reloadGridFiles();
	},
	
	reloadGridFiles: function() {
		var me = this, tr, node;
		if(me.isActive()) {
			tr = me.trStores();
			node = tr.getStore().getNodeById(me.curFile);
			if(node) {
				me.bcFiles().setSelection(node);
			}
			WTU.loadWithExtraParams(me.gpFiles().getStore(), {fileId: me.curFile});
		} else {
			me.needsReload = true;
		}
	},
	
	reloadTreeFile: function() {
		var me = this, sto, node;
		if(me.isActive()) {
			sto = me.trStores().getStore();
			node = sto.getNodeById(me.curFile);
			if(node) sto.load({node: node});
		} else {
			me.needsReload = true;
		}
	},
	
	
	
	loadTreeFileNode: function(node) {
		var me = this, sto, no;
		if(me.isActive()) {
			sto = me.trStores().getStore();
			if(node && node.isNode) {
				no = node;
			} else {
				no = sto.getNodeById(node || me.curFile);
			}
			if(no) sto.load({node: no});
		} else {
			me.needsReload = true;
		}
	},
	
	loadTreeRootNode: function(pid) {
		var me = this,
				sto = me.trStores().getStore(),
				node;
		
		node = sto.findNode('_pid', pid, false);
		if(node) sto.load({node: node});
	},
	
	
	/*
	reloadFiles: function(fileId) {
		var me = this, sto, pars = {};
		
		if(me.isActive()) {
			sto = me.gpFiles().getStore();
			if(fileId !== undefined) {
				pars = sto.getProxy().getExtraParams();
				if(pars['fileId'] !== fileId) {
					
					WTU.loadWithExtraParams(sto, {fileId: fileId});
				} else {
					sto.load();
				}
			}
			
			//if(fileId !== undefined) Ext.apply(pars, {fileId: fileId});
			//WTU.loadWithExtraParams(sto, pars);
		} else {
			me.needsReload = true;
		}
	},
	*/
	
	getSelectedFile: function(forceSingle) {
		if(forceSingle === undefined) forceSingle = true;
		var sel = this.getSelectedFiles();
		if(forceSingle && sel.length !== 1) return null;
		return (sel.length > 0) ? sel[0] : null;
	},
	
	getSelectedFiles: function() {
		return this.gpFiles().getSelection();
	},
	
	selectionIds: function(sel) {
		var ids = [];
		Ext.iterate(sel, function(rec) {
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
		me.setActionDisabled(action, dis);
	},
	
	/**
	 * @private
	 */
	isDisabled: function(action) {
		var me = this, sel;
		switch(action) {
			case 'editStore':
				sel = me.getCurrentFileNode();
				if(sel) {
					if (sel.get('_builtIn') > 0) return true;
					return !sel.getFPerms().UPDATE;
				} else {
					return true;
				}
			case 'deleteStore':
				sel = me.getCurrentFileNode();
				if (sel) {
					if (sel.get('_builtIn') > 0) return true;
					return !sel.getFPerms().DELETE;
				} else {
					return true;
				}
			case 'addStoreFtp':
				if (!me.isPermitted('STORE_OTHER', 'CREATE')) return true;
				sel = me.getCurrentFileNode();
				if (sel) {
					return !sel.getRPerms().MANAGE;
				} else {
					return true;
				}
			case 'addStoreDropbox':
				if (!me.isPermitted('STORE_CLOUD', 'CREATE')) return true;
				sel = me.getCurrentFileNode();
				if (sel) {
					return !sel.getRPerms().MANAGE;
				} else {
					return true;
				}
			case 'addStoreGooDrive':
				if (!me.isPermitted('STORE_CLOUD', 'CREATE')) return true;
				sel = me.getCurrentFileNode();
				if (sel) {
					return !sel.getRPerms().MANAGE;
				} else {
					return true;
				}
			case 'addStoreFile':
				if (!me.isPermitted('STORE_FILE', 'CREATE')) return true;
				sel = me.getCurrentFileNode();
				if (sel) {
					return !sel.getRPerms().MANAGE;
				} else {
					return true;
				}
			case 'addStoreOther':
				if (!me.isPermitted('STORE_OTHER', 'CREATE')) return true;
				sel = me.getCurrentFileNode();
				if (sel) {
					return !sel.getRPerms().MANAGE;
				} else {
					return true;
				}
			case 'editSharing':
				sel = me.getCurrentFileNode();
				if (sel) {
					if ((sel.get('_type') === 'folder') && (sel.get('_scheme') === 'googledrive')) return true;
					return !sel.getRPerms().MANAGE;
				} else {
					return true;
				}
			case 'createFileNode':
				sel = me.getCurrentFileNode();
				if (sel) {
					return !sel.getEPerms().CREATE;
				} else {
					return true;
				}
			case 'renameFileNode':
				sel = me.getCurrentFileNode();
				if (sel) {
					return !sel.getEPerms().UPDATE;
				} else {
					return true;
				}
			case 'deleteFileNode':
				sel = me.getCurrentFileNode();
				if (sel) {
					return !sel.getEPerms().DELETE;
				} else {
					return true;
				}
			case 'addFileNodeDlLink':
				sel = me.getCurrentFileNode();
				if (sel) {
					return !Ext.isEmpty(sel.getFDlLink());
				} else {
					return true;
				}
			case 'deleteFileNodeDlLink':
			case 'sendFileNodeDlLink':
				sel = me.getCurrentFileNode();
				if (sel) {
					return Ext.isEmpty(sel.getFDlLink());
				} else {
					return true;
				}
			case 'addFileNodeUlLink':
				sel = me.getCurrentFileNode();
				if (sel) {
					return !Ext.isEmpty(sel.getFUlLink());
				} else {
					return true;
				}
			case 'deleteFileNodeUlLink':
			case 'sendFileNodeUlLink':
				sel = me.getCurrentFileNode();
				if (sel) {
					return Ext.isEmpty(sel.getFUlLink());
				} else {
					return true;
				}
			case 'openFile':
				sel = me.getSelectedFiles();
				if (sel.length === 1) {
					return (sel[0].getFType() === 'folder') ? true : false;
				} else {
					return true;
				}
			case 'downloadFile':
				sel = me.getSelectedFiles();
				if (sel.length === 1) {
					return (sel[0].getFType() === 'folder') ? true : false;
				} else {
					return true;
				}
			case 'createFile':
				sel = me.getSelectedFiles();
				if (sel.length === 1) {
					return (sel[0].getFType() !== 'folder') ? true : !sel[0].getEPerms().CREATE;
				} else {
					return true;
				}
			case 'renameFile':
				sel = me.getSelectedFiles();
				if (sel.length === 1) {
					return !sel[0].getEPerms().UPDATE;
				} else {
					return true;
				}
			case 'addFileDlLink':
				sel = me.getSelectedFiles();
				if (sel.length === 1) {
					return !Ext.isEmpty(sel[0].getFDlLink());
				} else {
					return true;
				}
			case 'deleteFileDlLink':
			case 'sendFileDlLink':
				sel = me.getSelectedFiles();
				if (sel.length === 1) {
					return Ext.isEmpty(sel[0].getFDlLink());
				} else {
					return true;
				}
			case 'addFileUlLink':
				sel = me.getSelectedFiles();
				if (sel.length === 1) {
					if (!sel[0].getEPerms().CREATE) return true;
					return (sel[0].getFType() !== 'folder') ? true : !Ext.isEmpty(sel[0].getFUlLink());
				} else {
					return true;
				}
			case 'deleteFileUlLink':
			case 'sendFileUlLink':
				sel = me.getSelectedFiles();
				if (sel.length === 1) {
					if (!sel[0].getEPerms().CREATE) return true;
					return Ext.isEmpty(sel[0].getFUlLink());
				} else {
					return true;
				}
		}
	}
});
