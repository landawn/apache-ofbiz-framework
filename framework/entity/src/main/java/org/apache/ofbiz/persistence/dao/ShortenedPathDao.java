package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.ShortenedPathEntity;

public interface ShortenedPathDao extends CrudDao<ShortenedPathEntity, String, SQLBuilder.PSC, ShortenedPathDao> {
}
