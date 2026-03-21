package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.GlJournalEntity;

public interface GlJournalDao extends CrudDao<GlJournalEntity, String, SqlBuilder.PSC, GlJournalDao> {
}
