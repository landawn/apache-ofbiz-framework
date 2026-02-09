package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProdConfItemContentEntity;

public interface ProdConfItemContentDao extends CrudDao<ProdConfItemContentEntity, ProdConfItemContentEntity, SQLBuilder.PSC, ProdConfItemContentDao> {
}
