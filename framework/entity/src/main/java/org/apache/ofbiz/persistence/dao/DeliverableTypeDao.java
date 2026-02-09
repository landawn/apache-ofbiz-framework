package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.DeliverableTypeEntity;

public interface DeliverableTypeDao extends CrudDao<DeliverableTypeEntity, String, SQLBuilder.PSC, DeliverableTypeDao> {
}
