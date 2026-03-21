package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TestingRemoveAllEntity;

public interface TestingRemoveAllDao extends CrudDao<TestingRemoveAllEntity, String, SqlBuilder.PSC, TestingRemoveAllDao> {
}
