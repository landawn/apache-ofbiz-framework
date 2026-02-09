package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PersonEntity;

public interface PersonDao extends CrudDao<PersonEntity, String, SQLBuilder.PSC, PersonDao> {
}
