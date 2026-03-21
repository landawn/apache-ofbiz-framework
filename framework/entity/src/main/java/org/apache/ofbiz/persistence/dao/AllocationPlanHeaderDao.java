package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AllocationPlanHeaderEntity;

public interface AllocationPlanHeaderDao extends CrudDao<AllocationPlanHeaderEntity, AllocationPlanHeaderEntity, SqlBuilder.PSC, AllocationPlanHeaderDao>, DelegatorQueryDao {
}
