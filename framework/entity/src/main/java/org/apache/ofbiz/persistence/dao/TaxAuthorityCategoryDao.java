package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TaxAuthorityCategoryEntity;

public interface TaxAuthorityCategoryDao extends CrudDao<TaxAuthorityCategoryEntity, TaxAuthorityCategoryEntity, SqlBuilder.PSC, TaxAuthorityCategoryDao> {
}
