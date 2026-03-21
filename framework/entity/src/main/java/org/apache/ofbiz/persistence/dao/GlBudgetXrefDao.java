package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.GlBudgetXrefEntity;

public interface GlBudgetXrefDao extends CrudDao<GlBudgetXrefEntity, GlBudgetXrefEntity, SqlBuilder.PSC, GlBudgetXrefDao> {
}
