package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

record ProgramIdValueNode() implements ContextualValueNode {

  private static final ProgramIdValueNode INSTANCE = new ProgramIdValueNode();

  static ProgramIdValueNode parse(final JsonIterator ji) {
    ji.skipRestOfObject();
    return INSTANCE;
  }
}
