package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TerminationReasonEntity;

public interface TerminationReasonDao extends CrudDao<TerminationReasonEntity, String, SqlBuilder.PSC, TerminationReasonDao> {
}
