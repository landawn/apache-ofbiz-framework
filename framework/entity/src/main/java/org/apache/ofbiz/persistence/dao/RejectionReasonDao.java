package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.RejectionReasonEntity;

public interface RejectionReasonDao extends CrudDao<RejectionReasonEntity, String, SQLBuilder.PSC, RejectionReasonDao> {
}
