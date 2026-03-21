package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.ReturnItemShipmentEntity;

public interface ReturnItemShipmentDao extends CrudDao<ReturnItemShipmentEntity, ReturnItemShipmentEntity, SqlBuilder.PSC, ReturnItemShipmentDao> {
}
