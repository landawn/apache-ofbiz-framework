package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.OtherDataResourceEntity;

public interface OtherDataResourceDao extends CrudDao<OtherDataResourceEntity, String, SQLBuilder.PSC, OtherDataResourceDao> {
}
