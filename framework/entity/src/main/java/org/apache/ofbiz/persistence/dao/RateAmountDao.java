package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.RateAmountEntity;

public interface RateAmountDao extends CrudDao<RateAmountEntity, RateAmountEntity, SQLBuilder.PSC, RateAmountDao> {
}
