package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TerminationReasonEntity;

public interface TerminationReasonDao extends CrudDao<TerminationReasonEntity, String, SQLBuilder.PSC, TerminationReasonDao> {
}
