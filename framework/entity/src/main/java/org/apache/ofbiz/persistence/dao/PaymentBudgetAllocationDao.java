package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PaymentBudgetAllocationEntity;

public interface PaymentBudgetAllocationDao extends CrudDao<PaymentBudgetAllocationEntity, PaymentBudgetAllocationEntity, SqlBuilder.PSC, PaymentBudgetAllocationDao> {
}
