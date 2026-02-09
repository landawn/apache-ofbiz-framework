package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ReturnReasonEntity;

public interface ReturnReasonDao extends CrudDao<ReturnReasonEntity, String, SQLBuilder.PSC, ReturnReasonDao> {
}
