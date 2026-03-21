package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.SalesOpportunityQuoteEntity;

public interface SalesOpportunityQuoteDao extends CrudDao<SalesOpportunityQuoteEntity, SalesOpportunityQuoteEntity, SqlBuilder.PSC, SalesOpportunityQuoteDao> {
}
