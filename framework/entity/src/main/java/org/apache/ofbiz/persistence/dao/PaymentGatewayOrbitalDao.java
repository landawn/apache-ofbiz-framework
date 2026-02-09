package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PaymentGatewayOrbitalEntity;

public interface PaymentGatewayOrbitalDao extends CrudDao<PaymentGatewayOrbitalEntity, String, SQLBuilder.PSC, PaymentGatewayOrbitalDao> {
}
