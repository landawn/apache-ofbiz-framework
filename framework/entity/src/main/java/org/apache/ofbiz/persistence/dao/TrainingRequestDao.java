package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TrainingRequestEntity;

public interface TrainingRequestDao extends CrudDao<TrainingRequestEntity, String, SqlBuilder.PSC, TrainingRequestDao> {
}
