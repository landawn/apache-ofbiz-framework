package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TestFieldTypeEntity;

public interface TestFieldTypeDao extends CrudDao<TestFieldTypeEntity, String, SqlBuilder.PSC, TestFieldTypeDao> {
}
