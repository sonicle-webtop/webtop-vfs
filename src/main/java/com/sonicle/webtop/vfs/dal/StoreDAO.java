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
package com.sonicle.webtop.vfs.dal;

import com.sonicle.webtop.core.bol.Owner;
import com.sonicle.webtop.core.dal.BaseDAO;
import com.sonicle.webtop.core.dal.DAOException;
import com.sonicle.webtop.vfs.bol.OStore;
import static com.sonicle.webtop.vfs.jooq.Sequences.SEQ_STORES;
import static com.sonicle.webtop.vfs.jooq.Tables.STORES;
import com.sonicle.webtop.vfs.jooq.tables.records.StoresRecord;
import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

/**
 *
 * @author malbinola
 */
public class StoreDAO extends BaseDAO {
	private final static StoreDAO INSTANCE = new StoreDAO();
	public static StoreDAO getInstance() {
		return INSTANCE;
	}

	public Long getSequence(Connection con) throws DAOException {
		DSLContext dsl = getDSL(con);
		Long nextID = dsl.nextval(SEQ_STORES);
		return nextID;
	}
	
	public boolean existsById(Connection con, int storeId) throws DAOException {
		DSLContext dsl = getDSL(con);
		return dsl
			.selectCount()
			.from(STORES)
			.where(
				STORES.STORE_ID.equal(storeId)
			)
			.fetchOne(0, Integer.class) == 1;
	}
	
	public Owner selectOwnerById(Connection con, int storeId) throws DAOException {
		DSLContext dsl = getDSL(con);
		return dsl
			.select()
			.from(STORES)
			.where(
				STORES.STORE_ID.equal(storeId)
			)
			.fetchOneInto(Owner.class);
	}
	
	public List<OStore> selectByDomain(Connection con, String domainId) throws DAOException {
		DSLContext dsl = getDSL(con);
		return dsl
			.select()
			.from(STORES)
			.where(
				STORES.DOMAIN_ID.equal(domainId)
			)
			.orderBy(
				STORES.NAME.asc()
			)
			.fetchInto(OStore.class);
	}
	
	public List<OStore> selectByDomainIn(Connection con, String domainId, Collection<Integer> storeIds) throws DAOException {
		DSLContext dsl = getDSL(con);
		return dsl
			.select()
			.from(STORES)
			.where(
				STORES.DOMAIN_ID.equal(domainId)
				.and(STORES.STORE_ID.in(storeIds))
			)
			.orderBy(
				STORES.BUILT_IN.desc(),
				STORES.NAME.asc()
			)
			.fetchInto(OStore.class);
	}
	
	public List<OStore> selectByProfile(Connection con, String domainId, String userId) throws DAOException {
		DSLContext dsl = getDSL(con);
		return dsl
			.select()
			.from(STORES)
			.where(
				STORES.DOMAIN_ID.equal(domainId)
				.and(STORES.USER_ID.equal(userId))
			)
			.orderBy(
				STORES.BUILT_IN.desc(),
				STORES.NAME.asc()
			)
			.fetchInto(OStore.class);
	}
	
	public List<OStore> selectByProfileIn(Connection con, String domainId, String userId, Integer[] storeIds) throws DAOException {
		DSLContext dsl = getDSL(con);
		return dsl
				.select()
				.from(STORES)
				.where(
						STORES.DOMAIN_ID.equal(domainId)
						.and(STORES.USER_ID.equal(userId))
						.and(STORES.STORE_ID.in(storeIds))
				)
				.orderBy(
						STORES.BUILT_IN.desc(),
						STORES.NAME.asc()
				)
				.fetchInto(OStore.class);
	}
	
	
	
	public List<Integer> selectIdByDomainUserBuiltIn(Connection con, String domainId, String userId, short builtIn) throws DAOException {
		DSLContext dsl = getDSL(con);
		return dsl
				.select(
					STORES.STORE_ID
				)
				.from(STORES)
				.where(
						STORES.DOMAIN_ID.equal(domainId)
						.and(STORES.USER_ID.equal(userId))
						.and(STORES.BUILT_IN.equal(builtIn))
				)
				.orderBy(
						STORES.BUILT_IN.desc(),
						STORES.NAME.asc()
				)
				.fetchInto(Integer.class);
	}
	
	public List<OStore> selectByDomainUserBuiltIn(Connection con, String domainId, String userId, short builtIn) throws DAOException {
		DSLContext dsl = getDSL(con);
		return dsl
				.select()
				.from(STORES)
				.where(
						STORES.DOMAIN_ID.equal(domainId)
						.and(STORES.USER_ID.equal(userId))
						.and(STORES.BUILT_IN.equal(builtIn))
				)
				.orderBy(
						STORES.BUILT_IN.desc(),
						STORES.NAME.asc()
				)
				.fetchInto(OStore.class);
	}
	
