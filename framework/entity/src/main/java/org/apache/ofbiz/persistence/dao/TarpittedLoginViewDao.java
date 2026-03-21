package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TarpittedLoginViewEntity;

public interface TarpittedLoginViewDao extends CrudDao<TarpittedLoginViewEntity, TarpittedLoginViewEntity, SqlBuilder.PSC, TarpittedLoginViewDao> {
}
