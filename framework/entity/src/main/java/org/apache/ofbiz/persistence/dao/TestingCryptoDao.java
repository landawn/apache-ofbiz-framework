package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TestingCryptoEntity;

public interface TestingCryptoDao extends CrudDao<TestingCryptoEntity, String, SQLBuilder.PSC, TestingCryptoDao> {
}
