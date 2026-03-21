package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PostalAddressBoundaryEntity;

public interface PostalAddressBoundaryDao extends CrudDao<PostalAddressBoundaryEntity, PostalAddressBoundaryEntity, SqlBuilder.PSC, PostalAddressBoundaryDao> {
}
