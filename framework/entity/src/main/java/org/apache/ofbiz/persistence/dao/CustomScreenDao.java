package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CustomScreenEntity;

public interface CustomScreenDao extends CrudDao<CustomScreenEntity, String, SqlBuilder.PSC, CustomScreenDao> {
}
