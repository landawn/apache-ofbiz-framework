package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.UomGroupEntity;

public interface UomGroupDao extends CrudDao<UomGroupEntity, UomGroupEntity, SqlBuilder.PSC, UomGroupDao> {
}
