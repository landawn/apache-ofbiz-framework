package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CharacterSetEntity;

public interface CharacterSetDao extends CrudDao<CharacterSetEntity, String, SQLBuilder.PSC, CharacterSetDao> {
}
