package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.AllocationPlanItemEntity;

public interface AllocationPlanItemDao extends CrudDao<AllocationPlanItemEntity, AllocationPlanItemEntity, SQLBuilder.PSC, AllocationPlanItemDao> {
}
