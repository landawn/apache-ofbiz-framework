package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.MimeTypeHtmlTemplateEntity;

public interface MimeTypeHtmlTemplateDao extends CrudDao<MimeTypeHtmlTemplateEntity, String, SqlBuilder.PSC, MimeTypeHtmlTemplateDao> {
}
