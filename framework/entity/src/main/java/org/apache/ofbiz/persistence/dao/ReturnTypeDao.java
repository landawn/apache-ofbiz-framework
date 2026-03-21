package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ReturnTypeEntity;

public interface ReturnTypeDao extends CrudDao<ReturnTypeEntity, String, SqlBuilder.PSC, ReturnTypeDao> {
}
