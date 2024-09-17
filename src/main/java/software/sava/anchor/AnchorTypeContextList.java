package software.sava.anchor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public record AnchorTypeContextList(List<AnchorNamedType> fields) implements AnchorDefinedTypeContext {

  public static AnchorTypeContextList createList(final List<AnchorNamedType> fields) {
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
      final var distinctFields = new ArrayList<AnchorNamedType>(fields.size());
      fields.stream().limit(i).forEach(distinctFields::add);

      var field = fields.get(i);
      distinctFields.add(field.rename(field.name() + i));

      for (; ; ) {
        if (++i == fields.size()) {
          break;
        }
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
