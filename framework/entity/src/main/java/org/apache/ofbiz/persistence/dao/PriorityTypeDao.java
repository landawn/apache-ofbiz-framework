package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PriorityTypeEntity;

public interface PriorityTypeDao extends CrudDao<PriorityTypeEntity, String, SqlBuilder.PSC, PriorityTypeDao> {
}
