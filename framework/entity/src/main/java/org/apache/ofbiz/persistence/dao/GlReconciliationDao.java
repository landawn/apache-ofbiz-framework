package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.GlReconciliationEntity;

public interface GlReconciliationDao extends CrudDao<GlReconciliationEntity, String, SqlBuilder.PSC, GlReconciliationDao> {
}
