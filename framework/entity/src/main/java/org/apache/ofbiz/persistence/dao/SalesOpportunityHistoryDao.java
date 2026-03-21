package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.SalesOpportunityHistoryEntity;

public interface SalesOpportunityHistoryDao extends CrudDao<SalesOpportunityHistoryEntity, String, SqlBuilder.PSC, SalesOpportunityHistoryDao> {
}
