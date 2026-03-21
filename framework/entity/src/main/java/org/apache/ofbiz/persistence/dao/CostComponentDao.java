package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CostComponentEntity;

public interface CostComponentDao extends CrudDao<CostComponentEntity, String, SqlBuilder.PSC, CostComponentDao> , DelegatorQueryDao{
}
