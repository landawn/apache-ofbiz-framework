package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FinAccountTransTypeEntity;

public interface FinAccountTransTypeDao extends CrudDao<FinAccountTransTypeEntity, String, SqlBuilder.PSC, FinAccountTransTypeDao> {
}
