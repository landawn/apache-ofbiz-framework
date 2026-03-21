package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.SurveyQuestionApplEntity;

public interface SurveyQuestionApplDao extends CrudDao<SurveyQuestionApplEntity, SurveyQuestionApplEntity, SqlBuilder.PSC, SurveyQuestionApplDao> {
}
