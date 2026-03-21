package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContactMechEntity;

public interface ContactMechDao extends CrudDao<ContactMechEntity, String, SqlBuilder.PSC, ContactMechDao>, DelegatorQueryDao {
}
