package software.sava.idl.generator.codama;

import software.sava.core.programs.Discriminator;
import software.sava.idl.generator.anchor.SrcGenContext;
import software.sava.idl.generator.src.BaseStruct;
import software.sava.idl.generator.src.NamedType;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class StructTypeNode extends BaseStruct<StructFieldTypeNode> implements TypeNode {

  StructTypeNode(final List<StructFieldTypeNode> fields) {
    super(fields);
  }

  public String generateRecord(final SrcGenContext srcGenContext,
                               final NamedType namedType,
                               final boolean publicAccess,
                               final String interfaceName) {
    final boolean isAccount = namedType instanceof AccountNode;
    return generateRecord(
        srcGenContext,
        namedType,
        publicAccess,
        interfaceName,
        -1,
        isAccount,
        namedType,
        isAccount
    );
  }

  @Override
  protected Discriminator appendAccountDiscriminator(final SrcGenContext srcGenContext,
                                                     final StringBuilder builder,
                                                     final NamedType account) {
    if (account instanceof AccountNode accountNode) {
      Discriminator accountDiscriminator = null;
      for (final var discriminator : accountNode.discriminators()) {
        if (discriminator instanceof ConstantDiscriminatorNode(final ValueNode.Constant constant, final int offset)) {

          if (constant.val() instanceof final ValueNode.Bytes bytes) {
            accountDiscriminator = bytes.createDiscriminator();
          } else {
            throw new UnsupportedOperationException("Add support for discriminator constant " + constant.val());
          }
          appendAccountDiscriminator(srcGenContext, builder, accountDiscriminator, offset);
        } else {
          throw new UnsupportedOperationException("Add support for account " + discriminator);
        }
      }
      return accountDiscriminator;
    } else {
      throw new IllegalStateException("Account node expected, not: " + account);
    }
  }

  @Override
  protected boolean hasStringFields() {
    return fields.stream().anyMatch(StructFieldTypeNode::isString);
  }

  static StructTypeNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createTypeNode();
  }

  private static final class Parser implements FieldBufferPredicate {

    private List<StructFieldTypeNode> fields;

    StructTypeNode createTypeNode() {
      return new StructTypeNode(fields);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("fields", buf, offset, len)) {
        fields = new ArrayList<>();
        while (ji.readArray()) {
          fields.add(StructFieldTypeNode.parse(ji));
        }
      } else {
        throw new IllegalStateException(String.format(
            "Unhandled %s field %s.",
            getClass().getSimpleName(), new String(buf, offset, len)
        ));
      }
      return true;
    }
  }
}
