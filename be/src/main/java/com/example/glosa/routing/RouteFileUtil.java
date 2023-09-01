package com.example.glosa.routing;

import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.util.ResourceUtils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
class RouteFileUtil {

    @SneakyThrows
    static String read(String fileName) {
        return readLines(fileName).stream().collect(Collectors.joining("\n"));
    }

    @SneakyThrows
    static List<String> readLines(String fileName) {
        return Files.lines(
                ResourceUtils.getFile(ResourceUtils.CLASSPATH_URL_PREFIX + "routes/" + fileName).toPath())
                .toList().stream().map(s -> removeUTF8BOM(s)).toList();
    }

    public static final String UTF8_BOM = "\uFEFF";

    private static String removeUTF8BOM(String s) {
        if (s.startsWith(UTF8_BOM)) {
            s = s.substring(1);
        }
        return s;
    }
}
