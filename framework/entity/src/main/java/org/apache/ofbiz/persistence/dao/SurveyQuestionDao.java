package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.SurveyQuestionEntity;

public interface SurveyQuestionDao extends CrudDao<SurveyQuestionEntity, String, SqlBuilder.PSC, SurveyQuestionDao> {
}
