package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ContactMechEntity;

public interface ContactMechDao extends CrudDao<ContactMechEntity, String, SQLBuilder.PSC, ContactMechDao>, DelegatorQueryDao {
}
