package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.GlAccountGroupEntity;

public interface GlAccountGroupDao extends CrudDao<GlAccountGroupEntity, String, SQLBuilder.PSC, GlAccountGroupDao> {
}
