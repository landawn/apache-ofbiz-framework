package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.GlXbrlClassEntity;

public interface GlXbrlClassDao extends CrudDao<GlXbrlClassEntity, String, SqlBuilder.PSC, GlXbrlClassDao> {
}
