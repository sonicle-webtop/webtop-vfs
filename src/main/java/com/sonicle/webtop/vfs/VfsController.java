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
package com.sonicle.webtop.vfs;

import com.sonicle.webtop.core.CoreManager;
import com.sonicle.webtop.core.app.WT;
import com.sonicle.webtop.core.app.sdk.interfaces.IControllerServiceHooks;
import com.sonicle.webtop.core.app.sdk.interfaces.IControllerUserEvents;
import com.sonicle.webtop.core.bol.ODomain;
import com.sonicle.webtop.core.sdk.BaseController;
import com.sonicle.webtop.core.sdk.ServiceVersion;
import com.sonicle.webtop.core.sdk.UserProfileId;
import com.sonicle.webtop.core.sdk.WTException;
import org.slf4j.Logger;

/**
 *
 * @author malbinola
 */
public class VfsController extends BaseController implements IControllerServiceHooks, IControllerUserEvents {
	public static final Logger logger = WT.getLogger(VfsController.class);
	
	@Override
	public void initProfile(ServiceVersion current, UserProfileId profileId) throws WTException {
		CoreManager core = WT.getCoreManager(profileId);
		VfsManager manager = new VfsManager(true, profileId);
		
		// Adds built-in store
		try {
			manager.addBuiltInStoreMyDocuments();
			if (profileId.getUserId().equals("admin")) {
				for (ODomain odomain : core.listDomains(false)) {
					manager.addBuiltInStoreDomainImages(odomain.getDomainId());
				}
			}
		} catch(WTException ex) {
			throw ex;
		}
	}
	
	@Override
	public void upgradeProfile(ServiceVersion current, UserProfileId profileId, ServiceVersion profileLastSeen) throws WTException {}
	
	@Override
	public void onUserAdded(UserProfileId profileId) throws WTException {}
	
	@Override
	public void onUserRemoved(UserProfileId profileId) throws WTException {}
}
