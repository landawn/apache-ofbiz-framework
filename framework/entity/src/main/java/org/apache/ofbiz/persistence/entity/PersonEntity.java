package org.apache.ofbiz.persistence.entity;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.Table;
import java.math.BigDecimal;
import java.sql.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "PERSON")
@Table(name = "PERSON")
public class PersonEntity {
    @Id
    @Column(name = "PARTY_ID")
    private String partyId;

    @Column(name = "SALUTATION")
    private String salutation;

    @Column(name = "FIRST_NAME")
    private String firstName;

    @Column(name = "MIDDLE_NAME")
    private String middleName;

    @Column(name = "LAST_NAME")
    private String lastName;

    @Column(name = "PERSONAL_TITLE")
    private String personalTitle;

    @Column(name = "SUFFIX")
    private String suffix;

    @Column(name = "NICKNAME")
    private String nickname;

    @Column(name = "FIRST_NAME_LOCAL")
    private String firstNameLocal;

    @Column(name = "MIDDLE_NAME_LOCAL")
    private String middleNameLocal;

    @Column(name = "LAST_NAME_LOCAL")
    private String lastNameLocal;

    @Column(name = "OTHER_LOCAL")
    private String otherLocal;

    @Column(name = "MEMBER_ID")
    private String memberId;

    @Column(name = "GENDER")
    private String gender;

    @Column(name = "BIRTH_DATE")
    private Date birthDate;

    @Column(name = "DECEASED_DATE")
    private Date deceasedDate;

    @Column(name = "HEIGHT")
    private Double height;

    @Column(name = "WEIGHT")
    private Double weight;

    @Column(name = "MOTHERS_MAIDEN_NAME")
    private String mothersMaidenName;

    @Column(name = "marital_satus_enum_id")
    private String maritalSatusEnumId;

    @Column(name = "MARITAL_STATUS_TYPE_ID")
    private String maritalStatusTypeId;

    @Column(name = "SOCIAL_SECURITY_NUMBER")
    private String socialSecurityNumber;

    @Column(name = "PASSPORT_NUMBER")
    private String passportNumber;

    @Column(name = "PASSPORT_EXPIRE_DATE")
    private Date passportExpireDate;

    @Column(name = "TOTAL_YEARS_WORK_EXPERIENCE")
    private Double totalYearsWorkExperience;

    @Column(name = "COMMENTS")
    private String comments;

    @Column(name = "EMPLOYMENT_STATUS_ENUM_ID")
    private String employmentStatusEnumId;

    @Column(name = "RESIDENCE_STATUS_ENUM_ID")
    private String residenceStatusEnumId;

    @Column(name = "OCCUPATION")
    private String occupation;

    @Column(name = "YEARS_WITH_EMPLOYER")
    private BigDecimal yearsWithEmployer;

    @Column(name = "MONTHS_WITH_EMPLOYER")
    private BigDecimal monthsWithEmployer;

    @Column(name = "EXISTING_CUSTOMER")
    private String existingCustomer;

    @Column(name = "CARD_ID")
    private String cardId;
}
