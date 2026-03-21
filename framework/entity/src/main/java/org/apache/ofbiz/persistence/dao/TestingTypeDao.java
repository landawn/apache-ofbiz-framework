package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TestingTypeEntity;

public interface TestingTypeDao extends CrudDao<TestingTypeEntity, String, SqlBuilder.PSC, TestingTypeDao> {
}
