package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SurveyResponseEntity;

public interface SurveyResponseDao extends CrudDao<SurveyResponseEntity, String, SQLBuilder.PSC, SurveyResponseDao> {
}
