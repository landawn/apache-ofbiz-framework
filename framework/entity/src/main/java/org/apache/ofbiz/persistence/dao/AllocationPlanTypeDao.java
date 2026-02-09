package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.AllocationPlanTypeEntity;

public interface AllocationPlanTypeDao extends CrudDao<AllocationPlanTypeEntity, String, SQLBuilder.PSC, AllocationPlanTypeDao> {
}
