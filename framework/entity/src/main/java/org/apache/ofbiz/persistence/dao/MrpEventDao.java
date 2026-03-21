package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.MrpEventEntity;

public interface MrpEventDao extends CrudDao<MrpEventEntity, MrpEventEntity, SqlBuilder.PSC, MrpEventDao> {
}
