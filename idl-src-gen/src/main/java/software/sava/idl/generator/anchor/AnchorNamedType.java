package software.sava.idl.generator.anchor;

import software.sava.core.programs.Discriminator;
import software.sava.idl.generator.ParseUtil;
import software.sava.idl.generator.src.BaseNamedType;
import software.sava.idl.generator.src.NamedType;
import software.sava.idl.generator.src.SrcUtil;
import software.sava.idl.generator.src.TypeContext;

import java.util.List;
import java.util.Objects;

final class AnchorNamedType extends BaseNamedType<TypeContext> {

  private final Discriminator discriminator;
  private final AnchorSerialization serialization;
  private final AnchorRepresentation representation;

  AnchorNamedType(final Discriminator discriminator,
                  final String name,
                  final AnchorSerialization serialization,
                  final AnchorRepresentation representation,
                  final TypeContext type,
                  final List<String> docs,
                  final String docComments) {
    super(name, type, docs, docComments);
    this.discriminator = discriminator;
    this.serialization = serialization;
    this.representation = representation;
  }

  static NamedType createType(final Discriminator discriminator,
                              final String name,
                              final AnchorSerialization serialization,
                              final AnchorRepresentation representation,
                              final AnchorTypeContext type,
                              final List<String> docs) {
    final String checkedName;
    if (name == null) {
      checkedName = '_' + type.type().name();
    } else {
      checkedName = ParseUtil.checkTypeName(name);
    }
    return new AnchorNamedType(
        discriminator,
        checkedName,
        serialization == null ? AnchorSerialization.borsh : serialization,
        representation,
        type,
        docs == null ? IDL.NO_DOCS : docs,
        docs == null ? "" : SrcUtil.formatComments(docs)
    );
  }

  public static NamedType createType(final Discriminator discriminator,
                                     final String name,
                                     final AnchorTypeContext type) {
    return createType(discriminator, name, null, null, type, IDL.NO_DOCS);
  }

  @Override
  public Discriminator discriminator() {
    return discriminator;
  }

  @Override
  public NamedType rename(final String newName) {
    return new AnchorNamedType(
        discriminator,
        newName,
        serialization,
        representation,
        type,
        docs,
        docComments
    );
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof final AnchorNamedType that)) return false;
    if (!super.equals(o)) return false;
    return Objects.equals(discriminator, that.discriminator)
        && serialization == that.serialization
        && Objects.equals(representation, that.representation);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + Objects.hashCode(discriminator);
    result = 31 * result + serialization.hashCode();
    result = 31 * result + Objects.hashCode(representation);
    return result;
  }

  @Override
  public String toString() {
    return "AnchorNamedType{" +
        "discriminator=" + discriminator +
        ", serialization=" + serialization +
        ", representation=" + representation +
        ", name='" + name + '\'' +
        ", type=" + type +
        ", docs=" + docs +
        ", docComments='" + docComments + '\'' +
        '}';
  }
}
