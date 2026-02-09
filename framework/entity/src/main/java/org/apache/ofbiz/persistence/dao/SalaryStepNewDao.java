package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SalaryStepNewEntity;

public interface SalaryStepNewDao extends CrudDao<SalaryStepNewEntity, SalaryStepNewEntity, SQLBuilder.PSC, SalaryStepNewDao> {
}
