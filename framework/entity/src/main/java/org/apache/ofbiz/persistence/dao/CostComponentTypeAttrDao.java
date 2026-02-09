package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CostComponentTypeAttrEntity;

public interface CostComponentTypeAttrDao extends CrudDao<CostComponentTypeAttrEntity, CostComponentTypeAttrEntity, SQLBuilder.PSC, CostComponentTypeAttrDao> {
}
