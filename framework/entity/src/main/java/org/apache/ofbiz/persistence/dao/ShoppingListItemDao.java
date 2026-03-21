package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShoppingListItemEntity;

public interface ShoppingListItemDao extends CrudDao<ShoppingListItemEntity, ShoppingListItemEntity, SqlBuilder.PSC, ShoppingListItemDao> {
}
