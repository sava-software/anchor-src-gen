package software.sava.idl.generator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public record AnchorTypeContextList(List<NamedType> fields) implements AnchorDefinedTypeContext {

  public static AnchorTypeContextList createList(final List<NamedType> fields) {
    final var fieldNames = HashSet.<String>newHashSet(fields.size());
    int i = 0;
    for (final var field : fields) {
      if (!fieldNames.add(field.name())) {
        break;
      }
      ++i;
    }

    if (i == fields.size()) {
      return new AnchorTypeContextList(fields);
    } else {
      final var distinctFields = new ArrayList<NamedType>(fields.size());
      fields.stream().limit(i).forEach(distinctFields::add);

      var field = fields.get(i);
      distinctFields.add(field.rename(field.name() + i));

      while (++i != fields.size()) {
        field = fields.get(i);
        if (!fieldNames.add(field.name())) {
          distinctFields.add(field.rename(field.name() + i));
        } else {
          distinctFields.add(field);
        }
      }
      return new AnchorTypeContextList(distinctFields);
    }
  }

  @Override
  public AnchorType type() {
    return null;
  }
}
