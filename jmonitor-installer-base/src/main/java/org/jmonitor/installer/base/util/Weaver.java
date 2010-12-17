/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jmonitor.installer.base.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.MessageHandler;
import org.aspectj.tools.ajc.Main;
import org.aspectj.tools.ajc.Main.MessagePrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for weaving ear and war files using AspectJ.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class Weaver {

    private static final Logger LOGGER = LoggerFactory.getLogger(Weaver.class);

    private final List<File> aspectPath;
    private final List<File> aspectClasspath;
    private final List<String> ajcArgs;
    private final List<Pattern> exclusions;

    private final File tempDirectory;

    public Weaver(List<File> aspectPath, List<File> aspectClasspath, List<String> ajcArgs,
            List<Pattern> exclusions, File tempDirectory) {

        this.aspectPath = aspectPath;
        this.aspectClasspath = aspectClasspath;
        this.ajcArgs = ajcArgs;
        this.exclusions = exclusions;
        this.tempDirectory = tempDirectory;
    }

    public void weaveEar(JarInputStream earInputStream, JarOutputStream earOutputStream)
            throws IOException {

        JarEntry earEntry;
        while ((earEntry = earInputStream.getNextJarEntry()) != null) {
            if (earEntry.getName().matches("[^/]*\\.war")) {

                LOGGER.info("weaving war file " + earEntry.getName() + " ..");

                // found a war file
                JarEntry newJarEntry = new JarEntry(earEntry.getName());
                earOutputStream.putNextEntry(newJarEntry);
                // weave war file
                JarInputStream warInputStream = new JarInputStream(earInputStream);
                JarOutputStream warOutputStream = new JarOutputStream(earOutputStream);
                weaveWar(warInputStream, warOutputStream);
                // we cannot close it since that would close the underlying
                // stream, so instead we "finish"
                warOutputStream.finish();
            } else {
                JarEntry newJarEntry = new JarEntry(earEntry.getName());
                earOutputStream.putNextEntry(newJarEntry);
                IOUtils.copy(earInputStream, earOutputStream);
            }
        }
    }

    public void weaveWar(final JarInputStream warInputStream, final JarOutputStream warOutputStream)
            throws IOException {

        weaveWarInternal(warInputStream, warOutputStream, exclusions, tempDirectory);
    }

    private void weaveWarInternal(JarInputStream warInputStream, JarOutputStream warOutputStream,
            List<Pattern> exclusions, File tempDirectory) throws IOException, FileNotFoundException {

        // extract jar files under WEB-INF/lib and all classes under WEB-INF/classes
        File extractedDir = new File(tempDirectory, "extracted");
        JarEntry warEntry;
        while ((warEntry = warInputStream.getNextJarEntry()) != null) {

            for (Pattern exclusion : exclusions) {
                if (exclusion.matcher(warEntry.getName()).matches()) {
                    // entry matches exclusion pattern
                    continue;
                }
            }

            if (warEntry.getName().matches("WEB-INF/lib/[^/]*\\.jar")) {
                extractNextEntry(warInputStream, extractedDir, warEntry.getName());
            } else if (warEntry.getName().matches("WEB-INF/classes/.*\\.class")) {
                // found a class file, extract it for weaving
                String name = warEntry.getName();
                extractNextEntry(warInputStream, extractedDir, name);
            } else {
                JarEntry newJarEntry = new JarEntry(warEntry.getName());
                warOutputStream.putNextEntry(newJarEntry);
                IOUtils.copy(warInputStream, warOutputStream);
            }
        }

        // create directory to output weaving
        File wovenDir = new File(tempDirectory, "woven");
        FileUtils.forceMkdir(wovenDir);

        // build classpath for aspectj compiler
        List<File> fullClasspath = buildFullClasspath(extractedDir, aspectClasspath);

        int weavingTime = 0;

        // extract and weave class files
        File extractedClassesDir = new File(extractedDir, "WEB-INF/classes");
        if (extractedClassesDir.exists()) {

            LOGGER.info("weaving WEB-INF/classes ..");

            File wovenClassesDir = new File(wovenDir, "classes");
            FileUtils.forceMkdir(wovenClassesDir);

            weavingTime +=
                    weave(extractedClassesDir.getPath(), aspectPath, wovenClassesDir.getPath(),
                            fullClasspath, ajcArgs);

            // write classes to war output stream
            writeResourcesAndRecurse(wovenClassesDir, warOutputStream, "WEB-INF/classes");
        }

        // extract and weave jar files
        File extractedLibDir = new File(extractedDir, "WEB-INF/lib");
        if (extractedLibDir.exists()) {
            List<File> extractedJarFiles = Arrays.asList(extractedLibDir.listFiles());
            Collections.sort(extractedJarFiles);
            File wovenLibDir = new File(wovenDir, "lib");
            FileUtils.forceMkdir(wovenLibDir);
            for (File extractedJarFile : extractedJarFiles) {

                LOGGER.info("weaving WEB-INF/lib/" + extractedJarFile.getName() + " ..");

                if (!isValidJar(extractedJarFile)) {
                    // jar file is invalid
                    // (aspectj will fail on this jar file, so better we handle it here)
                    LOGGER.warn("skipping WEB-INF/lib/" + extractedJarFile.getName()
                            + " because it signed but it failed signature verification");
                    continue;
                }

                weavingTime +=
                        weave(extractedJarFile.getPath(), aspectPath, new File(wovenLibDir,
                                extractedJarFile.getName()).getPath(), fullClasspath, ajcArgs);
            }

            // write jar files to war output stream
            writeResourcesAndRecurse(wovenLibDir, warOutputStream, "WEB-INF/lib");
        }
        LOGGER.info("total aspectj weaving time: "
                + FormatUtils.formatAsMinutesAndSeconds(weavingTime));
    }

    private static boolean isValidJar(File file) {

        try {
            JarFile jarFile = new JarFile(file);

            // quick read through jar file to validate it
            // TODO add bcprov-jdk15-1.43.jar (invalid signature) to tests in order to validate that
            for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
                jarFile.getInputStream(e.nextElement());
            }

        } catch (SecurityException e) {
            // invalid signature
            return false;
        } catch (IOException e) {
            // not sure what to do here, at least
            throw new IllegalStateException(e);
        }

        return true;
    }

    // WHY DOESN'T THIS WORK????
    /*
     * private static boolean isValidJar(File jarFile) {
     * 
     * try { JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile));
     * 
     * // quick read through jar file to validate it // TODO add bcprov-jdk15-1.43.jar (invalid
     * signature) to tests in order to validate that OutputStream nullOutputStream = new
     * NullOutputStream(); while (jarInputStream.getNextJarEntry() != null) {
     * IOUtils.copy(jarInputStream, nullOutputStream); }
     * 
     * jarInputStream.close();
     * 
     * } catch (SecurityException e) { // invalid signature return false; } catch (IOException e) {
     * // not sure what to do here, at least throw new IllegalStateException(e); }
     * 
     * return true; }
     */

    private static void extractNextEntry(JarInputStream warInputStream, File extractedDir,
            String name) throws FileNotFoundException, IOException {

        File extractedFile = new File(extractedDir, name);
        FileUtils.forceMkdir(extractedFile.getParentFile());
        StreamUtils.copy(warInputStream, extractedFile);
    }

    private static List<File> buildFullClasspath(File extractedDir, List<File> aspectClasspath) {

        File extractedLibDir = new File(extractedDir, "WEB-INF/lib");
        List<File> extractedJarFiles = Arrays.asList(extractedLibDir.listFiles());
        Collections.sort(extractedJarFiles);
        File extractedClassesDir = new File(extractedDir, "WEB-INF/classes");

        List<File> classpathElements = new ArrayList<File>();
        classpathElements.addAll(aspectClasspath);
        if (extractedClassesDir.exists()) {
            classpathElements.add(extractedClassesDir);
        }

        if (extractedLibDir.exists()) {
            for (File extractedJarFile : extractedJarFiles) {
                classpathElements.add(extractedJarFile);
            }
        }
        return classpathElements;
    }

    private static void writeResourcesAndRecurse(File dir, JarOutputStream warOutputStream,
            String warBase) throws IOException {

        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                writeResourcesAndRecurse(file, warOutputStream, warBase + "/" + file.getName());
            } else {
                JarEntry newJarEntry = new JarEntry(warBase + "/" + file.getName());
                warOutputStream.putNextEntry(newJarEntry);
                StreamUtils.copy(file, warOutputStream);
            }
        }
    }

    private static long weave(String inpath, List<File> aspectpath, String output,
            List<File> classpath, List<String> ajcArgs) {

        List<String> args = new ArrayList<String>();
        args.add("-1.5");
        args.add("-inpath");
        args.add(inpath);
        args.add("-aspectpath");
        args.add(buildPath(aspectpath));
        if (new File(inpath).isFile()) {
            args.add("-outjar");
            args.add(output);
        } else {
            // inpath is a directory
            args.add("-d");
            args.add(output);
        }
        args.add("-classpath");
        args.add(buildPath(classpath));
        args.addAll(ajcArgs);

        LOGGER.debug("ajc " + StringUtils.join(args, " "));

        Main ajc = new Main();
        MessageHandler ajcMessageHolder = new MessageHandler(true);

        if (ajcArgs.contains("-verbose")) {
            // verbose logging is enabled
            ajcMessageHolder.setInterceptor(new MessageLogger(true));
        } else {
            ajcMessageHolder.ignore(IMessage.INFO);
            ajcMessageHolder.setInterceptor(new MessageLogger(false));
        }
        ajc.setHolder(ajcMessageHolder);

        long startTime = System.currentTimeMillis();
        ajc.runMain(args.toArray(new String[args.size()]), false);
        long duration = System.currentTimeMillis() - startTime;

        // TODO provide switch to enable log messages

        if (ajcMessageHolder.getMessages(IMessage.ERROR, true).length > 0) {
            // TODO validate that temp directories are deleted
            throw new IllegalStateException("error(s) occurred during aspectj compilation");
        }
        return duration;
    }

    private static String buildPath(List<File> pathElements) {
        return StringUtils.join(pathElements, File.pathSeparator);
    }

    private static class MessageLogger extends MessagePrinter {

        protected MessageLogger(boolean verbose) {
            super(verbose);
        }

        @Override
        public boolean handleMessage(IMessage message) {
            if (message != null) {
                if (message.getKind().isSameOrLessThan(IMessage.WEAVEINFO)) {
                    LOGGER.trace(message.getMessage());
                } else if (message.getKind().isSameOrLessThan(IMessage.TASKTAG)) {
                    LOGGER.debug(message.getMessage());
                } else if (message.getKind().isSameOrLessThan(IMessage.WARNING)) {
                    LOGGER.warn(message.getMessage());
                } else {
                    LOGGER.error(message.getMessage());
                }
            }
            return false;
        }
    }
}
