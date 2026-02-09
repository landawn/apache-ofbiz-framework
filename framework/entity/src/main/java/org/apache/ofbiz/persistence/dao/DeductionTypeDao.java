package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.DeductionTypeEntity;

public interface DeductionTypeDao extends CrudDao<DeductionTypeEntity, String, SQLBuilder.PSC, DeductionTypeDao> {
}
