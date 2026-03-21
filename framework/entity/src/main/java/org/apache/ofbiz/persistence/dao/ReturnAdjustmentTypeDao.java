package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ReturnAdjustmentTypeEntity;

public interface ReturnAdjustmentTypeDao extends CrudDao<ReturnAdjustmentTypeEntity, String, SqlBuilder.PSC, ReturnAdjustmentTypeDao> {
}
