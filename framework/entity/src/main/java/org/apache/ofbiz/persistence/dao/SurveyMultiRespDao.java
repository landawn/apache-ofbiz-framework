package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SurveyMultiRespEntity;

public interface SurveyMultiRespDao extends CrudDao<SurveyMultiRespEntity, SurveyMultiRespEntity, SQLBuilder.PSC, SurveyMultiRespDao> {
}
