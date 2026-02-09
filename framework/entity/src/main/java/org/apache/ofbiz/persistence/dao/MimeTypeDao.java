package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.MimeTypeEntity;

public interface MimeTypeDao extends CrudDao<MimeTypeEntity, String, SQLBuilder.PSC, MimeTypeDao> {
}
