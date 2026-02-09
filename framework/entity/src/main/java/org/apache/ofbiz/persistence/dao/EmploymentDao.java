package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.EmploymentEntity;

public interface EmploymentDao extends CrudDao<EmploymentEntity, EmploymentEntity, SQLBuilder.PSC, EmploymentDao> {
}
