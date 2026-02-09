package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.RateTypeEntity;

public interface RateTypeDao extends CrudDao<RateTypeEntity, String, SQLBuilder.PSC, RateTypeDao> {
}
