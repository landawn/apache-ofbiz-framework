package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ReturnAdjustmentEntity;

public interface ReturnAdjustmentDao extends CrudDao<ReturnAdjustmentEntity, String, SQLBuilder.PSC, ReturnAdjustmentDao> {
}
