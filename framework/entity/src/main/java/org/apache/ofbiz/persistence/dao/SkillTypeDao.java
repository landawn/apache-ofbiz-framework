package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.SkillTypeEntity;

public interface SkillTypeDao extends CrudDao<SkillTypeEntity, String, SQLBuilder.PSC, SkillTypeDao> {
}
