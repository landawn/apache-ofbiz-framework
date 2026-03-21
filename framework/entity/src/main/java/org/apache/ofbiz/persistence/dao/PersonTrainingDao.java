package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PersonTrainingEntity;

public interface PersonTrainingDao extends CrudDao<PersonTrainingEntity, PersonTrainingEntity, SqlBuilder.PSC, PersonTrainingDao> {
}
