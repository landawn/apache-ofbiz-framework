package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ReturnItemResponseEntity;

public interface ReturnItemResponseDao extends CrudDao<ReturnItemResponseEntity, String, SqlBuilder.PSC, ReturnItemResponseDao>, DelegatorQueryDao {
}
