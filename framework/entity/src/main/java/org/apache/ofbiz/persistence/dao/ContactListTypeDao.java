package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContactListTypeEntity;

public interface ContactListTypeDao extends CrudDao<ContactListTypeEntity, String, SqlBuilder.PSC, ContactListTypeDao> {
}
