package software.sava.idl.generator.anchor;

public final class AnchorIntConstant extends BaseAnchorConstant {

  private final int value;

  AnchorIntConstant(final String name, final int value) {
    super(name);
    this.value = value;
  }

  @Override
  public void toSrc(final SrcGenContext srcGenContext, final StringBuilder src) {
    src.append(String.format("""
            %spublic static final int %s = %d;
            
            """,
        srcGenContext.tab(), name, value
    ));
  }
}
