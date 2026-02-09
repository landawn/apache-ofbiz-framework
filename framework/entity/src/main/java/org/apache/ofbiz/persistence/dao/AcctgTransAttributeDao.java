package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.AcctgTransAttributeEntity;

public interface AcctgTransAttributeDao extends CrudDao<AcctgTransAttributeEntity, AcctgTransAttributeEntity, SQLBuilder.PSC, AcctgTransAttributeDao> {
}
