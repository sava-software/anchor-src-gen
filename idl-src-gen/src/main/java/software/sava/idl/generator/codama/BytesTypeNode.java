package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

record BytesTypeNode() implements TypeNode {

  private static final BytesTypeNode INSTANCE = new BytesTypeNode();

  public static BytesTypeNode parse(final JsonIterator ji) {
    ji.skipRestOfObject();
    return INSTANCE;
  }
}
