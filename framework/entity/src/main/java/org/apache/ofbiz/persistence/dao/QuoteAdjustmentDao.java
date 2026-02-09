package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.QuoteAdjustmentEntity;

public interface QuoteAdjustmentDao extends CrudDao<QuoteAdjustmentEntity, String, SQLBuilder.PSC, QuoteAdjustmentDao> {
}
