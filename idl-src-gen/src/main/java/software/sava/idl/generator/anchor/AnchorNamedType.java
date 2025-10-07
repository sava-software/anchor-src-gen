package software.sava.idl.generator.anchor;

import software.sava.core.programs.Discriminator;
import software.sava.idl.generator.src.SrcUtil;

import java.util.List;
import java.util.Objects;

final class AnchorNamedType extends BaseNamedType {

  private final Discriminator discriminator;
  private final AnchorSerialization serialization;
  private final AnchorRepresentation representation;

  AnchorNamedType(final Discriminator discriminator,
                  final String name,
                  final AnchorSerialization serialization,
                  final AnchorRepresentation representation,
                  final TypeContext type,
                  final List<String> docs,
                  final String docComments,
                  final boolean index) {
    super(name, type, docs, docComments, index);
    this.discriminator = discriminator;
    this.serialization = serialization;
    this.representation = representation;
  }

  static NamedType createType(final Discriminator discriminator,
                              final String name,
                              final AnchorSerialization serialization,
                              final AnchorRepresentation representation,
                              final AnchorTypeContext type,
                              final List<String> docs,
                              final boolean index) {
    if (name == null) {
      return new AnchorNamedType(
          discriminator,
          '_' + type.type().name(),
          serialization == null ? AnchorSerialization.borsh : serialization,
          representation,
          type,
          docs == null ? IDL.NO_DOCS : docs,
          docs == null ? "" : SrcUtil.formatComments(docs),
          index
      );
    } else {
      final String cleanedName;
      if (RESERVED_NAMES.contains(name)) {
        cleanedName = '_' + name;
      } else {
        cleanedName = NamedType.cleanName(name);
      }
      return new AnchorNamedType(
          discriminator,
          cleanedName,
          serialization == null ? AnchorSerialization.borsh : serialization,
          representation,
          type,
          docs == null ? IDL.NO_DOCS : docs,
          docs == null ? "" : SrcUtil.formatComments(docs),
          index
      );
    }
  }

  public static NamedType createType(final Discriminator discriminator,
                                     final String name,
                                     final AnchorTypeContext type) {
    return createType(discriminator, name, null, null, type, IDL.NO_DOCS, false);
  }

  public AnchorSerialization serialization() {
    return serialization;
  }

  public AnchorRepresentation representation() {
    return representation;
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
        docComments,
        index
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
}
