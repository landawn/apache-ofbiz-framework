package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CostComponentCalcEntity;

public interface CostComponentCalcDao extends CrudDao<CostComponentCalcEntity, String, SQLBuilder.PSC, CostComponentCalcDao> {
}
