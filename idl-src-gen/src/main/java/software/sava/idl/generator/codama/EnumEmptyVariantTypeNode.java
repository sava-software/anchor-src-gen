package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class EnumEmptyVariantTypeNode extends OrdinalNode implements TypeNode, EnumVariantTypeNode {

  EnumEmptyVariantTypeNode(final String name, final int discriminator) {
    super(name, discriminator);
  }

  static EnumEmptyVariantTypeNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createEnumEmptyVariantTypeNode();
  }

  private static final class Parser extends BaseParser {

    private int discriminator;

    EnumEmptyVariantTypeNode createEnumEmptyVariantTypeNode() {
      return new EnumEmptyVariantTypeNode(name, discriminator);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("discriminator", buf, offset, len)) {
        discriminator = ji.readInt();
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }
}
