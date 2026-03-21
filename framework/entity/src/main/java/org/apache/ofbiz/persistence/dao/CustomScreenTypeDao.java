package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CustomScreenTypeEntity;

public interface CustomScreenTypeDao extends CrudDao<CustomScreenTypeEntity, String, SqlBuilder.PSC, CustomScreenTypeDao> {
}
