package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ContactMechTypePurposeEntity;

public interface ContactMechTypePurposeDao extends CrudDao<ContactMechTypePurposeEntity, ContactMechTypePurposeEntity, SQLBuilder.PSC, ContactMechTypePurposeDao> {
}
