package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.GlXbrlClassEntity;

public interface GlXbrlClassDao extends CrudDao<GlXbrlClassEntity, String, SQLBuilder.PSC, GlXbrlClassDao> {
}
