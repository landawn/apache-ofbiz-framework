package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.RuntimeDataEntity;

public interface RuntimeDataDao extends CrudDao<RuntimeDataEntity, String, SqlBuilder.PSC, RuntimeDataDao> {
}
