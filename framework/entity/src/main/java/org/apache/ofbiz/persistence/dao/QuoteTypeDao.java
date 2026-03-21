package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.QuoteTypeEntity;

public interface QuoteTypeDao extends CrudDao<QuoteTypeEntity, String, SqlBuilder.PSC, QuoteTypeDao> {
}
