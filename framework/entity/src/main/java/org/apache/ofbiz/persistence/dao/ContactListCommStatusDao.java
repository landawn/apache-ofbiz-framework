package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContactListCommStatusEntity;

public interface ContactListCommStatusDao extends CrudDao<ContactListCommStatusEntity, ContactListCommStatusEntity, SqlBuilder.PSC, ContactListCommStatusDao>, DelegatorQueryDao {
}
