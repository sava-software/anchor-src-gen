package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

final class ArgumentValueNode extends NamedNode implements
    ContextualValueNode,
    ContextualValueNodeCondition,
    PdaSeedValueNodeValue {

  public ArgumentValueNode(final String name) {
    super(name);
  }

  public static ArgumentValueNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createArgumentValueNode();
  }

  static final class Parser extends BaseParser {

    ArgumentValueNode createArgumentValueNode() {
      return new ArgumentValueNode(name);
    }
  }
}
