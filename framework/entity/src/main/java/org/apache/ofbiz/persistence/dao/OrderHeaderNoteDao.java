package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.OrderHeaderNoteEntity;

public interface OrderHeaderNoteDao extends CrudDao<OrderHeaderNoteEntity, OrderHeaderNoteEntity, SqlBuilder.PSC, OrderHeaderNoteDao> {
}
