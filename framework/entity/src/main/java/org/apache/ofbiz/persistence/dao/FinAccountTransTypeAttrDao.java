package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FinAccountTransTypeAttrEntity;

public interface FinAccountTransTypeAttrDao extends CrudDao<FinAccountTransTypeAttrEntity, FinAccountTransTypeAttrEntity, SQLBuilder.PSC, FinAccountTransTypeAttrDao> {
}
