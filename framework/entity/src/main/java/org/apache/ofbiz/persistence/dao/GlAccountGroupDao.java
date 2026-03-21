package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.GlAccountGroupEntity;

public interface GlAccountGroupDao extends CrudDao<GlAccountGroupEntity, String, SqlBuilder.PSC, GlAccountGroupDao> {
}
