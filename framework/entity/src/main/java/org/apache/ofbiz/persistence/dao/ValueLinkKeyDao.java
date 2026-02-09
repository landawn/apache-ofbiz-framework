package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ValueLinkKeyEntity;

public interface ValueLinkKeyDao extends CrudDao<ValueLinkKeyEntity, String, SQLBuilder.PSC, ValueLinkKeyDao> {
}
