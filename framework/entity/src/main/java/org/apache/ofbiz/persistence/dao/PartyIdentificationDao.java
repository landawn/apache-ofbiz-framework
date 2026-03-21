package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyIdentificationEntity;

public interface PartyIdentificationDao extends CrudDao<PartyIdentificationEntity, PartyIdentificationEntity, SqlBuilder.PSC, PartyIdentificationDao> , DelegatorQueryDao{
}
