package com.coffiness.calfit.domain.template;

import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationTemplateReader {

  List<ApplicationTemplate> readAll();

  ApplicationTemplate read(Long templateId);
}
