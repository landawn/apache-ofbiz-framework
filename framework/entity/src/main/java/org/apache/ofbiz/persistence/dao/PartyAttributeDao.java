package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyAttributeEntity;

public interface PartyAttributeDao extends CrudDao<PartyAttributeEntity, PartyAttributeEntity, SqlBuilder.PSC, PartyAttributeDao> , DelegatorQueryDao{
}
