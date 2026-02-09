package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CreditCardEntity;

public interface CreditCardDao extends CrudDao<CreditCardEntity, String, SQLBuilder.PSC, CreditCardDao> {
}
