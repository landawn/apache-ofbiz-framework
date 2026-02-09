package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.GlAccountCategoryEntity;

public interface GlAccountCategoryDao extends CrudDao<GlAccountCategoryEntity, String, SQLBuilder.PSC, GlAccountCategoryDao> {
}
