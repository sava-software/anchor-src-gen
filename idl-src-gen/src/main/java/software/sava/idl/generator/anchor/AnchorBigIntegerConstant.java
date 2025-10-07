package software.sava.idl.generator.anchor;

import java.math.BigInteger;

public final class AnchorBigIntegerConstant extends BaseAnchorConstant {

  private final String value;

  AnchorBigIntegerConstant(final String name, final String value) {
    super(name);
    this.value = value;
  }

  @Override
  public void toSrc(final SrcGenContext srcGenContext, final StringBuilder src) {
    srcGenContext.addImport(BigInteger.class);
    src.append(String.format("""
            %spublic static final BigInteger %s = new BigInteger("%s");
            
            """,
        srcGenContext.tab(), name, value
    ));
  }
}
