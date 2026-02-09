package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.PartySkillEntity;

public interface PartySkillDao extends CrudDao<PartySkillEntity, PartySkillEntity, SQLBuilder.PSC, PartySkillDao> {
}
