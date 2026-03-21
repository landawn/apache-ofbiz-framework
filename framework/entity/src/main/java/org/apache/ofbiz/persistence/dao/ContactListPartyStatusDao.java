package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContactListPartyStatusEntity;

public interface ContactListPartyStatusDao extends CrudDao<ContactListPartyStatusEntity, ContactListPartyStatusEntity, SqlBuilder.PSC, ContactListPartyStatusDao>, DelegatorQueryDao {
}
