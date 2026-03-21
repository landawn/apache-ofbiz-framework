package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CostComponentTypeAttrEntity;

public interface CostComponentTypeAttrDao extends CrudDao<CostComponentTypeAttrEntity, CostComponentTypeAttrEntity, SqlBuilder.PSC, CostComponentTypeAttrDao> {
}
