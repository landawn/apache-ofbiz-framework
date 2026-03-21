package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TaxAuthorityRateTypeEntity;

public interface TaxAuthorityRateTypeDao extends CrudDao<TaxAuthorityRateTypeEntity, String, SqlBuilder.PSC, TaxAuthorityRateTypeDao> {
}
