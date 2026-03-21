package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.GlResourceTypeEntity;

public interface GlResourceTypeDao extends CrudDao<GlResourceTypeEntity, String, SqlBuilder.PSC, GlResourceTypeDao> {
}
