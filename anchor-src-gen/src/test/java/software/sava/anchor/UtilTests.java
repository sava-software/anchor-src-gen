package software.sava.anchor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class UtilTests {

  @Test
  void testSnakeCase() {
    assertEquals("system_transfer", AnchorUtil.snakeCase("systemTransfer"));
    assertEquals("system_transfer", AnchorUtil.snakeCase("SystemTransfer"));
    assertSame("system_transfer", AnchorUtil.snakeCase("system_transfer"));
    assertEquals("system_transfer", AnchorUtil.snakeCase("SYSTEM_TRANSFER"));
    assertEquals("system_t_r_a_n_s_f_e_r", AnchorUtil.snakeCase("SystemTRANSFER"));
    assertEquals("transfer", AnchorUtil.snakeCase("Transfer"));
    assertSame("transfer", AnchorUtil.snakeCase("transfer"));
    assertEquals("transfer", AnchorUtil.snakeCase("TRANSFER"));

    assertEquals("SYSTEM_TRANSFER", AnchorUtil.snakeCase("systemTransfer", true));
    assertEquals("SYSTEM_TRANSFER", AnchorUtil.snakeCase("SystemTransfer", true));
    assertEquals("SYSTEM_TRANSFER", AnchorUtil.snakeCase("system_transfer", true));
    assertSame("SYSTEM_TRANSFER", AnchorUtil.snakeCase("SYSTEM_TRANSFER", true));
    assertEquals("SYSTEM_T_R_A_N_S_F_E_R", AnchorUtil.snakeCase("SystemTRANSFER", true));
    assertEquals("TRANSFER", AnchorUtil.snakeCase("Transfer", true));
    assertEquals("TRANSFER", AnchorUtil.snakeCase("transfer", true));
    assertSame("TRANSFER", AnchorUtil.snakeCase("TRANSFER", true));

    assertEquals("_system_transfer", AnchorUtil.snakeCase("_systemTransfer"));
    assertEquals("_system_transfer", AnchorUtil.snakeCase("_SystemTransfer"));
    assertSame("_system_transfer", AnchorUtil.snakeCase("_system_transfer"));
    assertEquals("_system_transfer", AnchorUtil.snakeCase("_SYSTEM_TRANSFER"));
    assertEquals("_system_t_r_a_n_s_f_e_r", AnchorUtil.snakeCase("_SystemTRANSFER"));
    assertEquals("_transfer", AnchorUtil.snakeCase("_Transfer"));
    assertSame("_transfer", AnchorUtil.snakeCase("_transfer"));
    assertEquals("_transfer", AnchorUtil.snakeCase("_TRANSFER"));

    assertEquals("_SYSTEM_TRANSFER", AnchorUtil.snakeCase("_systemTransfer", true));
    assertEquals("_SYSTEM_TRANSFER", AnchorUtil.snakeCase("_SystemTransfer", true));
    assertEquals("_SYSTEM_TRANSFER", AnchorUtil.snakeCase("_system_transfer", true));
    assertSame("_SYSTEM_TRANSFER", AnchorUtil.snakeCase("_SYSTEM_TRANSFER", true));
    assertEquals("_SYSTEM_T_R_A_N_S_F_E_R", AnchorUtil.snakeCase("_SystemTRANSFER", true));
    assertEquals("_TRANSFER", AnchorUtil.snakeCase("_Transfer", true));
    assertEquals("_TRANSFER", AnchorUtil.snakeCase("_transfer", true));
    assertSame("_TRANSFER", AnchorUtil.snakeCase("_TRANSFER", true));
  }
}
