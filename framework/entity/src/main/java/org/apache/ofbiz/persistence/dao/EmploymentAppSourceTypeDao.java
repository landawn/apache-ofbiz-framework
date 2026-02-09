package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.EmploymentAppSourceTypeEntity;

public interface EmploymentAppSourceTypeDao extends CrudDao<EmploymentAppSourceTypeEntity, String, SQLBuilder.PSC, EmploymentAppSourceTypeDao> {
}
