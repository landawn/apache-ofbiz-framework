package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.StatusTypeEntity;

public interface StatusTypeDao extends CrudDao<StatusTypeEntity, String, SQLBuilder.PSC, StatusTypeDao> {
}
