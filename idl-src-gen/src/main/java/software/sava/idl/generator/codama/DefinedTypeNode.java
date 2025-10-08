package software.sava.idl.generator.codama;

import software.sava.core.programs.Discriminator;
import software.sava.idl.generator.src.BaseNamedType;
import software.sava.idl.generator.src.NamedType;
import software.sava.idl.generator.src.SrcUtil;
import systems.comodal.jsoniter.JsonIterator;

import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

sealed class DefinedTypeNode extends BaseNamedType<TypeNode> implements StructNode permits AccountNode {

  DefinedTypeNode(final String name,
                  final TypeNode type,
                  final List<String> docs,
                  final String docComments) {
    super(name, type, docs, docComments);
  }

  @Override
  public Discriminator discriminator() {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public NamedType rename(final String newName) {
    return new DefinedTypeNode(newName, type, docs, docComments);
  }

  static DefinedTypeNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createDefinedTypeNode();
  }

  private static final class Parser extends BaseDocsParser {

    private TypeNode type;

    private Parser() {
    }

    DefinedTypeNode createDefinedTypeNode() {
      return new DefinedTypeNode(
          name,
          type,
          docs == null ? List.of() : docs,
          docs == null ? "" : SrcUtil.formatComments(docs)
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("type", buf, offset, len)) {
        type = TypeNode.parse(ji);
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }
}
