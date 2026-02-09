package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.RequirementAttributeEntity;

public interface RequirementAttributeDao extends CrudDao<RequirementAttributeEntity, RequirementAttributeEntity, SQLBuilder.PSC, RequirementAttributeDao> {
}
