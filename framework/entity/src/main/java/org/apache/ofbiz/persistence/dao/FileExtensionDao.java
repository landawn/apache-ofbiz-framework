package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.FileExtensionEntity;

public interface FileExtensionDao extends CrudDao<FileExtensionEntity, String, SQLBuilder.PSC, FileExtensionDao> {
}
