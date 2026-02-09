package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SurveyPageEntity;

public interface SurveyPageDao extends CrudDao<SurveyPageEntity, SurveyPageEntity, SQLBuilder.PSC, SurveyPageDao> {
}
