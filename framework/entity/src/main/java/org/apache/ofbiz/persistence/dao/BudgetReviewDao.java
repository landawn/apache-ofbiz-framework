package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.BudgetReviewEntity;

public interface BudgetReviewDao extends CrudDao<BudgetReviewEntity, BudgetReviewEntity, SqlBuilder.PSC, BudgetReviewDao> {
}
