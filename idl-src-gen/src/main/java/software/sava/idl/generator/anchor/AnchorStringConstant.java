package software.sava.idl.generator.anchor;

public final class AnchorStringConstant extends BaseAnchorConstant {

  private final String value;

  AnchorStringConstant(final String name, final String value) {
    super(name);
    this.value = value;
  }

  @Override
  public void toSrc(final SrcGenContext srcGenContext, final StringBuilder src) {
    src.append(String.format("""
            %spublic static final String %s = \"""
            %s    %s\""";
            
            """,
        srcGenContext.tab(), name,
        srcGenContext.tab(), value
    ));
  }
}
