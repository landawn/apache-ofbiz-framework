package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.QuoteCoefficientEntity;

public interface QuoteCoefficientDao extends CrudDao<QuoteCoefficientEntity, QuoteCoefficientEntity, SQLBuilder.PSC, QuoteCoefficientDao> {
}
