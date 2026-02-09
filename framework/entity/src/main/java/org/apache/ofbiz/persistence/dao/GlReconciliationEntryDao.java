package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.GlReconciliationEntryEntity;

public interface GlReconciliationEntryDao extends CrudDao<GlReconciliationEntryEntity, GlReconciliationEntryEntity, SQLBuilder.PSC, GlReconciliationEntryDao> {
}
