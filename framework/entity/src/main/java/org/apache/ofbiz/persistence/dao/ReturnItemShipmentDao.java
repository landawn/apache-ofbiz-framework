package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ReturnItemShipmentEntity;

public interface ReturnItemShipmentDao extends CrudDao<ReturnItemShipmentEntity, ReturnItemShipmentEntity, SQLBuilder.PSC, ReturnItemShipmentDao> {
}
