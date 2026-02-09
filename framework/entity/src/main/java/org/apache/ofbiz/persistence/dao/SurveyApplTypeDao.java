package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SurveyApplTypeEntity;

public interface SurveyApplTypeDao extends CrudDao<SurveyApplTypeEntity, String, SQLBuilder.PSC, SurveyApplTypeDao> {
}
