package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TaxAuthorityEntity;

public interface TaxAuthorityDao extends CrudDao<TaxAuthorityEntity, TaxAuthorityEntity, SqlBuilder.PSC, TaxAuthorityDao> {
}
