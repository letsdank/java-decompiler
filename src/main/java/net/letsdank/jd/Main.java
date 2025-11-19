package net.letsdank.jd;

import net.letsdank.jd.io.ClassFileReader;
import net.letsdank.jd.model.ClassFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Простейший CLI: принимает путь к .class и печатает его версию.
 */
public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -jar java-decompiler.jar <path-to-class>");
            System.exit(1);
        }

        Path path = Path.of(args[0]);
        try (FileInputStream in = new FileInputStream(path.toFile())) {
            ClassFileReader reader = new ClassFileReader();
            ClassFile cf = reader.read(in);
            System.out.printf("Parsed class file: major=%d, minor=%d%n",
                    cf.majorVersion(), cf.minorVersion());
        } catch (IOException e) {
            System.err.println("Failed to read class file: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
