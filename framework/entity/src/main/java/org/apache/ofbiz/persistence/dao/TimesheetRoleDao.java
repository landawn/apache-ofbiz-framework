package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TimesheetRoleEntity;

public interface TimesheetRoleDao extends CrudDao<TimesheetRoleEntity, TimesheetRoleEntity, SQLBuilder.PSC, TimesheetRoleDao> {
}
