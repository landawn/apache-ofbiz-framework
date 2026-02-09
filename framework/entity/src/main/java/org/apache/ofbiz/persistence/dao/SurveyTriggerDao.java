package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SurveyTriggerEntity;

public interface SurveyTriggerDao extends CrudDao<SurveyTriggerEntity, SurveyTriggerEntity, SQLBuilder.PSC, SurveyTriggerDao> {
}
