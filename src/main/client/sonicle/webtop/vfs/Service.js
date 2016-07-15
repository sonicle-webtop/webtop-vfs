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
	extend: 'WT.sdk.Service',
	requires: [
		'Sonicle.Bytes',
		'Sonicle.grid.column.Icon',
		'Sonicle.grid.column.Bytes',
		'Sonicle.grid.column.Link',
		'Sonicle.toolbar.Breadcrumb',
		'Sonicle.upload.Button',
		'WT.ux.data.EmptyModel',
		'WT.ux.data.SimpleModel',
		'Sonicle.webtop.vfs.model.StoreNode',
		'Sonicle.webtop.vfs.model.GridFile'
	],
	mixins: [
		//'WT.mixin.FoldersTree'
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
		var me = this, tr, node;
		console.log('current file: '+fileId);
		
		if(fileId !== me.curFile) {
			me.curFile = fileId;
			if(fileId) {
				tr = me.trStores();
				node = tr.getStore().getNodeById(fileId);
				if(node) {
					tr.getSelectionModel().select(node);
					me.bcFiles().setSelection(node);
					me.reloadGridFiles();
					node.expand();
				}
			}
			me.btnUpload().uploader.mergeExtraParams({fileId: fileId});
		}
	},
	
	/**
	 * Allow breadcrumb to update its internal splitbutton's menu
	 * @param {Ext.data.NodeInterface} node
	 */
	resyncFileBreadcrumb: function(node) {
		var me = this;
		if(me.curFile === node.getId()) {
			me.bcFiles().setSelection(node);
		}
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
				'-',
				{
					xtype: 'button',
					text: 'Ftp',
					handler: function() {
						me.setupStoreFtp();
					}
				},
				{
					xtype: 'button',
					text: 'Dropbox',
					handler: function() {
						me.setupStoreDropbox();
					}
				},
				{
					xtype: 'button',
					text: 'GoogleDrive',
					handler: function() {
						me.setupStoreGoogleDrive();
					}
				}, {
					xtype: 'souploadbutton',
					reference: 'btnupload',
					uploaderConfig: WTF.uploader(me.ID, 'UploadStoreFile', {
						maxFileSize: me.getOption('privateUploadMaxFileSize'),
						extraParams: {
							fileId: null
						},
						listeners: {
							invalidfilesize: function() {
								WT.warn(WT.res(WT.ID, 'error.upload.sizeexceeded', Sonicle.Bytes.format(me.getOption('privateUploadMaxFileSize'))));
							}
						}
					})
				},
				//me.getAction('deleteTask2'),
				'->'
				/*
				me.addRef('txtsearch', Ext.create({
					xtype: 'textfield',
					width: 200,
					triggers: {
						search: {
							cls: Ext.baseCSSPrefix + 'form-search-trigger',
							handler: function(s) {
								me.queryTasks(s.getValue());
							}
						}
					},
					listeners: {
						specialkey: function(s, e) {
							if(e.getKey() === e.ENTER) me.queryTasks(s.getValue());
						}
					}
				}))
				*/
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
					},
					listeners: {
						write: function(s,op) {
							//me.reloadTasks();
						}
					}
				},
				hideHeaders: true,
				listeners: {
					selectionchange: function(s, rec) {
						console.log('selectionchange');
						
						var type = rec[0].get('_type');
						if(type === 'folder') {
							me.setCurNode(rec[0].getId());
						} else if(type === 'file') {
							me.setCurNode(rec[0].getId());
						} else {
							me.setCurNode(null);
						}
					},
					itemclick: function(s, rec, itm, i, e) {
						console.log('itemclick');
						
						var type = rec.get('_type');
						if(type === 'folder') {
							me.setCurFile(rec.getId());
						} else if(type === 'file') {
							me.setCurFile(rec.getId());
						}
					},
					itemcontextmenu: function(s, rec, itm, i, e) {
						console.log('itemcontextmenu');
						
						var type = rec.get('_type');
						if(type === 'root') {
							//WT.showContextMenu(e, me.getRef('cxmRoot'), {node: rec});
						} else if(type === 'folder') {
							me.setCurNode(rec.get('id'));
							//WT.showContextMenu(e, me.getRef('cxmStore'), {node: rec});
						} else if(type === 'file') {
							me.setCurNode(rec.get('id'));
							WT.showContextMenu(e, me.getRef('cxmFile'), {node: rec});
						} else {
							me.setCurNode(null);
						}
					},
					afteritemexpand: function(rec) {
						console.log('afteritemexpand');
						
						me.resyncFileBreadcrumb(rec);
					}
				}
			}]
		}));
		
		var sto = me.trStores().getStore();
		
		me.setMainComponent(Ext.create({
			xtype: 'container',
			layout: 'border',
			referenceHolder: true,
			viewModel: {
				formulas: {
					/*selectedTask: {
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
					*/
				}
			},
			items: [{
				region: 'center',
				xtype: 'grid',
				reference: 'gpfiles',
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
				/*
				viewConfig: {
					getRowClass: function (rec, indx) {
						if (rec.get('status') === 'completed')
							return 'wttasks-row-completed';
						if (Ext.isDate(rec.get('dueDate')) && Sonicle.Date.compare(rec.get('dueDate'),new Date(),false)>0 )
							return 'wttasks-row-expired';
						return '';
					}
				},
				*/
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
					dataIndex: 'dLink',
					header: WTF.headerWithGlyphIcon('fa fa-cloud-download'),
					getIconCls: function(v) {
						return Ext.isEmpty(v) ? '' : 'wtvfs-icon-downloadLink-xs';
					},
					iconSize: WTU.imgSizeToPx('xs'),
					width: 40
				}, {
					xtype: 'soiconcolumn',
					dataIndex: 'uLink',
					header: WTF.headerWithGlyphIcon('fa fa-cloud-upload'),
					getIconCls: function(v) {
						return Ext.isEmpty(v) ? '' : 'wtvfs-icon-uploadLink-xs';
					},
					iconSize: WTU.imgSizeToPx('xs'),
					width: 40
				}],
				tbar: [{
					xtype: 'sobreadcrumb',
					reference: 'bcfiles',
					store: sto,
					minDepth: 2,
					//useSplitButtons: false,
					height: 30,
					listeners: {
						selectionchange: function(s, node) {
							console.log('breadselectionchange');
							
							me.setCurFile(node.getId());
						}
					}
				}],
				listeners: {
					selectionchange: function() {
						me.updateCxmGridFile();
					},
					rowdblclick: function(s, rec) {
						//var er = me.toRightsObj(rec.get('_erights'));
						//me.showTask(er.UPDATE, rec.get('taskId'));
					},
					rowcontextmenu: function(s, rec, itm, i, e) {
						s.getSelectionModel().select(rec);
						WT.showContextMenu(e, me.getRef('cxmGridFile'), {
							file: rec,
							files: s.getSelection()
						});
					}
				}
			}, {
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
			}]
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
	
	btnUpload: function() {
		return this.getToolbar().lookupReference('btnupload');
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
	
	initActions: function() {
		var me = this;
		/*
		me.addAction('new', 'newTask', {
			handler: function() {
				me.getAction('addTask').execute();
			}
		});
		me.addAction('editSharing', {
			text: WT.res('sharing.tit'),
			iconCls: WTF.cssIconCls(WT.XID, 'sharing', 'xs'),
			handler: function() {
				var node = me.getSelectedNode(me.getRef('folderstree'));
				if(node) me.editShare(node.getId());
			}
		});
		me.addAction('addCategory', {
			handler: function() {
				var node = me.getSelectedFolder(me.getRef('folderstree'));
				if(node) me.addCategory(node.get('_domainId'), node.get('_userId'));
			}
		});
		me.addAction('editCategory', {
			handler: function() {
				var node = me.getSelectedFolder(me.getRef('folderstree'));
				if(node) me.editCategory(node.get('_catId'));
			}
		});
		me.addAction('deleteCategory', {
			handler: function() {
				var node = me.getSelectedFolder(me.getRef('folderstree'));
				if(node) me.confirmDeleteCategory(node);
			}
		});
		me.addAction('viewAllFolders', {
			iconCls: 'wt-icon-select-all-xs',
			handler: function() {
				me.showHideAllFolders(me.getSelectedRootFolder(me.getRef('folderstree')), true);
			}
		});
		me.addAction('viewNoneFolders', {
			iconCls: 'wt-icon-select-none-xs',
			handler: function() {
				me.showHideAllFolders(me.getSelectedRootFolder(me.getRef('folderstree')), false);
			}
		});
		me.addAction('showTask', {
			text: WT.res('act-open.lbl'),
			handler: function() {
				var rec = me.getSelectedTask(), er;
				if(rec) {
					er = me.toRightsObj(rec.get('_erights'));
					me.showTask(er.UPDATE, rec.get('taskId'));
				}
			}
		});
		me.addAction('addTask', {
			handler: function() {
				var node = me.getSelectedFolder(me.getRef('folderstree'));
				if(node) me.addTask(node.get('_pid'), node.get('_catId'));
			}
		});
		
		me.addAction('copyTask', {
			handler: function() {
				me.moveTasksSel(true, me.getSelectedTasks());
			}
		});
		me.addAction('moveTask', {
			handler: function() {
				me.moveTasksSel(false, me.getSelectedTasks());
			}
		});
		me.addAction('printTask', {
			text: WT.res('act-print.lbl'),
			iconCls: 'wt-icon-print-xs',
			handler: function() {
				var sel = me.getSelectedTasks();
				if(sel.length > 0) me.printSelTasks(sel);
			}
		});
		me.addAction('print', {
			text: null,
			tooltip: WT.res('act-print.lbl'),
			iconCls: 'wt-icon-print-xs',
			handler: function() {
				me.getAction('printTask').execute();
			}
		});
		me.addAction('deleteTask2', {
			text: null,
			tooltip: WT.res('act-delete.tip'),
			iconCls: 'wt-icon-delete-xs',
			handler: function() {
				me.getAction('deleteTask').execute();
			}
		});
		me.addAction('addTask2', {
			text: null,
			tooltip: me.res('act-addTask.lbl'),
			iconCls: me.cssIconCls('addTask', 'xs'),
			handler: function() {
				me.getAction('addTask').execute();
			}
		});
		*/
		
		me.addAction('openFile', {
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.openSelFile(sel);
			}
		});
		me.addAction('downloadFile', {
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.downloadSelFile(sel);
			}
		});
		me.addAction('renameFile', {
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.renameSelFile(sel);
			}
		});
		me.addAction('deleteFile', {
			handler: function() {
				var sel = me.getSelectedFiles();
				if(sel.length > 0) me.deleteSelFiles(sel);
			}
		});
		me.addAction('addFileDlLink', {
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.addSelFileLink('d', sel);
			}
		});
		me.addAction('deleteFileDlLink', {
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.deleteSelFileLink('d', sel);
			}
		});
		me.addAction('addFileUlLink', {
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.addSelFileLink('u', sel);
			}
		});
		me.addAction('deleteFileUlLink', {
			handler: function() {
				var sel = me.getSelectedFile();
				if(sel) me.deleteSelFileLink('u', sel);
			}
		});
		
		
		
		
		
		me.addAction('addFileFolder', {
			handler: function() {
				var sel = me.getCurrentFileNode();
				if(sel) me.add(sel);
			}
		});
		me.addAction('renameFileNode', {
			text: me.res('act-renameFile.lbl'),
			iconCls: me.cssIconCls('renameFile', 'xs'),
			handler: function() {
				var sel = me.getCurrentFileNode();
				if(sel) me.renameSelFile([sel]);
			}
		});
		me.addAction('deleteFileNode', {
			text: me.res('act-deleteFile.lbl'),
			iconCls: me.cssIconCls('deleteFile', 'xs'),
			handler: function() {
				var sel = me.getCurrentFileNode();
				if(sel) me.deleteSelFiles([sel]);
			}
		});
		me.addAction('addFileNodeDlLink', {
			text: me.res('act-addFileDlLink.lbl'),
			iconCls: me.cssIconCls('addFileDlLink', 'xs'),
			handler: function() {
				var sel = me.getCurrentFileNode();
				if(sel) me.addSelFileLink('d', sel);
			}
		});
		me.addAction('deleteFileNodeDlLink', {
			text: me.res('act-deleteFileDlLink.lbl'),
			iconCls: me.cssIconCls('deleteFileDlLink', 'xs'),
			handler: function() {
				var sel = me.getCurrentFileNode();
				if(sel) me.deleteSelFileLink('d', sel);
			}
		});
		me.addAction('addFileNodeUlLink', {
			text: me.res('act-addFileUlLink.lbl'),
			iconCls: me.cssIconCls('addFileUlLink', 'xs'),
			handler: function() {
				var sel = me.getCurrentFileNode();
				if(sel) me.addSelFileLink('u', sel);
			}
		});
		me.addAction('deleteFileNodeUlLink', {
			text: me.res('act-deleteFileUlLink.lbl'),
			iconCls: me.cssIconCls('deleteFileUlLink', 'xs'),
			handler: function() {
				var sel = me.getCurrentFileNode();
				if(sel) me.deleteSelFileLink('u', sel);
			}
		});
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
	
	updateCxmFile: function() {
		var me = this;
		me.updateDisabled('addFileNodeDlLink');
		me.updateDisabled('deleteFileNodeDlLink');
		//me.updateDisabled('sendFolderDlLink');
		me.updateDisabled('addFileNodeUlLink');
		me.updateDisabled('deleteFileNodeUlLink');
		//me.updateDisabled('sendFolderUlLink');
	},
	
	openSelFile: function(sel) {
		
	},
	
	downloadSelFile: function(sel) {
		this.downloadFile(sel.get('fileId'));
	},
	
	renameSelFile: function(sel) {
		var me = this;
		WT.prompt(me.res('gpfiles.name.lbl'), {
			title: me.res('act-renameFile.lbl'),
			value: sel.get('name'),
			fn: function(bid, name) {
				if(bid === 'ok') {
					if(name !== sel.get('name')) {
						me.renameFile(sel.get('fileId'), name, {
							callback: function(success) {
								if(success) me.reloadFiles();
							}
						});
					}
				}
			}
		});
	},
	
	deleteSelFiles: function(sel) {
		var me = this,
			sto = me.gpFiles().getStore(),
			ids = me.selectionIds(sel),
			msg;
		
		if(sel.length === 1) {
			msg = me.res('gpfiles.confirm.'+sel[0].get('type')+'.delete', Ext.String.ellipsis(sel[0].get('name'), 40));
		} else {
			msg = me.res('gpfiles.confirm.delete.selection');
		}
		WT.confirm(msg, function(bid) {
			if(bid === 'yes') {
				me.deleteFiles(ids, {
					callback: function(success) {
						if(success) sto.remove(sel);
					}
				});
			}
		});
	},
	
	addSelFileLink: function(type, sel) {
		var me = this,
				pre = sel.isNode ? '_' : '';
		me.setupLink(type, sel.get(pre+'fileId'), {
			callback: function(success) {
				if(success) me.reloadFiles();
			}
		});
	},
	
	deleteSelFileLink: function(type, sel) {
		var me = this,
				pre = sel.isNode ? '_' : '',
				field = pre+type+'Link',
				fileId = sel.get(field);
		WT.confirm(me.res('link.confirm.delete'), function(bid) {
			if(bid === 'yes') {
				me.deleteLink(type, fileId, {
					callback: function(success) {
						if(success) {
							sel.set(field, null);
							me.updateCxmFile();
							me.updateCxmGridFile();
						}
					}
				});
			}
		});
	},
	
	sendSelLink: function(type, sel) {
		
	},
	
	openfile: function(fileId) {
		
	},
	
	downloadFile: function(fileId) {
		Sonicle.URLMgr.download(WTF.processBinUrl(this.ID, 'DownloadFiles', {
			fileId: fileId
		}));
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
		var me = this, vwc;
		
		if(type === 'd') {
			vwc = me.wizardDownloadLink(fileId);
		} else if(type === 'u') {
			vwc = me.wizardUploadLink(fileId);
		}
		vwc.getView().on('viewclose', function(s) {
			var result = s.getVM().get('result');
			Ext.callback(opts.callback, opts.scope || me, [result !== null, result]);
		});
		vwc.show();
	},
	
	deleteLink: function(type, linkId, opts) {
		opts = opts || {};
		var me = this, act;
		
		if(type === 'd') {
			act = 'ManageDownloadLink';
		} else if(type === 'u') {
			act = 'ManageUploadLink';
		}
		WT.ajaxReq(me.ID, act, {
			params: {
				crud: 'delete',
				id: linkId
			},
			callback: function(success, json) {
				Ext.callback(opts.callback, opts.scope || me, [success, json.data, json]);
			}
		});
	},
	
	initCxm: function() {
		var me = this;
		/*
		me.addRef('cxmRootFolder', Ext.create({
			xtype: 'menu',
			items: [
				me.getAction('addCategory'),
				'-',
				me.getAction('editSharing')
				//TODO: azioni altri servizi?
			],
			listeners: {
				beforeshow: function(s) {
					var rec = s.menuData.folder,
							rr = me.toRightsObj(rec.get('_rrights'));
					me.getAction('addCategory').setDisabled(!rr.MANAGE);
					me.getAction('editSharing').setDisabled(!rr.MANAGE);
				}
			}
		}));
		
		me.addRef('cxmFolder', Ext.create({
			xtype: 'menu',
			items: [
				me.getAction('editCategory'),
				me.getAction('deleteCategory'),
				me.getAction('addCategory'),
				'-',
				me.getAction('editSharing'),
				'-',
				me.getAction('viewAllFolders'),
				me.getAction('viewNoneFolders'),
				'-',
				me.getAction('addTask')
				//TODO: azioni altri servizi?
			],
			listeners: {
				beforeshow: function(s) {
					var rec = s.menuData.folder,
							rr = me.toRightsObj(rec.get('_rrights')),
							fr = me.toRightsObj(rec.get('_frights')),
							er = me.toRightsObj(rec.get('_erights'));
					me.getAction('editCategory').setDisabled(!fr.UPDATE);
					me.getAction('deleteCategory').setDisabled(!fr.DELETE || rec.get('_builtIn'));
					me.getAction('addCategory').setDisabled(!rr.MANAGE);
					me.getAction('editSharing').setDisabled(!rr.MANAGE);
					me.getAction('addTask').setDisabled(!er.CREATE);
				}
			}
		}));
		*/
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
				'-',
				me.getAction('addFileUlLink'),
				me.getAction('deleteFileUlLink')
			],
			listeners: {
				beforeshow: function(s) {
					me.updateCxmGridFile();
				}
			}
		}));
		me.addRef('cxmFile', Ext.create({
			xtype: 'menu',
			items: [
				me.getAction('addFileFolder'),
				'-',
				me.getAction('renameFileNode'),
				me.getAction('deleteFileNode'),
				'-',
				me.getAction('addFileNodeDlLink'),
				me.getAction('deleteFileNodeDlLink'),
				'-',
				me.getAction('addFileNodeUlLink'),
				me.getAction('deleteFileNodeUlLink')
			],
			listeners: {
				beforeshow: function(s) {
					me.updateCxmFile();
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
	
	reloadGridFiles: function() {
		var me = this;
		if(me.isActive()) {
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
	
	setupStoreFtp: function() {
		var me = this,
				vwc = WT.createView(me.ID, 'view.FtpWiz', {
					viewCfg: {
						//categoryId: categoryId
					}
				});
		
		vwc.getView().on('dosuccess', function() {
			//me.reloadContacts();
		});
		vwc.show();
	},
	
	setupStoreDropbox: function() {
		var me = this,
				vwc = WT.createView(me.ID, 'view.DropboxWiz', {
					viewCfg: {
						//categoryId: categoryId
					}
				});
		
		vwc.getView().on('dosuccess', function() {
			//me.reloadContacts();
		});
		vwc.show();
	},
	
	setupStoreGoogleDrive: function() {
		var me = this,
				vwc = WT.createView(me.ID, 'view.GooDriveWiz', {
					viewCfg: {
						//categoryId: categoryId
					}
				});
		
		vwc.getView().on('dosuccess', function() {
			//me.reloadContacts();
		});
		vwc.show();
	},
	
	wizardDownloadLink: function(fileId) {
		var me = this,
			vwc = WT.createView(me.ID, 'view.DownloadLinkWiz', {
				viewCfg: {
					fileId: fileId
				}
			});
		return vwc;
	},
	
	wizardUploadLink: function(fileId) {
		var me = this,
			vwc = WT.createView(me.ID, 'view.UploadLinkWiz', {
				viewCfg: {
					fileId: fileId
				}
			});
		return vwc;
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
			case 'openFile':
				sel = me.getSelectedFiles();
				if(sel.length === 1) {
					return (sel[0].get('type') !== 'folder') ? true : false;
				} else {
					return true;
				}
			case 'downloadFile':
				sel = me.getSelectedFiles();
				if(sel.length === 1) {
					return (sel[0].get('type') !== 'folder') ? true : false;
				} else {
					return true;
				}
			case 'renameFile':
				sel = me.getSelectedFiles();
				if(sel.length === 1) {
					return false;
				} else {
					return true;
				}
			case 'addFileDlLink':
				sel = me.getSelectedFiles();
				if(sel.length === 1) {
					return !Ext.isEmpty(sel[0].get('dLink'));
				} else {
					return true;
				}
			case 'deleteFileDlLink':
				sel = me.getSelectedFiles();
				if(sel.length === 1) {
					return Ext.isEmpty(sel[0].get('dLink'));
				} else {
					return true;
				}
			case 'addFileUlLink':
				sel = me.getSelectedFiles();
				if(sel.length === 1) {
					return (sel[0].get('type') !== 'folder') ? true : !Ext.isEmpty(sel[0].get('uLink'));
				} else {
					return true;
				}
			case 'deleteFileUlLink':
				sel = me.getSelectedFiles();
				if(sel.length === 1) {
					return Ext.isEmpty(sel[0].get('uLink'));
				} else {
					return true;
				}
			case 'addFileNodeDlLink':
				sel = me.getCurrentFileNode();
				if(sel) {
					return !Ext.isEmpty(sel.get('_dLink'));
				} else {
					return true;
				}
			case 'deleteFileNodeDlLink':
				sel = me.getCurrentFileNode();
				if(sel) {
					return Ext.isEmpty(sel.get('_dLink'));
				} else {
					return true;
				}
			case 'addFileNodeUlLink':
				sel = me.getCurrentFileNode();
				if(sel) {
					return !Ext.isEmpty(sel.get('_uLink'));
				} else {
					return true;
				}
			case 'deleteFileNodeUlLink':
				sel = me.getCurrentFileNode();
				if(sel.length === 1) {
					return Ext.isEmpty(sel.get('_uLink'));
				} else {
					return true;
				}	
		}
	}
});
