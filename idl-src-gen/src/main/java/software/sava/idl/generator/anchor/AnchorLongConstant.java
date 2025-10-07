package software.sava.idl.generator.anchor;

public final class AnchorLongConstant extends BaseAnchorConstant {

  private final long value;

  AnchorLongConstant(final String name, final long value) {
    super(name);
    this.value = value;
  }

  @Override
  public void toSrc(final SrcGenContext srcGenContext, final StringBuilder src) {
    src.append(String.format("""
            %spublic static final long %s = %d;
            
            """,
        srcGenContext.tab(), name, value
    ));
  }
}
