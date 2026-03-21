package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OtherDataResourceEntity;

public interface OtherDataResourceDao extends CrudDao<OtherDataResourceEntity, String, SqlBuilder.PSC, OtherDataResourceDao> {
}
