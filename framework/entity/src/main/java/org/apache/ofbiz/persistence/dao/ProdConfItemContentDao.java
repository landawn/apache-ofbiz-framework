package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ProdConfItemContentEntity;

public interface ProdConfItemContentDao extends CrudDao<ProdConfItemContentEntity, ProdConfItemContentEntity, SqlBuilder.PSC, ProdConfItemContentDao> {
}
