package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PlatformTypeEntity;

public interface PlatformTypeDao extends CrudDao<PlatformTypeEntity, String, SqlBuilder.PSC, PlatformTypeDao> {
}
