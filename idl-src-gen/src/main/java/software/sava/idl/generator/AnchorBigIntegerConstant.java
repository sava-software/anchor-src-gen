package software.sava.idl.generator;

import java.math.BigInteger;

public final class AnchorBigIntegerConstant extends BaseAnchorConstant {

  private final String value;

  AnchorBigIntegerConstant(final String name, final String value) {
    super(name);
    this.value = value;
  }

  @Override
  public void toSrc(final GenSrcContext genSrcContext, final StringBuilder src) {
    genSrcContext.addImport(BigInteger.class);
    src.append(String.format("""
            %spublic static final BigInteger %s = new BigInteger("%s");
            
            """,
        genSrcContext.tab(), name, value
    ));
  }
}
