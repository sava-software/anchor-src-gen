package software.sava.anchor;

import software.sava.core.programs.Discriminator;
import systems.comodal.jsoniter.JsonIterator;
import systems.comodal.jsoniter.ValueType;
import systems.comodal.jsoniter.factory.ElementFactory;

import java.util.List;

import static software.sava.anchor.AnchorType.ANCHOR_TYPE_PARSER;
import static software.sava.anchor.AnchorUtil.parseDocs;
import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class AnchorNamedTypeParser extends BaseNamedTypeParser {

  private final IDLType idlType;
  private Discriminator discriminator;
  private AnchorRepresentation representation;
  private boolean index;

  AnchorNamedTypeParser(final IDLType idlType, final boolean firstUpper) {
    super(firstUpper);
    this.idlType = idlType;
  }

  static List<NamedType> parseLowerList(final IDLType idlType, final JsonIterator ji) {
    ji.skipObjField();
    return ElementFactory.parseList(ji, idlType.lowerTypeParserFactory());
  }

  static List<NamedType> parseUpperList(final IDLType idlType, final JsonIterator ji) {
    ji.skipObjField();
    return ElementFactory.parseList(ji, idlType.upperTypeParserFactory());
  }

  private static AnchorTypeContext parseTypeContext(final IDLType idlType, final JsonIterator ji) {
    final var jsonType = ji.whatIsNext();
    if (jsonType == ValueType.STRING) {
      return ji.applyChars(ANCHOR_TYPE_PARSER).primitiveType();
    } else if (jsonType == ValueType.OBJECT) {
      final var type = AnchorType.parseContextType(idlType, ji);
      ji.closeObj();
      return type;
    } else {
      throw new IllegalStateException(String.format("TODO: Support %s Anchor types", jsonType));
    }
  }

  @Override
  public NamedType create() {
    return NamedType.createType(discriminator, name, serialization, representation, type, docs, index);
  }

  @Override
  public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
    if (fieldEquals("discriminator", buf, offset, len)) {
      this.discriminator = AnchorUtil.parseDiscriminator(ji);
    } else if (fieldEquals("docs", buf, offset, len)) {
      this.docs = parseDocs(ji);
    } else if (fieldEquals("fields", buf, offset, len)) {
      this.type = AnchorTypeContextList.createList(ElementFactory.parseList(ji, idlType.lowerTypeParserFactory(), this));
    } else if (fieldEquals("index", buf, offset, len)) {
      this.index = ji.readBoolean();
    } else if (fieldEquals("name", buf, offset, len)) {
      this.name = NamedTypeParser.cleanName(ji.readString(), firstUpper);
      // System.out.println(name);
    } else if (fieldEquals("option", buf, offset, len)) {
      this.type = new AnchorOption(parseTypeContext(idlType, ji));
    } else if (fieldEquals("type", buf, offset, len)) {
      this.type = parseTypeContext(idlType, ji);
    } else if (fieldEquals("array", buf, offset, len)) {
      this.type = AnchorArray.parseArray(idlType, ji);
    } else if (fieldEquals("defined", buf, offset, len)) {
      this.type = AnchorDefined.parseDefined(idlType, ji);
    } else if (fieldEquals("serialization", buf, offset, len)) {
      this.serialization = AnchorSerialization.valueOf(ji.readString());
    } else if (fieldEquals("repr", buf, offset, len)) {
      this.representation = AnchorTransparentRepresentation.parseRepresentation(ji);
    } else {
      throw new IllegalStateException("Unhandled defined type field: " + new String(buf, offset, len));
    }
    return true;
  }

  @Override
  public NamedType apply(final char[] chars, final int offset, final int len) {
    final var primitiveType = ANCHOR_TYPE_PARSER.apply(chars, offset, len).primitiveType();
    return NamedType.createType(null, null, primitiveType);
  }
}
