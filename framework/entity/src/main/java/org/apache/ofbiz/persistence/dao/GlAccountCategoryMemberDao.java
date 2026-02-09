package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.GlAccountCategoryMemberEntity;

public interface GlAccountCategoryMemberDao extends CrudDao<GlAccountCategoryMemberEntity, GlAccountCategoryMemberEntity, SQLBuilder.PSC, GlAccountCategoryMemberDao> {
}
