package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShoppingListWorkEffortEntity;

public interface ShoppingListWorkEffortDao extends CrudDao<ShoppingListWorkEffortEntity, ShoppingListWorkEffortEntity, SqlBuilder.PSC, ShoppingListWorkEffortDao> {
}
