package software.sava.idl.generator.codama;

public enum ValueStrategy {
  OMITTED("omitted"),
  OPTIONAL("optional");

  private final String jsonValue;

  ValueStrategy(final String jsonValue) {
    this.jsonValue = jsonValue;
  }

  public String jsonValue() {
    return jsonValue;
  }

  public static ValueStrategy fromJsonValue(final String jsonValue) {
    return switch (jsonValue) {
      case "omitted" -> OMITTED;
      case "optional" -> OPTIONAL;
      default -> throw new IllegalArgumentException("Unknown ValueStrategy: " + jsonValue);
    };
  }
}
