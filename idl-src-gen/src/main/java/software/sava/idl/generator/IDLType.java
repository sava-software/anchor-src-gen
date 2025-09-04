package software.sava.idl.generator;

import java.util.function.Supplier;

public enum IDLType {

  ANCHOR,
  SHANK;

  private final Supplier<NamedTypeParser> upperTypeParserFactory;
  private final Supplier<NamedTypeParser> lowerTypeParserFactory;
  private final Supplier<AnchorInstructionParser> instructionParserFactory;

  IDLType() {
    this.upperTypeParserFactory = () -> new AnchorNamedTypeParser(this, true);
    this.lowerTypeParserFactory = () -> new AnchorNamedTypeParser(this, false);
    this.instructionParserFactory = () -> new AnchorInstructionParser(this);
  }

  Supplier<NamedTypeParser> upperTypeParserFactory() {
    return upperTypeParserFactory;
  }

  Supplier<NamedTypeParser> lowerTypeParserFactory() {
    return lowerTypeParserFactory;
  }

  Supplier<AnchorInstructionParser> instructionParserFactory() {
    return instructionParserFactory;
  }
}
