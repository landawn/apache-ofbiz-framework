package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ConfigOptionProductOptionEntity;

public interface ConfigOptionProductOptionDao extends CrudDao<ConfigOptionProductOptionEntity, ConfigOptionProductOptionEntity, SqlBuilder.PSC, ConfigOptionProductOptionDao> {
}
