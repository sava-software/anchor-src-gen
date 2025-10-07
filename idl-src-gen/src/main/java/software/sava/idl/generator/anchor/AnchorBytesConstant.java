package software.sava.idl.generator.anchor;

public final class AnchorBytesConstant extends BaseAnchorConstant {

  private final byte[] value;
  private static final byte b = -127;

  AnchorBytesConstant(final String name, final byte[] value) {
    super(name);
    this.value = value;
  }

  @Override
  public void toSrc(final SrcGenContext srcGenContext, final StringBuilder src) {
    final var elements = new String[value.length];
    for (int i = 0; i < elements.length; i++) {
      elements[i] = Byte.toString(value[i]);
    }

    src.append(String.format("""
            %spublic static final byte[] %s = new byte[]{%s};
            
            """,
        srcGenContext.tab(), name, String.join(", ", elements)
    ));
  }

  @Override
  public byte[] bytes() {
    return value;
  }
}
