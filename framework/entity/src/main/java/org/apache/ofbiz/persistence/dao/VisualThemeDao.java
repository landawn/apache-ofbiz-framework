package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.VisualThemeEntity;

public interface VisualThemeDao extends CrudDao<VisualThemeEntity, String, SqlBuilder.PSC, VisualThemeDao> {
}
