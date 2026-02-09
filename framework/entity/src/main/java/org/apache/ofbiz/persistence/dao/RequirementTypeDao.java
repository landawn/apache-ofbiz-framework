package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.RequirementTypeEntity;

public interface RequirementTypeDao extends CrudDao<RequirementTypeEntity, String, SQLBuilder.PSC, RequirementTypeDao> {
}
