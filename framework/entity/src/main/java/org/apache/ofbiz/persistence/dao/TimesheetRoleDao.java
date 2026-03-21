package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TimesheetRoleEntity;

public interface TimesheetRoleDao extends CrudDao<TimesheetRoleEntity, TimesheetRoleEntity, SqlBuilder.PSC, TimesheetRoleDao> {
}
