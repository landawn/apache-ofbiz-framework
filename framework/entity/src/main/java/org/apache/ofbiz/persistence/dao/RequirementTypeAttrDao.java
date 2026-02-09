package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.RequirementTypeAttrEntity;

public interface RequirementTypeAttrDao extends CrudDao<RequirementTypeAttrEntity, RequirementTypeAttrEntity, SQLBuilder.PSC, RequirementTypeAttrDao> {
}
