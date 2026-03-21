package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TestingSubtypeEntity;

public interface TestingSubtypeDao extends CrudDao<TestingSubtypeEntity, String, SqlBuilder.PSC, TestingSubtypeDao> {
}
