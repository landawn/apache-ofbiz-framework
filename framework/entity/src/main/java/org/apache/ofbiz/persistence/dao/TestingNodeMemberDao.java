package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TestingNodeMemberEntity;

public interface TestingNodeMemberDao extends CrudDao<TestingNodeMemberEntity, TestingNodeMemberEntity, SqlBuilder.PSC, TestingNodeMemberDao> {
}
