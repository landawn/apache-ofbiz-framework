package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShoppingListEntity;

public interface ShoppingListDao extends CrudDao<ShoppingListEntity, String, SqlBuilder.PSC, ShoppingListDao> {
}
