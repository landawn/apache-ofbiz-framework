package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.RateTypeEntity;

public interface RateTypeDao extends CrudDao<RateTypeEntity, String, SqlBuilder.PSC, RateTypeDao> {
}
