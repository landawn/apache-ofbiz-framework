package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CustomMethodTypeEntity;

public interface CustomMethodTypeDao extends CrudDao<CustomMethodTypeEntity, String, SqlBuilder.PSC, CustomMethodTypeDao> {
}
