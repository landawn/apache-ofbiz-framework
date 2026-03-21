package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.DeliverableEntity;

public interface DeliverableDao extends CrudDao<DeliverableEntity, String, SqlBuilder.PSC, DeliverableDao> {
}
