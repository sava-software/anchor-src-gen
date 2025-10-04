package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

record PayerValueNode() implements ContextualValueNode {

  private static final PayerValueNode INSTANCE = new PayerValueNode();

  public static PayerValueNode parse(final JsonIterator ji) {
    ji.skipRestOfObject();
    return INSTANCE;
  }
}
