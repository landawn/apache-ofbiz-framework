package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TemporalExpressionAssocEntity;

public interface TemporalExpressionAssocDao extends CrudDao<TemporalExpressionAssocEntity, TemporalExpressionAssocEntity, SqlBuilder.PSC, TemporalExpressionAssocDao> {
}
