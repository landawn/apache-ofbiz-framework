package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.EmploymentAppEntity;

public interface EmploymentAppDao extends CrudDao<EmploymentAppEntity, String, SQLBuilder.PSC, EmploymentAppDao> {
}
