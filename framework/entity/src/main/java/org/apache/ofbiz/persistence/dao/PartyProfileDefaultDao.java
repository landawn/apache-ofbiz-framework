package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.PartyProfileDefaultEntity;

public interface PartyProfileDefaultDao extends CrudDao<PartyProfileDefaultEntity, PartyProfileDefaultEntity, SqlBuilder.PSC, PartyProfileDefaultDao> {
}
