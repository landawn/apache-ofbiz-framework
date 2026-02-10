package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ContactListCommStatusEntity;

public interface ContactListCommStatusDao extends CrudDao<ContactListCommStatusEntity, ContactListCommStatusEntity, SQLBuilder.PSC, ContactListCommStatusDao>, DelegatorQueryDao {
}
