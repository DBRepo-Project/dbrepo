package at.tuwien.test.utils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ArrayUtil {

    public static String[] merge(List<String[]> list) {
        final List<String> out = new LinkedList<>();
        list.stream()
                .forEach(roles -> out.addAll(Arrays.asList(roles)));
        return out.toArray(new String[]{});
    }

}
