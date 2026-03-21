package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ReturnReasonEntity;

public interface ReturnReasonDao extends CrudDao<ReturnReasonEntity, String, SqlBuilder.PSC, ReturnReasonDao> {
}
