package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TestingStatusEntity;

public interface TestingStatusDao extends CrudDao<TestingStatusEntity, String, SqlBuilder.PSC, TestingStatusDao> {
}
