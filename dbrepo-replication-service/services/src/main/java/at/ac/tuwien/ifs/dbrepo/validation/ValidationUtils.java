package at.ac.tuwien.ifs.dbrepo.validation;

import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@UtilityClass
public class ValidationUtils {

    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    /**
     * Validates if a string is not null and not empty
     */
    public static boolean isValidString(String value) {
        return StringUtils.hasText(value);
    }

    /**
     * Validates if a string is a valid UUID
     */
    public static boolean isValidUuid(String uuid) {
        return isValidString(uuid) && UUID_PATTERN.matcher(uuid).matches();
    }

    /**
     * Validates if a number is positive
     */
    public static boolean isPositive(Long value) {
        return value != null && value > 0;
    }

    /**
     * Validates if a number is positive
     */
    public static boolean isPositive(Integer value) {
        return value != null && value > 0;
    }
} 