package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FinAccountAttributeEntity;

public interface FinAccountAttributeDao extends CrudDao<FinAccountAttributeEntity, FinAccountAttributeEntity, SQLBuilder.PSC, FinAccountAttributeDao> {
}
