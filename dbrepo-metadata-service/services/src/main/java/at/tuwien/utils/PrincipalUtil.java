package at.tuwien.utils;

import java.security.Principal;

public class PrincipalUtil {

    public static String formatForDebug(Principal principal) {
        if (principal == null) {
            return "principal=null";
        }
        return "principal.name=" + principal.getName();
    }

}
