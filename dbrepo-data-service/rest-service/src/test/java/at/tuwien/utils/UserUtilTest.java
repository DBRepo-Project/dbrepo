package at.tuwien.utils;

import at.tuwien.test.BaseTest;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

public class UserUtilTest extends BaseTest {

    @Test
    public void hasRole_succeeds() {
        assertTrue(UserUtil.hasRole(USER_1_PRINCIPAL, "find-container"));
    }

    @Test
    public void hasRole_principalMissing_fails() {
        assertFalse(UserUtil.hasRole(null, "find-container"));
    }

    @Test
    public void hasRole_roleMissing_fails() {
        assertFalse(UserUtil.hasRole(USER_1_PRINCIPAL, null));
    }

    @Test
    public void getId_succeeds() {
        assertEquals(USER_1_ID, UserUtil.getId(USER_1_PRINCIPAL));
    }

    @Test
    public void getId_principalMissing_fails() {
        assertNull(UserUtil.getId(null));
    }

    @Test
    public void getId_roleMissing_fails() {
        assertNull(UserUtil.getId(USER_LOCAL_ADMIN_PRINCIPAL));
    }
}
