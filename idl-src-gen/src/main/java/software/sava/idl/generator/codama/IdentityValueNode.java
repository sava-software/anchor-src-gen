package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

record IdentityValueNode() implements ContextualValueNode {

  private static final IdentityValueNode INSTANCE = new IdentityValueNode();

  static IdentityValueNode parse(final JsonIterator ji) {
    ji.skipRestOfObject();
    return INSTANCE;
  }
}
