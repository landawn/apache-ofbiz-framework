package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.BudgetReviewResultTypeEntity;

public interface BudgetReviewResultTypeDao extends CrudDao<BudgetReviewResultTypeEntity, String, SQLBuilder.PSC, BudgetReviewResultTypeDao> {
}
