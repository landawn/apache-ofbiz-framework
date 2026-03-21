package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TestingItemEntity;

public interface TestingItemDao extends CrudDao<TestingItemEntity, TestingItemEntity, SqlBuilder.PSC, TestingItemDao> {
}
