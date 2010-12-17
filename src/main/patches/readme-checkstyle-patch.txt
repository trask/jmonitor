svn co https://checkstyle.svn.sourceforge.net/svnroot/checkstyle/tags/release5_1 checkstyle-5.1

cd checkstyle-5.1

patch -p0 < ../checkstyle-5.1-add-javadoctype-ignorenested-flag.patch

ant clean build.bindist

mvn install:install-file -DgroupId=checkstyle -DartifactId=checkstyle -Dversion=5.1.1-INTERNAL-1 -Dpackaging=jar -Dfile=target/dist/checkstyle-5.1/checkstyle-5.1.jar -DpomFile=pom.xml

cd ..
rm -rf checkstyle-5.1
