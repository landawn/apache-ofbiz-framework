package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartyAcctgPreferenceEntity;

public interface PartyAcctgPreferenceDao extends CrudDao<PartyAcctgPreferenceEntity, String, SQLBuilder.PSC, PartyAcctgPreferenceDao> {
}
