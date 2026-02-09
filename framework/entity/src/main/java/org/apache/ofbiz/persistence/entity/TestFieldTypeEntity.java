package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "TEST_FIELD_TYPE")
@Table(name = "TEST_FIELD_TYPE")
public class TestFieldTypeEntity {
    @Id
    @Column(name = "TEST_FIELD_TYPE_ID")
    private String testFieldTypeId;

    @Column(name = "BLOB_FIELD")
    private byte[] blobField;

    @Column(name = "BYTE_ARRAY_FIELD")
    private byte[] byteArrayField;

    @Column(name = "OBJECT_FIELD")
    private byte[] objectField;

    @Column(name = "DATE_FIELD")
    private Date dateField;

    @Column(name = "TIME_FIELD")
    private Time timeField;

    @Column(name = "DATE_TIME_FIELD")
    private Timestamp dateTimeField;

    @Column(name = "FIXED_POINT_FIELD")
    private BigDecimal fixedPointField;

    @Column(name = "FLOATING_POINT_FIELD")
    private Double floatingPointField;

    @Column(name = "NUMERIC_FIELD")
    private BigDecimal numericField;

    @Column(name = "CLOB_FIELD")
    private String clobField;
}