	public List<OStore> selectByDomainUserBuiltInUri(Connection con, String domainId, String userId, short builtIn, String uri) throws DAOException {
		DSLContext dsl = getDSL(con);
		return dsl
				.select()
				.from(STORES)
				.where(
						STORES.DOMAIN_ID.equal(domainId)
						.and(STORES.USER_ID.equal(userId))
						.and(STORES.BUILT_IN.equal(builtIn))
						.and(STORES.URI.equal(uri))
				)
				.orderBy(
						STORES.BUILT_IN.desc(),
						STORES.NAME.asc()
				)
				.fetchInto(OStore.class);
	}
	
	public Set<Integer> selectIdsByProfile(Connection con, String domainId, String userId) throws DAOException {
		return selectIdsByProfileIn(con, domainId, userId, null);
	}
	
	public Set<Integer> selectIdsByProfileIn(Connection con, String domainId, String userId, Collection<Integer> storeIds) throws DAOException {
		DSLContext dsl = getDSL(con);
		Condition cndtIn = storeIds != null ? STORES.STORE_ID.in(storeIds) : DSL.trueCondition();
		return dsl
			.select(
				STORES.STORE_ID
			)
			.from(STORES)
			.where(
				STORES.DOMAIN_ID.equal(domainId)
				.and(STORES.USER_ID.equal(userId))
				.and(cndtIn)
			)
			.orderBy(
				STORES.BUILT_IN.desc(),
				//STORES.PROVIDER.asc(),
				STORES.NAME.asc()
			)
			.fetchSet(STORES.STORE_ID);
	}
	
	public OStore selectById(Connection con, int storeId) throws DAOException {
		DSLContext dsl = getDSL(con);
		return dsl
			.select()
			.from(STORES)
			.where(
				STORES.STORE_ID.equal(storeId)
			)
			.fetchOneInto(OStore.class);
	}
	
	/*
	public OStore selectBuiltInByDomainUser(Connection con, String domainId, String userId) throws DAOException {
		DSLContext dsl = getDSL(con);
		return dsl
			.select()
			.from(STORES)
			.where(
					STORES.DOMAIN_ID.equal(domainId)
					.and(STORES.USER_ID.equal(userId))
					.and(STORES.BUILT_IN.equal(true))
			)
			.fetchOneInto(OStore.class);
	}
	*/
	
	public int insert(Connection con, OStore item) throws DAOException {
		DSLContext dsl = getDSL(con);
		StoresRecord record = dsl.newRecord(STORES, item);
		return dsl
			.insertInto(STORES)
			.set(record)
			.execute();
	}
	
	public int update(Connection con, OStore item) throws DAOException {
		DSLContext dsl = getDSL(con);
		return dsl
			.update(STORES)
			.set(STORES.NAME, item.getName())
			.set(STORES.URI, item.getUri())
			.set(STORES.PARAMETERS, item.getParameters())
			.where(
				STORES.STORE_ID.equal(item.getStoreId())
			)
			.execute();
	}
	
	public int updateUriParameters(Connection con, int storeId, String uri, String parameters) throws DAOException {
		DSLContext dsl = getDSL(con);
		return dsl
			.update(STORES)
			.set(STORES.URI, uri)
			.set(STORES.PARAMETERS, parameters)
			.where(
				STORES.STORE_ID.equal(storeId)
			)
			.execute();
	}
	
	public int update(Connection con, int storeId, FieldsMap fieldValues) throws DAOException {
		DSLContext dsl = getDSL(con);
		return dsl
			.update(STORES)
			.set(fieldValues)
			.where(
				STORES.STORE_ID.equal(storeId)
			)
			.execute();
	}
	
	public int deleteById(Connection con, int storeId) throws DAOException {
		DSLContext dsl = getDSL(con);
		return dsl
				.delete(STORES)
				.where(STORES.STORE_ID.equal(storeId))
				.execute();
	}
	
	public int deleteByDomain(Connection con, String domainId) throws DAOException {
		DSLContext dsl = getDSL(con);
		return dsl
				.delete(STORES)
				.where(STORES.DOMAIN_ID.equal(domainId))
				.execute();
	}
	
	public int deleteByDomainUser(Connection con, String domainId, String userId) throws DAOException {
		DSLContext dsl = getDSL(con);
		return dsl
				.delete(STORES)
				.where(
						STORES.DOMAIN_ID.equal(domainId)
						.and(STORES.USER_ID.equal(userId))
				)
				.execute();
	}
}
