package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.WebPreferenceTypeEntity;

public interface WebPreferenceTypeDao extends CrudDao<WebPreferenceTypeEntity, String, SqlBuilder.PSC, WebPreferenceTypeDao> {
}
