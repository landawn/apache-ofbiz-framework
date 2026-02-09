package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.QuoteTermAttributeEntity;

public interface QuoteTermAttributeDao extends CrudDao<QuoteTermAttributeEntity, QuoteTermAttributeEntity, SQLBuilder.PSC, QuoteTermAttributeDao> {
}
