package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FacilityGroupMemberEntity;

public interface FacilityGroupMemberDao extends CrudDao<FacilityGroupMemberEntity, FacilityGroupMemberEntity, SQLBuilder.PSC, FacilityGroupMemberDao> {
}
