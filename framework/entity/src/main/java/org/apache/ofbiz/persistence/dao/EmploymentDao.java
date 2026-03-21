package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.EmploymentEntity;

public interface EmploymentDao extends CrudDao<EmploymentEntity, EmploymentEntity, SqlBuilder.PSC, EmploymentDao> {
}
