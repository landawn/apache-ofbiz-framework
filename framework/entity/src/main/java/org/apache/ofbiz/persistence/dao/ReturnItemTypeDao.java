package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ReturnItemTypeEntity;

public interface ReturnItemTypeDao extends CrudDao<ReturnItemTypeEntity, String, SqlBuilder.PSC, ReturnItemTypeDao> {
}
