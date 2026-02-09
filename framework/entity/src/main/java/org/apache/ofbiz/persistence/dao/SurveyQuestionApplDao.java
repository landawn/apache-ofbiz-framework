package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SurveyQuestionApplEntity;

public interface SurveyQuestionApplDao extends CrudDao<SurveyQuestionApplEntity, SurveyQuestionApplEntity, SQLBuilder.PSC, SurveyQuestionApplDao> {
}
