package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FinAccountTransAttributeEntity;

public interface FinAccountTransAttributeDao extends CrudDao<FinAccountTransAttributeEntity, FinAccountTransAttributeEntity, SqlBuilder.PSC, FinAccountTransAttributeDao> {
}
