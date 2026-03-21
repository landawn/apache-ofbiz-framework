package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyAcctgPreferenceEntity;

public interface PartyAcctgPreferenceDao extends CrudDao<PartyAcctgPreferenceEntity, String, SqlBuilder.PSC, PartyAcctgPreferenceDao> {
}
