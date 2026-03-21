package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FinAccountTypeAttrEntity;

public interface FinAccountTypeAttrDao extends CrudDao<FinAccountTypeAttrEntity, FinAccountTypeAttrEntity, SqlBuilder.PSC, FinAccountTypeAttrDao> {
}
