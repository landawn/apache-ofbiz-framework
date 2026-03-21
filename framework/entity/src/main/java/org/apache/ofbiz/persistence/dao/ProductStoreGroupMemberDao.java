package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreGroupMemberEntity;

public interface ProductStoreGroupMemberDao extends CrudDao<ProductStoreGroupMemberEntity, ProductStoreGroupMemberEntity, SqlBuilder.PSC, ProductStoreGroupMemberDao> {
}
