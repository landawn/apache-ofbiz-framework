package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.GlAccountClassEntity;

public interface GlAccountClassDao extends CrudDao<GlAccountClassEntity, String, SQLBuilder.PSC, GlAccountClassDao> {
}
