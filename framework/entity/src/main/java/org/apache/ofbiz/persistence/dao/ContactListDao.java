package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContactListEntity;

public interface ContactListDao extends CrudDao<ContactListEntity, String, SqlBuilder.PSC, ContactListDao>, DelegatorQueryDao {
}
