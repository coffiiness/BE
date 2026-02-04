package com.coffiness.calfit.storage.db.core.template;

import com.coffiness.calfit.core.enums.TemplateType;
import com.coffiness.calfit.storage.db.core.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "templates")
public class TemplateEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TemplateType type;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "schema", columnDefinition = "JSON", nullable = false)
    private String schema;

    @Column(name = "is_default")
    private Boolean isDefault = false;

}
