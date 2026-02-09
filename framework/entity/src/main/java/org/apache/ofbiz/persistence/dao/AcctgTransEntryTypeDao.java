package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.AcctgTransEntryTypeEntity;

public interface AcctgTransEntryTypeDao extends CrudDao<AcctgTransEntryTypeEntity, String, SQLBuilder.PSC, AcctgTransEntryTypeDao> {
}
