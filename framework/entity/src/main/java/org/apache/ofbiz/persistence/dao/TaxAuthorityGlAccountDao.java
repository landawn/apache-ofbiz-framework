package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TaxAuthorityGlAccountEntity;

public interface TaxAuthorityGlAccountDao extends CrudDao<TaxAuthorityGlAccountEntity, TaxAuthorityGlAccountEntity, SQLBuilder.PSC, TaxAuthorityGlAccountDao> {
}
