package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.UserPreferenceEntity;

public interface UserPreferenceDao extends CrudDao<UserPreferenceEntity, UserPreferenceEntity, SQLBuilder.PSC, UserPreferenceDao> {
}
