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
package com.sonicle.webtop.vfs.bol.model;

import com.google.gson.annotations.SerializedName;
import com.sonicle.commons.EnumUtils;
import com.sonicle.commons.web.json.CId;
import com.sonicle.webtop.core.sdk.UserProfileId;
import net.sf.qualitycheck.Check;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author malbinola
 */
public class StoreNodeId extends CId {
	
	// Node IDs are designed to be self-explanatory; obtained an ID you can 
	// extract information about the item referring to. This tree will only 
	// render two-level hierarchy.
	// IDs have the following format: {type}'|'{origin}['|'{folder}]
	//                                | depth 0 + 1    | depth 1   |
	//   type:
	//     O -> Origin tree-node (depth 0)
	//     F -> Folder tree-node (depth 1)
	//     G -> Grouper tree-node (depth 0)
	//   origin:
	//     profile-id  -> The real profile ID referring to the owning origin
	//   folder:
	//     Optional, the folder ID pointing at (only valid in level 1)
	
	protected StoreNodeId(CId.AbstractBuilder builder) {
		super(builder);
	}
	
	public StoreNodeId(String nodeId) {
		super(nodeId, 4);
	}
	
	public Type getType() {
		return Check.notNull(EnumUtils.forSerializedName(getTokenOrNull(0), Type.class), "token[0]");
	}
	
	public String getOrigin() {
		return getTokenOrNull(1);
	}
	
	public UserProfileId getOriginAsProfileId() {
		return new UserProfileId(getOrigin());
	}
	
	public Integer getFolderId() {
		final String folderId = getTokenOrNull(2);
		return !StringUtils.isBlank(folderId) ? Integer.valueOf(folderId) : null;
	}
	
	public String getFilePath() {
		return getTokenOrNull(3);
	}
	
	public static StoreNodeId build(final Type type, final String origin) {
		return new Builder()
			.withTokens(EnumUtils.toSerializedName(Check.notNull(type, "type")), Check.notNull(origin, "origin"))
			.build();
	}
	
	public static StoreNodeId build(final Type type, final UserProfileId originProfileId) {
		return build(Check.notNull(type, "type"), Check.notNull(originProfileId, "originProfileId").toString());
	}
	
	public static StoreNodeId build(final Type type, final UserProfileId originProfileId, final int folderId) {
		return new Builder()
			.withTokens(EnumUtils.toSerializedName(Check.notNull(type, "type")), Check.notNull(originProfileId, "originProfileId").toString(), folderId)
			.build();
	}
	
	public static StoreNodeId build(final Type type, final UserProfileId originProfileId, final int folderId, final String filePath) {
		return new Builder()
			.withTokens(EnumUtils.toSerializedName(Check.notNull(type, "type")), Check.notNull(originProfileId, "originProfileId").toString(), Check.notNull(folderId, "folderId"), filePath)
			.build();
	}
	
	public static enum Type {
		@SerializedName("O") ORIGIN,
		@SerializedName("F") FOLDER,
		@SerializedName("G") GROUPER,
		@SerializedName("L") FILEOBJECT;
	}
	
	private static class Builder extends CId.AbstractBuilder<Builder, StoreNodeId> {
		@Override
		public StoreNodeId build() {
			return new StoreNodeId(this);
		}
	}
}
