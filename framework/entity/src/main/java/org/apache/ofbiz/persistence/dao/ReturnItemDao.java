package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ReturnItemEntity;

public interface ReturnItemDao extends CrudDao<ReturnItemEntity, ReturnItemEntity, SqlBuilder.PSC, ReturnItemDao>, DelegatorQueryDao {
}
