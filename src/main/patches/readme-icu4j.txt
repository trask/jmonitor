http://userguide.icu-project.org/packaging-icu4j

svn co http://source.icu-project.org/repos/icu/icu4j/tags/release-4-6 icu-4.6

cd icu-4.6

ant normalizer
ant moduleJar

# verify the build
ant normalizerTests
java -classpath out/module/tests:out/module/bin com.ibm.icu.dev.test.TestAll -nothrow -w

# install the build
mvn install:install-file -DgroupId=com.ibm.icu -DartifactId=icu4j-normalizer -Dversion=4.6 -Dpackaging=jar -Dfile=out/module/lib/icu4j-module.jar -DgeneratePom=true

# download and install sources jar (it will be a superset of the normalizer module which is fine)
curl -O http://repo2.maven.org/maven2/com/ibm/icu/icu4j/4.6/icu4j-4.6-sources.jar

mvn install:install-file -DgroupId=com.ibm.icu -DartifactId=icu4j-normalizer -Dversion=4.6 -Dpackaging=jar -Dclassifier=sources -Dfile=icu4j-4.6-sources.jar

cd ..
rm -rf icu-4.6
