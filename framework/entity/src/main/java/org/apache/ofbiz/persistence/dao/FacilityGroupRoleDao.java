package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FacilityGroupRoleEntity;

public interface FacilityGroupRoleDao extends CrudDao<FacilityGroupRoleEntity, FacilityGroupRoleEntity, SQLBuilder.PSC, FacilityGroupRoleDao> {
}
