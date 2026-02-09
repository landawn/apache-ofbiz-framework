package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.GlAccountCategoryTypeEntity;

public interface GlAccountCategoryTypeDao extends CrudDao<GlAccountCategoryTypeEntity, String, SQLBuilder.PSC, GlAccountCategoryTypeDao> {
}
