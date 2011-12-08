/**
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmonitor.installer.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.MessageHandler;
import org.aspectj.tools.ajc.Main;
import org.aspectj.tools.ajc.Main.MessagePrinter;
import org.jmonitor.installer.InstallerMain;
import org.jmonitor.installer.util.TempDirectoryUtils.TempDirectoryCallback;

/**
 * Utility class for weaving ear and war files using AspectJ.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public final class WeaverUtils {

    private static final Log LOG = LogFactory.getLog(WeaverUtils.class);

    // utility class
    private WeaverUtils() {}

    public static void weaveEar(String[] aspectPath, String[] aspectClasspath,
            List<String> ajcArgs, JarInputStream earInputStream, JarOutputStream earOutputStream)
            throws IOException {

        JarEntry earEntry;
        while ((earEntry = earInputStream.getNextJarEntry()) != null) {
            if (earEntry.getName().matches("[^/]*\\.war")) {

                LOG.info("weaving war file " + earEntry.getName() + " ..");

                // found a war file
                JarEntry newJarEntry = new JarEntry(earEntry.getName());
                earOutputStream.putNextEntry(newJarEntry);
                // weave war file
                JarInputStream warInputStream = new JarInputStream(earInputStream);
                JarOutputStream warOutputStream = new JarOutputStream(earOutputStream);
                WeaverUtils.weaveWar(aspectPath, aspectClasspath, ajcArgs, warInputStream,
                        warOutputStream);
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

    public static void weaveWar(final String[] aspectPath, final String[] aspectClasspath,
            final List<String> ajcArgs, final JarInputStream warInputStream,
            final JarOutputStream warOutputStream) throws IOException {

        TempDirectoryUtils.execute("jmonitor-weaver-utils-", new TempDirectoryCallback() {
            public void doWithTempDirectory(File tempDirectory) throws IOException {
                weaveWarInternal(aspectPath, aspectClasspath, ajcArgs, warInputStream,
                        warOutputStream, tempDirectory);
            }
        });
    }

    private static void weaveWarInternal(String[] aspectPath, String[] aspectClasspath,
            List<String> ajcArgs, JarInputStream warInputStream, JarOutputStream warOutputStream,
            File tempDirectory) throws IOException, FileNotFoundException {

        // extract jar files under WEB-INF/lib and all classes under WEB-INF/classes
        File extractedDir = new File(tempDirectory, "extracted");
        JarEntry warEntry;
        while ((warEntry = warInputStream.getNextJarEntry()) != null) {
            if (InstallerMain.isJmonitorRuntimeLtwJarFile(warEntry)) {
                // TODO WeaverUtils should be independent of jmonitor
                continue;
            } else if (warEntry.getName().matches("WEB-INF/lib/[^/]*\\.jar")) {
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
        String[] fullClasspath = buildFullClasspath(extractedDir, aspectClasspath);

        int weavingTime = 0;

        // extract and weave class files
        File extractedClassesDir = new File(extractedDir, "WEB-INF/classes");
        if (extractedClassesDir.exists()) {

            LOG.info("weaving WEB-INF/classes ..");

            File wovenClassesDir = new File(wovenDir, "classes");
            FileUtils.forceMkdir(wovenClassesDir);

            weavingTime += weave(extractedClassesDir.getPath(), aspectPath,
                    wovenClassesDir.getPath(), fullClasspath, ajcArgs);

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

                LOG.info("weaving WEB-INF/lib/" + extractedJarFile.getName() + " ..");

                weavingTime += weave(extractedJarFile.getPath(), aspectPath, new File(wovenLibDir,
                        extractedJarFile.getName()).getPath(), fullClasspath, ajcArgs);
            }

            // write jar files to war output stream
            writeResourcesAndRecurse(wovenLibDir, warOutputStream, "WEB-INF/lib");
        }
        LOG.info("total aspectj weaving time: "
                + FormatUtils.formatAsMinutesAndSeconds(weavingTime));
    }

    private static void extractNextEntry(JarInputStream warInputStream, File extractedDir,
            String name) throws FileNotFoundException, IOException {

        File extractedFile = new File(extractedDir, name);
        FileUtils.forceMkdir(extractedFile.getParentFile());
        StreamUtils.copy(warInputStream, extractedFile);
    }

    private static String[] buildFullClasspath(File extractedDir, String[] aspectClasspath) {

        File extractedLibDir = new File(extractedDir, "WEB-INF/lib");
        List<File> extractedJarFiles = Arrays.asList(extractedLibDir.listFiles());
        Collections.sort(extractedJarFiles);
        File extractedClassesDir = new File(extractedDir, "WEB-INF/classes");

        List<String> classpathElements = new ArrayList<String>();
        classpathElements.addAll(Arrays.asList(aspectClasspath));
        if (extractedClassesDir.exists()) {
            classpathElements.add(extractedClassesDir.getPath());
        }

        if (extractedLibDir.exists()) {
            for (File extractedJarFile : extractedJarFiles) {
                classpathElements.add(extractedJarFile.getPath());
            }
        }
        return classpathElements.toArray(new String[classpathElements.size()]);
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

    private static long weave(String inpath, String[] aspectpath, String output,
            String[] classpath, List<String> ajcArgs) {

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
        if (ajcArgs != null) {
            args.addAll(ajcArgs);
        }

        LOG.debug("ajc " + StringUtils.join(args, " "));

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
            throw new RuntimeException("error(s) occurred during aspectj compilation");
        }
        return duration;
    }

    private static String buildPath(String[] pathElements) {
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
                    // TODO currently not logging TRACE messages (see log4j.properties)
                    // TODO make log4j.properties threshold configurable(?)
                    // and set default to INFO, better also set default to not log
                    // (only console output)
                    LOG.trace(message);
                } else if (message.getKind().isSameOrLessThan(IMessage.TASKTAG)) {
                    LOG.debug(message);
                } else if (message.getKind().isSameOrLessThan(IMessage.WARNING)) {
                    LOG.warn(message);
                } else if (message.getKind().isSameOrLessThan(IMessage.ERROR)) {
                    LOG.error(message);
                } else {
                    LOG.fatal(message);
                }
            }
            return false;
        }
    }
}
