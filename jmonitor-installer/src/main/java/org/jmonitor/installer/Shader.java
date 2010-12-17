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

package org.jmonitor.installer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugins.shade.DefaultShader;
import org.apache.maven.plugins.shade.relocation.SimpleRelocator;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// shading aspectj has two benefits
// * doesn't conflict with other versions
// * running shaded ajc doesn't pick up application aspects
// TODO what impact does this have on jmonitor-installer-unshaded

// TODO this was previously part of maven shade plugin configuration

// <relocation>
// <pattern>org.aspectj</pattern>
// <shadedPattern>org.jmonitor.hidden.org.aspectj</shadedPattern>
// </relocation>
// <relocation>
// <!--
// AspectJ org.aspectj.weaver.bcel.BcelShadow uses the text
// "(I)Lorg/aspectj/lang/ProceedingJoinPoint;" which is the method args
// concatenated with the return type and so the maven shade plugin doesn't relocate
// this
// -->
// <pattern>(I)Lorg/aspectj/lang/ProceedingJoinPoint;</pattern>
// <shadedPattern>(I)Lorg/jmonitor/hidden/org/aspectj/lang/ProceedingJoinPoint;</shadedPattern>
// </relocation>

public final class Shader {

    // utility class
    private Shader() {
    }

    public static File shadeInstallerJar(File inputFile, File tempDirectory) throws IOException {

        String filename = inputFile.getName();
        File shadedRuntimeJarFile = new File(tempDirectory, filename);

        List<SimpleRelocator> relocators = new ArrayList<SimpleRelocator>();

        // installer only needs to shade aspectj
        relocators.add(newSimpleRelocator("org.aspectj"));

        DefaultShader shader = new DefaultShader();
        shader.shade(Collections.singleton(inputFile), shadedRuntimeJarFile,
                Collections.emptyList(), relocators, Collections.emptyList());

        return shadedRuntimeJarFile;
    }

    public static File shadeRuntimeJar(File inputFile, File tempDirectory) throws IOException {

        String filename = inputFile.getName();
        File shadedRuntimeJarFile = new File(tempDirectory, filename);

        List<SimpleRelocator> relocators = new ArrayList<SimpleRelocator>();

        // TODO doc very clearly why we cannot shade GWT classes
        // (due to GWT serialization issue)
        
        relocators.add(newSimpleRelocator("org.aspectj"));
        relocators.add(newSimpleRelocator("com.google.common"));
        relocators.add(newSimpleRelocator("org.apache.commons"));
        relocators.add(newSimpleRelocator("org.jasypt"));
        relocators.add(newSimpleRelocator("com.ibm.icu"));
        relocators.add(newSimpleRelocator("ch.qos.logback"));
        relocators.add(newSimpleRelocator("org.slf4j"));

        DefaultShader shader = new DefaultShader();
        shader.shade(Collections.singleton(inputFile), shadedRuntimeJarFile,
                Collections.emptyList(), relocators, Collections.emptyList());

        return shadedRuntimeJarFile;
    }

    public static List<File> shadeProbeJars(List<File> probeJarFiles, File tempDirectory)
            throws IOException {

        final List<File> shadedProbeJarFiles = new ArrayList<File>();

        // shade probe jar files
        if (!probeJarFiles.isEmpty()) {
            // create probes directory under temp directory
            File customProbesDir = new File(tempDirectory, "probes");
            for (File probeJarFile : probeJarFiles) {
                File shadedProbeJarFile = new File(customProbesDir, getShadedName(probeJarFile));
                shadeProbeJar(probeJarFile, shadedProbeJarFile);
                shadedProbeJarFiles.add(shadedProbeJarFile);
            }
        }

        return shadedProbeJarFiles;
    }

    private static String getShadedName(File jarFile) {
        return StringUtils.substringBeforeLast(jarFile.getName(), ".jar") + "-shaded.jar";
    }

    private static void shadeProbeJar(File inputFile, File outputFile) throws IOException {

        FileUtils.forceMkdir(outputFile.getParentFile());

        List<SimpleRelocator> relocators = new ArrayList<SimpleRelocator>();

        // probes should only depend on minimal API
        relocators.add(newSimpleRelocator("org.aspectj"));
        relocators.add(newSimpleRelocator("org.slf4j"));

        DefaultShader shader = new DefaultShader();
        shader.shade(Collections.singleton(inputFile), outputFile, Collections.emptyList(),
                relocators, Collections.emptyList());
    }

    private static SimpleRelocator newSimpleRelocator(String pattern) {
        return new SimpleRelocator(pattern, "org.jmonitor.hidden." + pattern, null);
    }
}
