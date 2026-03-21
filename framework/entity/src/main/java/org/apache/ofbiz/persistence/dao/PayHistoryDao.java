package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PayHistoryEntity;

public interface PayHistoryDao extends CrudDao<PayHistoryEntity, PayHistoryEntity, SqlBuilder.PSC, PayHistoryDao> {
}
