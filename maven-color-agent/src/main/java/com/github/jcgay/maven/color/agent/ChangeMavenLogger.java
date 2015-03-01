package com.github.jcgay.maven.color.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import static com.github.jcgay.maven.color.agent.MavenSurefireVisitor.Version;

/**
 * Java agent to rewrite some part of Maven logging initialization.
 */
public class ChangeMavenLogger {

    private ChangeMavenLogger() {
        // hide construction
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        if (!"false".equals(System.getenv("MAVEN_COLOR"))) {
            inst.addTransformer(new ReplaceMavenLoggerWithAnsiLogger());
        }
    }

    /**
     * Rewrite classes:
     * <ul>
     *     <li>Maven : MavenCli#setupLogger</li>
     *     <li>Surefire 2.13 : DefaultReporterFactory#createConsoleLogger</li>
     *     <li>Surefire 2.9 : FileReporterFactory#createConsoleLogger</li>
     *     <li>Surefire 2.3 : SurefireBooter#getForkingStreamConsumer</li>
     * </ul>
     */
    static class ReplaceMavenLoggerWithAnsiLogger implements ClassFileTransformer {

        public byte[] transform(ClassLoader classLoader, String s, Class<?> aClass, ProtectionDomain protectionDomain, byte[] bytes) throws IllegalClassFormatException {

            if ("org/apache/maven/cli/MavenCli".equals(s)) {
                ClassReader reader = new ClassReader(bytes);
                ClassWriter writer = new ClassWriter(reader, 0);
                MavenCliVisitor visitor = new MavenCliVisitor(writer);
                reader.accept(visitor, 0);
                return writer.toByteArray();
            }
            if ("org/apache/maven/plugin/surefire/report/DefaultReporterFactory".equals(s)) {
                ClassReader reader = new ClassReader(bytes);
                ClassWriter writer = new ClassWriter(reader, 0);
                MavenSurefireVisitor visitor = new MavenSurefireVisitor(writer, Version.SUREFIRE_2_13);
                reader.accept(visitor, 0);
                return writer.toByteArray();
            }
            if ("org/apache/maven/plugin/surefire/report/FileReporterFactory".equals(s)) {
                ClassReader reader = new ClassReader(bytes);
                ClassWriter writer = new ClassWriter(reader, 0);
                MavenSurefireVisitor visitor = new MavenSurefireVisitor(writer, Version.SUREFIRE_2_9);
                reader.accept(visitor, 0);
                return writer.toByteArray();
            }
            if ("org/apache/maven/surefire/booter/SurefireBooter".equals(s)) {
                ClassReader reader = new ClassReader(bytes);
                ClassWriter writer = new ClassWriter(reader, 0);
                MavenSurefireBooterVisitor visitor = new MavenSurefireBooterVisitor(writer);
                reader.accept(visitor, 0);
                return writer.toByteArray();
            }

            return null;
        }
    }
}
