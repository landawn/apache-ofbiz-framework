package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.TelecomGatewayConfigEntity;

public interface TelecomGatewayConfigDao extends CrudDao<TelecomGatewayConfigEntity, String, SQLBuilder.PSC, TelecomGatewayConfigDao> {
}
