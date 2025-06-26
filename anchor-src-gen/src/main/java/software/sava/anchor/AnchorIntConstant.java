package software.sava.anchor;

public final class AnchorIntConstant extends BaseAnchorConstant {

  private final int value;

  AnchorIntConstant(final String name, final int value) {
    super(name);
    this.value = value;
  }

  @Override
  public void toSrc(final GenSrcContext genSrcContext, final StringBuilder src) {
    src.append(String.format("""
            %spublic static final int %s = %d;
            
            """,
        genSrcContext.tab(), name, value
    ));
  }
}
