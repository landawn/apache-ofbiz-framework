package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SalesOpportunityQuoteEntity;

public interface SalesOpportunityQuoteDao extends CrudDao<SalesOpportunityQuoteEntity, SalesOpportunityQuoteEntity, SQLBuilder.PSC, SalesOpportunityQuoteDao> {
}
