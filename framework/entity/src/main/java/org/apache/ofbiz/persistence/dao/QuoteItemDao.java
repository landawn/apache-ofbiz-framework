package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.QuoteItemEntity;

public interface QuoteItemDao extends CrudDao<QuoteItemEntity, QuoteItemEntity, SqlBuilder.PSC, QuoteItemDao> {
}
