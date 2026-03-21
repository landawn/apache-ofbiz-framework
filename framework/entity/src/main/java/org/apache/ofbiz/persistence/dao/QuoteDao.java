package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.QuoteEntity;

public interface QuoteDao extends CrudDao<QuoteEntity, String, SqlBuilder.PSC, QuoteDao> {
}
