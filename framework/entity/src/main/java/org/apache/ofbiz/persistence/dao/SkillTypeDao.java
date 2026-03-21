package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.SkillTypeEntity;

public interface SkillTypeDao extends CrudDao<SkillTypeEntity, String, SqlBuilder.PSC, SkillTypeDao> {
}
