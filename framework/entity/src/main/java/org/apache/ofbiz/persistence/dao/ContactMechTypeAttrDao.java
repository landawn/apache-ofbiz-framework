package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContactMechTypeAttrEntity;

public interface ContactMechTypeAttrDao extends CrudDao<ContactMechTypeAttrEntity, ContactMechTypeAttrEntity, SqlBuilder.PSC, ContactMechTypeAttrDao> {
}
