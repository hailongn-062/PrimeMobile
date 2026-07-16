package org.example.primemobile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FixDaGiao {
    public static void main(String[] args) throws IOException {
        Path start = Paths.get("src/main");
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String name = file.toString();
                if (name.endsWith(".java") || name.endsWith(".html") || name.endsWith(".css")) {
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    if (content.contains("da_giao")) {
                        content = content.replace("da_giao", "da_hoan_thanh");
                        Files.writeString(file, content, StandardCharsets.UTF_8);
                        System.out.println("Fixed " + file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        System.out.println("DONE FIXING DA_GIAO");
    }
}
