package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.CreditCardTypeGlAccountEntity;

public interface CreditCardTypeGlAccountDao extends CrudDao<CreditCardTypeGlAccountEntity, CreditCardTypeGlAccountEntity, SqlBuilder.PSC, CreditCardTypeGlAccountDao> {
}
