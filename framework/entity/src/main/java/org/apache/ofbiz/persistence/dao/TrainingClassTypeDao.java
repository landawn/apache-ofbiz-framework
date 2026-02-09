package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TrainingClassTypeEntity;

public interface TrainingClassTypeDao extends CrudDao<TrainingClassTypeEntity, String, SQLBuilder.PSC, TrainingClassTypeDao> {
}
