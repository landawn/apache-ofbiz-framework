package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ShoppingListItemSurveyEntity;

public interface ShoppingListItemSurveyDao extends CrudDao<ShoppingListItemSurveyEntity, ShoppingListItemSurveyEntity, SqlBuilder.PSC, ShoppingListItemSurveyDao> {
}
