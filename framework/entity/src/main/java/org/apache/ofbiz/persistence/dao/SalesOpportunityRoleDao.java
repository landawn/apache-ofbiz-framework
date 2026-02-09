package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SalesOpportunityRoleEntity;

public interface SalesOpportunityRoleDao extends CrudDao<SalesOpportunityRoleEntity, SalesOpportunityRoleEntity, SQLBuilder.PSC, SalesOpportunityRoleDao> {
}
