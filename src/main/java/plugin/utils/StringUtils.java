package plugin.utils;

import java.util.Random;

public class StringUtils {

    public String generateRandomString(final int length) {
        Random random = new Random();
        int leftLimit = 48; // 0
        int rightLimit = 122; // z

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97)) // exclude trash
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public int generateId(int length) {
        Random random = new Random();
        int leftLimit = 48; // 0
        int rightLimit = 57; // 9

        return Integer.parseInt(random.ints(leftLimit, rightLimit + 1)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString());
    }

}
