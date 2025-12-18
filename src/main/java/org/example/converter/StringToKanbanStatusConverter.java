package org.example.converter;

import org.example.enums.KanbanStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToKanbanStatusConverter implements Converter<String, KanbanStatus> {

  @Override
  public KanbanStatus convert(String source) {
    return KanbanStatus.fromString(source);
  }
}
