package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

record RemainderCountNode() implements CountNode {

  private static final RemainderCountNode INSTANCE = new RemainderCountNode();

  static RemainderCountNode parse(final JsonIterator ji) {
    ji.skipRestOfObject();
    return INSTANCE;
  }
}
