package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ResponsibilityTypeEntity;

public interface ResponsibilityTypeDao extends CrudDao<ResponsibilityTypeEntity, String, SqlBuilder.PSC, ResponsibilityTypeDao> {
}
