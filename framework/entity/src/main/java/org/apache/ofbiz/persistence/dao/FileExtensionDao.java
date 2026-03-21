package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.FileExtensionEntity;

public interface FileExtensionDao extends CrudDao<FileExtensionEntity, String, SqlBuilder.PSC, FileExtensionDao> {
}
