package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SurveyQuestionTypeEntity;

public interface SurveyQuestionTypeDao extends CrudDao<SurveyQuestionTypeEntity, String, SQLBuilder.PSC, SurveyQuestionTypeDao> {
}
