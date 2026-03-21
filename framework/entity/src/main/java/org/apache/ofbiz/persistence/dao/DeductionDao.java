package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.DeductionEntity;

public interface DeductionDao extends CrudDao<DeductionEntity, String, SqlBuilder.PSC, DeductionDao> {
}
