package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ContactListTypeEntity;

public interface ContactListTypeDao extends CrudDao<ContactListTypeEntity, String, SQLBuilder.PSC, ContactListTypeDao> {
}
