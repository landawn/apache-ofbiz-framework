package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.RequirementStatusEntity;

public interface RequirementStatusDao extends CrudDao<RequirementStatusEntity, RequirementStatusEntity, SQLBuilder.PSC, RequirementStatusDao> {
}
