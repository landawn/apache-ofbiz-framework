package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ContactMechPurposeTypeEntity;

public interface ContactMechPurposeTypeDao extends CrudDao<ContactMechPurposeTypeEntity, String, SqlBuilder.PSC, ContactMechPurposeTypeDao> , DelegatorQueryDao{
}
