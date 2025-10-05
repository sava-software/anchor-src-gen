package software.sava.idl.generator.anchor;

import java.util.List;

public abstract class BaseNamedTypeParser implements NamedTypeParser {

  protected final boolean firstUpper;
  protected List<String> docs;
  protected String name;
  protected AnchorSerialization serialization;
  protected AnchorTypeContext type;

  public BaseNamedTypeParser(final boolean firstUpper) {
    this.firstUpper = firstUpper;
  }
}
