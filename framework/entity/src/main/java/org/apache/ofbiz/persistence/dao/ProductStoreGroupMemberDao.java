package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductStoreGroupMemberEntity;

public interface ProductStoreGroupMemberDao extends CrudDao<ProductStoreGroupMemberEntity, ProductStoreGroupMemberEntity, SQLBuilder.PSC, ProductStoreGroupMemberDao> {
}
