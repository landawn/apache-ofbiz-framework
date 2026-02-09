package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ReturnStatusEntity;

public interface ReturnStatusDao extends CrudDao<ReturnStatusEntity, String, SQLBuilder.PSC, ReturnStatusDao> {
}
