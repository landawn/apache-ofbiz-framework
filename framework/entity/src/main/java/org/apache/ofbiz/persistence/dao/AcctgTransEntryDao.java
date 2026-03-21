package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.AcctgTransEntryEntity;

public interface AcctgTransEntryDao extends CrudDao<AcctgTransEntryEntity, AcctgTransEntryEntity, SqlBuilder.PSC, AcctgTransEntryDao> {
}
