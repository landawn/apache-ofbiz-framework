package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ProdConfItemContentTypeEntity;

public interface ProdConfItemContentTypeDao extends CrudDao<ProdConfItemContentTypeEntity, String, SQLBuilder.PSC, ProdConfItemContentTypeDao> {
}
