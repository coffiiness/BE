package com.coffiness.calfit.storage.db.core.applicant;

import com.coffiness.calfit.core.enums.UserRole;
import com.coffiness.calfit.storage.db.core.TenancyEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "applicants") // 지원자
@NoArgsConstructor
@Getter
public class ApplicantEntity extends TenancyEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

}
