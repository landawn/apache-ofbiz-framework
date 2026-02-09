package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TemporalExpressionAssocEntity;

public interface TemporalExpressionAssocDao extends CrudDao<TemporalExpressionAssocEntity, TemporalExpressionAssocEntity, SQLBuilder.PSC, TemporalExpressionAssocDao> {
}
