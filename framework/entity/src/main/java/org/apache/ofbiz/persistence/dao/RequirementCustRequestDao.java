package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.RequirementCustRequestEntity;

public interface RequirementCustRequestDao extends CrudDao<RequirementCustRequestEntity, RequirementCustRequestEntity, SQLBuilder.PSC, RequirementCustRequestDao> {
}
