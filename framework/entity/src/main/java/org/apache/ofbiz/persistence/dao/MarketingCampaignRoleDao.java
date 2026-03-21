package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SqlBuilder;
import org.apache.ofbiz.persistence.entity.MarketingCampaignRoleEntity;

public interface MarketingCampaignRoleDao extends CrudDao<MarketingCampaignRoleEntity, MarketingCampaignRoleEntity, SqlBuilder.PSC, MarketingCampaignRoleDao> {
}
