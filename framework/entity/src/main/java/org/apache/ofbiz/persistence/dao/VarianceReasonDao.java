package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.VarianceReasonEntity;

public interface VarianceReasonDao extends CrudDao<VarianceReasonEntity, String, SQLBuilder.PSC, VarianceReasonDao> {
}
