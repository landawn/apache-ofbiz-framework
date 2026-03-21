package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TaxAuthorityAssocEntity;

public interface TaxAuthorityAssocDao extends CrudDao<TaxAuthorityAssocEntity, TaxAuthorityAssocEntity, SqlBuilder.PSC, TaxAuthorityAssocDao> {
}
