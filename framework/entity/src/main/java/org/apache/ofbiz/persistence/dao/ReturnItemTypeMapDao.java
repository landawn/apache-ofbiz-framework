package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ReturnItemTypeMapEntity;

public interface ReturnItemTypeMapDao extends CrudDao<ReturnItemTypeMapEntity, ReturnItemTypeMapEntity, SqlBuilder.PSC, ReturnItemTypeMapDao>, DelegatorQueryDao {
}
