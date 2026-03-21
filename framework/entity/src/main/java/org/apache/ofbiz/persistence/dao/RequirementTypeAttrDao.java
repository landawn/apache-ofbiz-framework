package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.RequirementTypeAttrEntity;

public interface RequirementTypeAttrDao extends CrudDao<RequirementTypeAttrEntity, RequirementTypeAttrEntity, SqlBuilder.PSC, RequirementTypeAttrDao> {
}
