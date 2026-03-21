package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.GlAccountTypeDefaultEntity;

public interface GlAccountTypeDefaultDao extends CrudDao<GlAccountTypeDefaultEntity, GlAccountTypeDefaultEntity, SqlBuilder.PSC, GlAccountTypeDefaultDao> {
}
