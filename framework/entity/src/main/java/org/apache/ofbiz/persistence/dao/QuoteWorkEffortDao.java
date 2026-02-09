package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.QuoteWorkEffortEntity;

public interface QuoteWorkEffortDao extends CrudDao<QuoteWorkEffortEntity, QuoteWorkEffortEntity, SQLBuilder.PSC, QuoteWorkEffortDao> {
}
