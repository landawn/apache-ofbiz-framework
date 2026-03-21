package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContactMechAttributeEntity;

public interface ContactMechAttributeDao extends CrudDao<ContactMechAttributeEntity, ContactMechAttributeEntity, SqlBuilder.PSC, ContactMechAttributeDao> {
}
