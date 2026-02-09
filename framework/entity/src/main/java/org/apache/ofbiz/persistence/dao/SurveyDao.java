package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SurveyEntity;

public interface SurveyDao extends CrudDao<SurveyEntity, String, SQLBuilder.PSC, SurveyDao> {
}
