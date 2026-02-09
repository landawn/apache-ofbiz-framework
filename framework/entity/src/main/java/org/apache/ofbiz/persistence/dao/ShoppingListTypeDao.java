package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ShoppingListTypeEntity;

public interface ShoppingListTypeDao extends CrudDao<ShoppingListTypeEntity, String, SQLBuilder.PSC, ShoppingListTypeDao> {
}
