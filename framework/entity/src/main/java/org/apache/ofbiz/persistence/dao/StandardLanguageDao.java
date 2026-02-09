package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.StandardLanguageEntity;

public interface StandardLanguageDao extends CrudDao<StandardLanguageEntity, String, SQLBuilder.PSC, StandardLanguageDao> {
}
