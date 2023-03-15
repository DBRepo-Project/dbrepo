package at.tuwien.auth;

import org.apache.commons.codec.digest.DigestUtils;

import javax.xml.bind.DatatypeConverter;

public class MariaDbPassword {

    /**
     * Encodes a plain password to the MariaDB cipher text, is equivalent to the MySQL function
     * <p>
     * <code>SELECT CONCAT('*', UPPER(SHA1(UNHEX(SHA1('the_password')))))</code>
     * <p>
     * Source: <a href="https://stackoverflow.com/questions/50914611/how-to-create-password-hash-for-mysql-externally">https://stackoverflow.com/questions/50914611/how-to-create-password-hash-for-mysql-externally</a>
     *
     * @param plain The plain password
     * @return The password cipher for MariaDB 10.5
     */
    public static String encode(String plain) {
        return "*" + DigestUtils.sha1Hex(DatatypeConverter.parseHexBinary(DigestUtils.sha1Hex(plain)))
                .toUpperCase();
    }

}
