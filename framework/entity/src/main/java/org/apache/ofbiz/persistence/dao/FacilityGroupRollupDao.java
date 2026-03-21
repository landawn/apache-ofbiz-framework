package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FacilityGroupRollupEntity;

public interface FacilityGroupRollupDao extends CrudDao<FacilityGroupRollupEntity, FacilityGroupRollupEntity, SqlBuilder.PSC, FacilityGroupRollupDao> {
}
