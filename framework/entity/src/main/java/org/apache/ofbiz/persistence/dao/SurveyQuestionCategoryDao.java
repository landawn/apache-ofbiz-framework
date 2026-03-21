package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.SurveyQuestionCategoryEntity;

public interface SurveyQuestionCategoryDao extends CrudDao<SurveyQuestionCategoryEntity, String, SqlBuilder.PSC, SurveyQuestionCategoryDao> {
}
