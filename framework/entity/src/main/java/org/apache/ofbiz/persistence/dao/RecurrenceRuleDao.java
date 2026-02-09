package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.RecurrenceRuleEntity;

public interface RecurrenceRuleDao extends CrudDao<RecurrenceRuleEntity, String, SQLBuilder.PSC, RecurrenceRuleDao> {
}
