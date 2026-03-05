package com.coffiness.calfit.domain.template;

import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationTemplateStore {

  ApplicationTemplate store(ApplicationTemplate template);

  ApplicationTemplate update(ApplicationTemplate template);

  void delete(Long templateId);
}
