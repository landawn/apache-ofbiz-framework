package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.RejectionReasonEntity;

public interface RejectionReasonDao extends CrudDao<RejectionReasonEntity, String, SqlBuilder.PSC, RejectionReasonDao> {
}
