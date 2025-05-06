package software.sava.anchor;

import software.sava.core.borsh.RustEnum;
import software.sava.core.rpc.Filter;
import systems.comodal.jsoniter.JsonIterator;

import java.util.List;
import java.util.Map;

import static software.sava.anchor.AnchorNamedTypeParser.parseUpperList;
import static software.sava.anchor.AnchorSourceGenerator.removeBlankLines;
import static software.sava.anchor.AnchorStruct.generateRecord;

public record AnchorEnum(List<AnchorNamedType> values) implements AnchorDefinedTypeContext {

  static AnchorEnum parseEnum(final IDLType idlType, final JsonIterator ji) {
    return new AnchorEnum(parseUpperList(idlType, ji));
  }

  @Override
  public AnchorType type() {
    return AnchorType._enum;
  }

  @Override
  public int numElements() {
    return values.size();
  }

  @Override
  public boolean isFixedLength(final Map<String, AnchorNamedType> definedTypes) {
    return values.stream().noneMatch(t -> t.type() != null);
  }

  @Override
  public int serializedLength(final GenSrcContext genSrcContext) {
    return type().dataLength();
  }

  @Override
  public void generateMemCompFilter(final GenSrcContext genSrcContext,
                                    final StringBuilder builder,
                                    final String varName,
                                    final String offsetVarName,
                                    final boolean optional) {
    final var serializeCode = String.format("return Filter.createMemCompFilter(%s, new byte[]{(byte) %s.ordinal()});", offsetVarName, varName);
    builder.append(String.format("""
            
            public static Filter create%sFilter(final %s %s) {
            %s}
            """,
        AnchorUtil.camelCase(varName, true), typeName(), varName, serializeCode.indent(genSrcContext.tabLength())
    ));
    genSrcContext.addImport(Filter.class);
  }

  private String generateSimpleEnum(final GenSrcContext genSrcContext,
                                    final AnchorNamedType context,
                                    final StringBuilder builder) {
    final var tab = genSrcContext.tab();
    final int tabLength = genSrcContext.tabLength();
    final var name = context.name();
    builder.append(String.format("""
            import software.sava.core.borsh.Borsh;
            
            %spublic enum %s implements Borsh.Enum {
            
            """,
        context.docComments(), name
    ));
    final var iterator = values.iterator();
    for (AnchorNamedType next; ; ) {
      next = iterator.next();
      builder.append(next.docComments().indent(tabLength).stripTrailing());
      builder.append(tab).append(next.name());
      if (iterator.hasNext()) {
        builder.append(",\n");
      } else {
        builder.append(";\n\n").append(String.format("""
            public static %s read(final byte[] _data, final int offset) {""", name
        ).indent(tabLength));
        builder.append(tab).append(tab).append(String.format("return Borsh.read(%s.values(), _data, offset);", name));
        builder.append('\n').append(tab);
        return builder.append("}\n}").toString();
      }
    }
  }

  private boolean wrappedType(final Map<String, AnchorNamedType> definedTypes, final AnchorNamedType enumEntry) {
    final var typeName = enumEntry.name();
    final var type = enumEntry.type();
    if (type instanceof AnchorTypeContextList(final List<AnchorNamedType> fields)) {
      if (fields.size() == 1) {
        final var field = fields.getFirst();
        if (field.type() instanceof AnchorDefined(String _typeName) && _typeName.equals(typeName)) {
          return definedTypes.get(typeName).type() instanceof AnchorStruct;
        }
      }
    }
    return false;
  }

  public String generateSource(final GenSrcContext genSrcContext, final AnchorNamedType context) {
    return generateSource(genSrcContext, context, false);
  }

  public String generateSource(final GenSrcContext genSrcContext,
                               final AnchorNamedType context,
                               final boolean isAccount) {
    final var header = new StringBuilder(2_048);
    header.append("package ").append(genSrcContext.typePackage()).append(";\n\n");

    final var name = context.name();
    if (values.stream().noneMatch(t -> t.type() != null)) {
      return generateSimpleEnum(genSrcContext, context, header);
    } else {
      final var tab = genSrcContext.tab();
      final int tabLength = genSrcContext.tabLength();
      genSrcContext.addImport(RustEnum.class);
      final var builder = new StringBuilder(2_048);
      builder.append('\n');
      builder.append(context.docComments());
      builder.append(String.format("public sealed interface %s extends RustEnum permits\n", name));

      final var iterator = values.iterator();
      for (AnchorNamedType next; ; ) {
        next = iterator.next();
        builder.append(next.docComments().indent(tabLength).stripTrailing());
        builder.append(tab).append(name).append('.').append(next.name());
        if (iterator.hasNext()) {
          builder.append(",\n");
        } else {
          builder.append(" {\n");
          break;
        }
      }

      builder.append(String.format("""
              
              static %s read(final byte[] _data, final int offset) {
              %sfinal int ordinal = _data[offset] & 0xFF;
              %sfinal int i = offset + 1;
              %sreturn switch (ordinal) {""",
          name, tab, tab, tab
      ).indent(tabLength));

      int ordinal = 0;
      for (final var entry : values) {
        final var type = entry.type();
        builder.append(tab).append(tab).append(tab).append(String.format("case %d -> ", ordinal++));
        if (type == null) {
          builder.append(String.format("%s.INSTANCE", entry.name()));
        } else {
          builder.append(String.format("%s.read(_data, i)", entry.name()));
        }
        builder.append(";\n");
      }
      builder.append(tab).append(tab).append(tab).append("default -> throw new IllegalStateException(java.lang.String.format(\n");
      builder.append(tab).append(tab).append(tab).append(tab).append(tab).append(String.format("""
          "Unexpected ordinal [%%d] for enum [%s]", ordinal
          """, name
      ));
      builder.append(tab).append(tab).append(tab).append("));\n");
      builder.append(tab).append(tab).append("};\n").append(tab).append("}\n");

      ordinal = 0;
      for (final var entry : values) {
        final var type = entry.type();
        if (type == null) {
          builder.append('\n').append(String.format("""
              record %s() implements EnumNone, %s {""", entry.name(), name
          ).indent(tabLength));
          builder.append(String.format("""
                  
                  public static final %s INSTANCE = new %s();
                  
                  @Override
                  public int ordinal() {
                  %sreturn %d;
                  }""",
              entry.name(), entry.name(), tab, ordinal
          ).indent(tabLength << 1));
          builder.append(tab).append('}').append('\n');
        } else if (type instanceof AnchorTypeContextList(final List<AnchorNamedType> fields)) {
          builder.append('\n');
          if (fields.size() == 1) {
            final var field = fields.getFirst();
            builder.append(field.type().generateEnumRecord(genSrcContext, name, entry, ordinal).indent(tabLength));
          } else {
            final var recordSrc = generateRecord(genSrcContext, entry, fields, "", name, ordinal, isAccount, entry, isAccount);
            builder.append(recordSrc.indent(tabLength));
          }
        } else {
          throw new IllegalStateException("Expected AnchorTypeContextList, not: " + type);
        }
        ++ordinal;
      }

      genSrcContext.appendImports(header);

      final var sourceCode = header.append(builder).append('}').toString();
      return removeBlankLines(sourceCode);
    }
  }
}
