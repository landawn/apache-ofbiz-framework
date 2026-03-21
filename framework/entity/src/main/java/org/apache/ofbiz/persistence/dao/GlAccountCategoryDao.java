package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.GlAccountCategoryEntity;

public interface GlAccountCategoryDao extends CrudDao<GlAccountCategoryEntity, String, SqlBuilder.PSC, GlAccountCategoryDao> {
}
