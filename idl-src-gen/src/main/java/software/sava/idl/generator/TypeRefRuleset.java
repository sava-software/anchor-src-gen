package software.sava.idl.generator;

import software.sava.core.accounts.PublicKey;
import software.sava.rpc.json.PublicKeyEncoding;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.*;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record TypeRefRuleset(PublicKey refProgram,
                      SrcMismatch srcMismatch,
                      boolean matchOnTypeName,
                      Set<String> excludeTypes,
                      Map<String, TypeRefRule> explicitRules) {

  enum SrcMismatch {
    ERROR,
    KEEP_LOCAL,
    KEEP_REF,
    WARN_KEEP_LOCAL,
    WARN_KEEP_REF
  }

  static List<TypeRefRuleset> parseRulesets(final JsonIterator ji) {
    final var rulesets = new ArrayList<TypeRefRuleset>();
    while (ji.readArray()) {
      rulesets.add(parseRuleset(ji));
    }
    return rulesets;
  }

  static TypeRefRuleset parseRuleset(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createRuleset();
  }

  public boolean isExcluded(final String type) {
    return excludeTypes.contains(type);
  }

  private static final class Parser implements FieldBufferPredicate {

    private PublicKey refProgram;
    private SrcMismatch srcMismatch = SrcMismatch.WARN_KEEP_LOCAL;
    private boolean matchOnTypeName;
    private List<String> excludeTypes;
    private Map<String, TypeRefRule> explicitRules;

    private Parser() {
    }

    private TypeRefRuleset createRuleset() {
      return new TypeRefRuleset(
          refProgram,
          srcMismatch,
          matchOnTypeName,
          excludeTypes == null ? Set.of() : new HashSet<>(excludeTypes),
          explicitRules
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("refProgram", buf, offset, len)) {
        this.refProgram = PublicKeyEncoding.parseBase58Encoded(ji);
      } else if (fieldEquals("srcMismatch", buf, offset, len)) {
        this.srcMismatch = SrcMismatch.valueOf(ji.readString());
      } else if (fieldEquals("matchOnTypeName", buf, offset, len)) {
        this.matchOnTypeName = ji.readBoolean();
      } else if (fieldEquals("excludeTypes", buf, offset, len)) {
        this.excludeTypes = new ArrayList<>();
        while (ji.readArray()) {
          excludeTypes.add(ji.readString());
        }
      } else if (fieldEquals("explicitRules", buf, offset, len)) {
        final var rules = new HashMap<String, TypeRefRule>();
        while (ji.readArray()) {
          final var ruleParser = new TypeRefRuleParser(srcMismatch);
          ji.testObject(ruleParser);
          final var rule = ruleParser.createRule();
          rules.put(rule.localType(), rule);
        }
        this.explicitRules = rules;
      } else {
        throw new IllegalStateException("Unhandled TypeRefRuleset field " + new String(buf, offset, len));
      }
      return true;
    }
  }

  private static final class TypeRefRuleParser implements FieldBufferPredicate {

    private String refType;
    private String localType;
    private SrcMismatch srcMismatch;

    private TypeRefRuleParser(final SrcMismatch srcMismatch) {
      this.srcMismatch = srcMismatch;
    }

    private TypeRefRule createRule() {
      return new TypeRefRule(refType, localType, srcMismatch);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("refType", buf, offset, len)) {
        this.refType = ji.readString();
      } else if (fieldEquals("localType", buf, offset, len)) {
        this.localType = ji.readString();
      } else if (fieldEquals("srcMismatch", buf, offset, len)) {
        this.srcMismatch = SrcMismatch.valueOf(ji.readString());
      } else {
        throw new IllegalStateException("Unhandled TypeRefRule field " + new String(buf, offset, len));
      }
      return true;
    }
  }
}
