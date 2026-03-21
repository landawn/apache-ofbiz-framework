package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.GlAccountOrganizationEntity;

public interface GlAccountOrganizationDao extends CrudDao<GlAccountOrganizationEntity, GlAccountOrganizationEntity, SqlBuilder.PSC, GlAccountOrganizationDao> {
}
