package at.tuwien.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminToken {

    private static AdminToken instance = null;

    private String token;

    public static synchronized AdminToken getInstance() {
        if (instance == null) {
            instance = new AdminToken();
        }
        return instance;
    }

}
