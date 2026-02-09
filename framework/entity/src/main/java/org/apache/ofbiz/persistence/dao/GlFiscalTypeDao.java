package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.GlFiscalTypeEntity;

public interface GlFiscalTypeDao extends CrudDao<GlFiscalTypeEntity, String, SQLBuilder.PSC, GlFiscalTypeDao> {
}
