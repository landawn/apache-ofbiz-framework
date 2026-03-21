package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProductCategoryMemberEntity;

public interface ProductCategoryMemberDao
        extends CrudDao<ProductCategoryMemberEntity, ProductCategoryMemberEntity, SqlBuilder.PSC, ProductCategoryMemberDao>, DelegatorQueryDao {
}
