package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.GlAccountClassEntity;

public interface GlAccountClassDao extends CrudDao<GlAccountClassEntity, String, SqlBuilder.PSC, GlAccountClassDao> {
}
