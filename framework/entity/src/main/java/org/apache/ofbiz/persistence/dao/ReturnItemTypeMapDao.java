package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ReturnItemTypeMapEntity;

public interface ReturnItemTypeMapDao extends CrudDao<ReturnItemTypeMapEntity, ReturnItemTypeMapEntity, SQLBuilder.PSC, ReturnItemTypeMapDao> {
}
