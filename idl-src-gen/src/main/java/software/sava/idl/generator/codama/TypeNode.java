package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import java.util.function.Function;

public sealed interface TypeNode permits AmountTypeNode,
    ArrayTypeNode,
    BooleanTypeNode,
    BytesTypeNode,
    CountNode,
    DateTimeTypeNode,
    EnumEmptyVariantTypeNode,
    EnumStructVariantTypeNode,
    EnumTupleVariantTypeNode,
    EnumTypeNode,
    MapTypeNode,
    NestedTypeNode,
    NumberTypeNode,
    OptionTypeNode,
    PublicKeyTypeNode,
    RemainderOptionTypeNode,
    SetTypeNode,
    SolAmountTypeNode,
    StringTypeNode,
    StructTypeNode,
    TupleTypeNode,
    ZeroableOptionTypeNode {

  static TypeNode parse(final JsonIterator ji, final Function<JsonIterator, TypeNode> parser) {
    // final int mark = ji.mark();
    // final var kind = ji.skipUntil("kind").readString();
    // ji.reset(mark);
    final var kind = ji.skipObjField().readString();
    return switch (kind) {
      case "numberTypeNode" -> NumberTypeNode.parse(ji);
      case "amountTypeNode" -> AmountTypeNode.parse(ji);
      case "arrayTypeNode" -> ArrayTypeNode.parse(ji);
      case "booleanTypeNode" -> BooleanTypeNode.parse(ji);
      case "bytesTypeNode" -> BytesTypeNode.parse(ji);
      case "dateTimeTypeNode" -> DateTimeTypeNode.parse(ji);
      case "enumEmptyVariantTypeNode" -> EnumEmptyVariantTypeNode.parse(ji);
      case "enumStructVariantTypeNode" -> EnumStructVariantTypeNode.parse(ji);
      case "enumTupleVariantTypeNode" -> EnumTupleVariantTypeNode.parse(ji);
      case "enumTypeNode" -> EnumTypeNode.parse(ji);
      case "mapTypeNode" -> MapTypeNode.parse(ji);
      case "optionTypeNode" -> OptionTypeNode.parse(ji);
      case "publicKeyTypeNode" -> PublicKeyTypeNode.parse(ji);
      case "remainderOptionTypeNode" -> RemainderOptionTypeNode.parse(ji);
      case "solAmountTypeNode" -> SolAmountTypeNode.parse(ji);
      case "stringTypeNode" -> StringTypeNode.parse(ji);
      case "structTypeNode" -> StructTypeNode.parse(ji);
      case "tupleTypeNode" -> TupleTypeNode.parse(ji);
      case "zeroableOptionTypeNode" -> ZeroableOptionTypeNode.parse(ji);
      default -> parseNestedTypeNode(ji, parser, kind);
    };
  }

  static NestedTypeNode parseNestedTypeNode(final JsonIterator ji,
                                            final Function<JsonIterator, TypeNode> parser,
                                            final String kind) {

    return switch (kind) {
      case "fixedSizeTypeNode" -> FixedSizeTypeNode.parse(ji, parser);
      case "hiddenPrefixTypeNode" -> HiddenPrefixTypeNode.parse(ji, parser);
      case "hiddenSuffixTypeNode" -> HiddenSuffixTypeNode.parse(ji, parser);
      case "postOffsetTypeNode" -> PostOffsetTypeNode.parse(ji, parser);
      case "preOffsetTypeNode" -> PreOffsetTypeNode.parse(ji, parser);
      case "sentinelTypeNode" -> SentinelTypeNode.parse(ji, parser);
      case "sizePrefixTypeNode" -> SizePrefixTypeNode.parse(ji, parser);
      default -> throw new UnsupportedOperationException(kind);
    };
  }

  static NestedTypeNode parseNestedTypeNode(final JsonIterator ji, final Function<JsonIterator, TypeNode> parser) {
    final var kind = ji.skipObjField().readString();
    return parseNestedTypeNode(ji, parser, kind);
  }

  static TypeNode parse(final JsonIterator ji) {
    return parse(ji, null);
  }
}
