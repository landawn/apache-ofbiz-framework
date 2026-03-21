package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.QuoteAdjustmentEntity;

public interface QuoteAdjustmentDao extends CrudDao<QuoteAdjustmentEntity, String, SqlBuilder.PSC, QuoteAdjustmentDao> {
}
