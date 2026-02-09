package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.GlJournalEntity;

public interface GlJournalDao extends CrudDao<GlJournalEntity, String, SQLBuilder.PSC, GlJournalDao> {
}
