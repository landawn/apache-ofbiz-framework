package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProductRoleEntity;

public interface ProductRoleDao extends CrudDao<ProductRoleEntity, ProductRoleEntity, SQLBuilder.PSC, ProductRoleDao> {
}
