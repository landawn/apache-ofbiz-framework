package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SurveyResponseAnswerEntity;

public interface SurveyResponseAnswerDao extends CrudDao<SurveyResponseAnswerEntity, SurveyResponseAnswerEntity, SQLBuilder.PSC, SurveyResponseAnswerDao> {
}
