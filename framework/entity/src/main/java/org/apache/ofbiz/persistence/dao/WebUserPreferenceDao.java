package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.WebUserPreferenceEntity;

public interface WebUserPreferenceDao extends CrudDao<WebUserPreferenceEntity, WebUserPreferenceEntity, SQLBuilder.PSC, WebUserPreferenceDao> {
}
