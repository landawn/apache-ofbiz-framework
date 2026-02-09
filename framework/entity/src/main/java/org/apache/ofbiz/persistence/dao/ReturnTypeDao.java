package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ReturnTypeEntity;

public interface ReturnTypeDao extends CrudDao<ReturnTypeEntity, String, SQLBuilder.PSC, ReturnTypeDao> {
}
