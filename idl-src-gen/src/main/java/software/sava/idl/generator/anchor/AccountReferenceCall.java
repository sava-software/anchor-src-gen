package software.sava.idl.generator.anchor;

import software.sava.anchor.AnchorUtil;
import software.sava.core.accounts.PublicKey;
import software.sava.core.accounts.SolanaAccounts;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Map;

import static java.lang.invoke.MethodType.methodType;

public record AccountReferenceCall(Class<?> clas,
                                   String referenceVarName,
                                   Method zeroArgsMethodName,
                                   PublicKey key,
                                   String callReference) {

  public static void generateAccounts(final Class<?> api,
                                      final Object instance,
                                      final Map<PublicKey, AccountReferenceCall> accountMethods) {
    try {
      final var referenceVarName = AnchorUtil.camelCase(api.getSimpleName(), false);
      for (final var method : api.getMethods()) {
        if (method.getReturnType().equals(PublicKey.class)) {
          final var handle = MethodHandles.lookup().findVirtual(api, method.getName(), methodType(PublicKey.class));
          final var key = (PublicKey) handle.invoke(instance);
          final var getter = String.format("%s.%s()", referenceVarName, method.getName());
          accountMethods.put(key, new AccountReferenceCall(api, referenceVarName, method, key, getter));
        }
      }
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  public static void generateMainNetNativeAccounts(final Map<PublicKey, AccountReferenceCall> accountMethods) {
    generateAccounts(SolanaAccounts.class, SolanaAccounts.MAIN_NET, accountMethods);
  }
}
