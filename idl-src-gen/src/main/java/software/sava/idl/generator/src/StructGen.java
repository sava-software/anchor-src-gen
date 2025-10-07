package software.sava.idl.generator.src;

import software.sava.core.accounts.PublicKey;
import software.sava.idl.generator.anchor.SrcGenContext;
import software.sava.rpc.json.http.response.AccountInfo;

import java.util.function.BiFunction;

import static software.sava.idl.generator.ParseUtil.removeBlankLines;

public class StructGen {

  public static int sigLine(final StringBuilder src, final String name, final boolean publicAccess) {
    int recordSigLineLength = 8 + name.length();
    if (publicAccess) {
      recordSigLineLength += 7;
      src.append("public ");
    }
    src.append("record ").append(name).append('(');
    return recordSigLineLength;
  }

  public static String emptyStruct(final String tab,
                                   final StringBuilder src,
                                   final String interfaceName,
                                   final String name) {
    src.append(String.format("""
        ) implements %s {
        
        """, interfaceName
    ));
    src.append(String.format("""
        private static final %s INSTANCE = new %s();
        
        public static %s read(final byte[] _data, final int offset) {
        %sreturn INSTANCE;
        }
        
        @Override
        public int write(final byte[] _data, final int offset) {
        %sreturn 0;
        }
        
        @Override
        public int l() {
        %sreturn 0;
        }
        """, name, name, name, tab, tab, tab
    ).indent(tab.length()));
    src.append('}');
    return removeBlankLines(src.toString());
  }

  public static void readAccountInfo(final SrcGenContext srcGenContext,
                                     final StringBuilder src,
                                     final String name) {
    srcGenContext.addImport(AccountInfo.class);
    srcGenContext.addImport(BiFunction.class);
    srcGenContext.addImport(PublicKey.class);
    final var tab = srcGenContext.tab();
    src.append(String.format("""
            %sreturn read(null, _data, offset);
            }
            
            public static %s read(final AccountInfo<byte[]> accountInfo) {
            %sreturn read(accountInfo.pubKey(), accountInfo.data(), 0);
            }
            
            public static %s read(final PublicKey _address, final byte[] _data) {
            %sreturn read(_address, _data, 0);
            }
            
            public static final BiFunction<PublicKey, byte[], %s> FACTORY = %s::read;
            
            public static %s read(final PublicKey _address, final byte[] _data, final int offset) {""",
        tab, name, tab, name, tab, name, name, name
    ).indent(tab.length()));
  }
}
