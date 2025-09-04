package software.sava.idl.generator;

public final class AnchorStringConstant extends BaseAnchorConstant {

  private final String value;

  AnchorStringConstant(final String name, final String value) {
    super(name);
    this.value = value;
  }

  @Override
  public void toSrc(final GenSrcContext genSrcContext, final StringBuilder src) {
    src.append(String.format("""
            %spublic static final String %s = \"""
            %s    %s\""";
            
            """,
        genSrcContext.tab(), name,
        genSrcContext.tab(), value
    ));
  }
}
