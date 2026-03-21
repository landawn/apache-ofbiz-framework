package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.GlReconciliationEntryEntity;

public interface GlReconciliationEntryDao extends CrudDao<GlReconciliationEntryEntity, GlReconciliationEntryEntity, SqlBuilder.PSC, GlReconciliationEntryDao> {
}
