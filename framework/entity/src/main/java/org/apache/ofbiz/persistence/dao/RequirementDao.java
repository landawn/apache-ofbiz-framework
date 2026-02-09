package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.RequirementEntity;

public interface RequirementDao extends CrudDao<RequirementEntity, String, SQLBuilder.PSC, RequirementDao> {
}
