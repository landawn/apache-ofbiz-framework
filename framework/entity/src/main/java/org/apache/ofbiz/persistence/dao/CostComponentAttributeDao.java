package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CostComponentAttributeEntity;

public interface CostComponentAttributeDao extends CrudDao<CostComponentAttributeEntity, CostComponentAttributeEntity, SqlBuilder.PSC, CostComponentAttributeDao> {
}
