package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.QuoteTypeAttrEntity;

public interface QuoteTypeAttrDao extends CrudDao<QuoteTypeAttrEntity, QuoteTypeAttrEntity, SQLBuilder.PSC, QuoteTypeAttrDao> {
}
