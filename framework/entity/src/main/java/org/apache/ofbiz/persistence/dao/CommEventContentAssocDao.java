package org.apache.ofbiz.persistence.dao;

import com.landawn.abacus.jdbc.dao.CrudDao;
import com.landawn.abacus.query.SQLBuilder;
import org.apache.ofbiz.persistence.entity.CommEventContentAssocEntity;

public interface CommEventContentAssocDao extends CrudDao<CommEventContentAssocEntity, CommEventContentAssocEntity, SQLBuilder.PSC, CommEventContentAssocDao> {
}
