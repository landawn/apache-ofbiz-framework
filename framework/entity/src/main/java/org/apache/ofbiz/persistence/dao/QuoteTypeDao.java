package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.QuoteTypeEntity;

public interface QuoteTypeDao extends CrudDao<QuoteTypeEntity, String, SQLBuilder.PSC, QuoteTypeDao> {
}
