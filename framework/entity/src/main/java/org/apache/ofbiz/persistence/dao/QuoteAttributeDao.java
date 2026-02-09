package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.QuoteAttributeEntity;

public interface QuoteAttributeDao extends CrudDao<QuoteAttributeEntity, QuoteAttributeEntity, SQLBuilder.PSC, QuoteAttributeDao> {
}
