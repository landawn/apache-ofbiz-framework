package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.SurveyQuestionOptionEntity;

public interface SurveyQuestionOptionDao extends CrudDao<SurveyQuestionOptionEntity, SurveyQuestionOptionEntity, SqlBuilder.PSC, SurveyQuestionOptionDao> {
}
