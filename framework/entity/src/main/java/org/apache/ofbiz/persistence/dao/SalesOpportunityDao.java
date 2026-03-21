package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.SalesOpportunityEntity;

public interface SalesOpportunityDao extends CrudDao<SalesOpportunityEntity, String, SqlBuilder.PSC, SalesOpportunityDao> {
}
