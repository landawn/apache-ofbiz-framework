package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TaxAuthorityRateProductEntity;

public interface TaxAuthorityRateProductDao extends CrudDao<TaxAuthorityRateProductEntity, String, SQLBuilder.PSC, TaxAuthorityRateProductDao> {
}
