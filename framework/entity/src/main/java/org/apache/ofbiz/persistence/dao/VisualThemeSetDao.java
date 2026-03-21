package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.VisualThemeSetEntity;

public interface VisualThemeSetDao extends CrudDao<VisualThemeSetEntity, String, SqlBuilder.PSC, VisualThemeSetDao> {
}
