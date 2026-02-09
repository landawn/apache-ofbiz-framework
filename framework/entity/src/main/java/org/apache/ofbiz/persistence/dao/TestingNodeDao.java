package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TestingNodeEntity;

public interface TestingNodeDao extends CrudDao<TestingNodeEntity, String, SQLBuilder.PSC, TestingNodeDao> {
}
