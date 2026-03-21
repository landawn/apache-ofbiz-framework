package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FinAccountTransTypeAttrEntity;

public interface FinAccountTransTypeAttrDao extends CrudDao<FinAccountTransTypeAttrEntity, FinAccountTransTypeAttrEntity, SqlBuilder.PSC, FinAccountTransTypeAttrDao> {
}
