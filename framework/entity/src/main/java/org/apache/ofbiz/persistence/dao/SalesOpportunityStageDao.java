package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.SalesOpportunityStageEntity;

public interface SalesOpportunityStageDao extends CrudDao<SalesOpportunityStageEntity, String, SqlBuilder.PSC, SalesOpportunityStageDao> {
}
