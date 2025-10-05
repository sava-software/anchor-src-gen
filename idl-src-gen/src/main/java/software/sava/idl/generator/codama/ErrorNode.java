package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class ErrorNode extends NamedDocsNode {

  private final int code;
  private final String message;

  ErrorNode(final String name,
                   final List<String> docs,
                   final int code,
                   final String message) {
    super(name, docs);
    this.code = code;
    this.message = message;
  }

  int code() {
    return code;
  }

  String message() {
    return message;
  }

  static ErrorNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createErrorNode();
  }

  private static final class Parser extends BaseDocsParser {

    private int code;
    private String message;

    private Parser() {
    }

    ErrorNode createErrorNode() {
      return new ErrorNode(
          name,
          docs == null ? List.of() : docs,
          code,
          message
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("code", buf, offset, len)) {
        code = ji.readInt();
        return true;
      } else if (fieldEquals("message", buf, offset, len)) {
        message = ji.readString();
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }
}
