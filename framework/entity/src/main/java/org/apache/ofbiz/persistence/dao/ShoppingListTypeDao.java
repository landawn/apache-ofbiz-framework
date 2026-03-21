package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShoppingListTypeEntity;

public interface ShoppingListTypeDao extends CrudDao<ShoppingListTypeEntity, String, SqlBuilder.PSC, ShoppingListTypeDao> {
}
