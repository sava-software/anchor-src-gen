package software.sava.anchor;

import java.util.function.Supplier;

public enum IDLType {

  ANCHOR,
  SHANK;

  private final Supplier<AnchorNamedTypeParser> upperTypeParserFactory;
  private final Supplier<AnchorNamedTypeParser> lowerTypeParserFactory;
  private final Supplier<AnchorInstructionParser> instructionParserFactory;

  IDLType() {
    this.upperTypeParserFactory = () -> new AnchorNamedTypeParser(this, true);
    this.lowerTypeParserFactory = () -> new AnchorNamedTypeParser(this, false);
    this.instructionParserFactory = () -> new AnchorInstructionParser(this);
  }

  Supplier<AnchorNamedTypeParser> upperTypeParserFactory() {
    return upperTypeParserFactory;
  }

  Supplier<AnchorNamedTypeParser> lowerTypeParserFactory() {
    return lowerTypeParserFactory;
  }

  Supplier<AnchorInstructionParser> instructionParserFactory() {
    return instructionParserFactory;
  }
}
