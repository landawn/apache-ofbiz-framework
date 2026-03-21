package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.TelecomGatewayConfigEntity;

public interface TelecomGatewayConfigDao extends CrudDao<TelecomGatewayConfigEntity, String, SqlBuilder.PSC, TelecomGatewayConfigDao> {
}
