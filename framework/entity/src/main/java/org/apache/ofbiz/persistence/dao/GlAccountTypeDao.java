package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.GlAccountTypeEntity;

public interface GlAccountTypeDao extends CrudDao<GlAccountTypeEntity, String, SQLBuilder.PSC, GlAccountTypeDao> {
}
