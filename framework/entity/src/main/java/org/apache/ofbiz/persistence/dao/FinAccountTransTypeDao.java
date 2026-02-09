package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FinAccountTransTypeEntity;

public interface FinAccountTransTypeDao extends CrudDao<FinAccountTransTypeEntity, String, SQLBuilder.PSC, FinAccountTransTypeDao> {
}
