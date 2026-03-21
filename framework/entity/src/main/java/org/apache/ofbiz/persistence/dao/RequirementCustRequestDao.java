package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.RequirementCustRequestEntity;

public interface RequirementCustRequestDao extends CrudDao<RequirementCustRequestEntity, RequirementCustRequestEntity, SqlBuilder.PSC, RequirementCustRequestDao> {
}
