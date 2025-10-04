package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

record PublicKeyTypeNode() implements TypeNode {

  private static final PublicKeyTypeNode INSTANCE = new PublicKeyTypeNode();

  public static PublicKeyTypeNode parse(final JsonIterator ji) {
    ji.skipRestOfObject();
    return INSTANCE;
  }
}
