package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.DeductionEntity;

public interface DeductionDao extends CrudDao<DeductionEntity, String, SQLBuilder.PSC, DeductionDao> {
}
