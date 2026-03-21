package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TestingEntity;

public interface TestingDao extends CrudDao<TestingEntity, String, SqlBuilder.PSC, TestingDao> {
}
