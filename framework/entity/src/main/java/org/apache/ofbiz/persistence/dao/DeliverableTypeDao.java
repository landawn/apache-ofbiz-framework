package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.DeliverableTypeEntity;

public interface DeliverableTypeDao extends CrudDao<DeliverableTypeEntity, String, SqlBuilder.PSC, DeliverableTypeDao> {
}
