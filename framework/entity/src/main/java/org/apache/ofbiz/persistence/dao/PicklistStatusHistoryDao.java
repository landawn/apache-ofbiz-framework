package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PicklistStatusHistoryEntity;

public interface PicklistStatusHistoryDao extends CrudDao<PicklistStatusHistoryEntity, PicklistStatusHistoryEntity, SqlBuilder.PSC, PicklistStatusHistoryDao> {
}
