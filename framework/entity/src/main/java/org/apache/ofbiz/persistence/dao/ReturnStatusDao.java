package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ReturnStatusEntity;

public interface ReturnStatusDao extends CrudDao<ReturnStatusEntity, String, SqlBuilder.PSC, ReturnStatusDao> {
}
