package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartyIdentificationTypeEntity;

public interface PartyIdentificationTypeDao extends CrudDao<PartyIdentificationTypeEntity, String, SQLBuilder.PSC, PartyIdentificationTypeDao> {
}
