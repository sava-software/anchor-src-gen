package software.sava.anchor;

import java.util.function.Supplier;

public enum IDLType {

  ANCHOR,
  SHANK;

  private final Supplier<AnchorNamedTypeParser> upperFactory;
  private final Supplier<AnchorNamedTypeParser> lowerFactory;
  private final Supplier<AnchorInstructionParser> instructionParserFactory;

  IDLType() {
    this.upperFactory = () -> new AnchorNamedTypeParser(this, true);
    this.lowerFactory = () -> new AnchorNamedTypeParser(this, false);
    this.instructionParserFactory = () -> new AnchorInstructionParser(this);
  }

  Supplier<AnchorNamedTypeParser> upperFactory() {
    return upperFactory;
  }

  Supplier<AnchorNamedTypeParser> lowerFactory() {
    return lowerFactory;
  }

  Supplier<AnchorInstructionParser> instructionParserFactory() {
    return instructionParserFactory;
  }
}
