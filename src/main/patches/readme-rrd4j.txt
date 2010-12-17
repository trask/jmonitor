curl -O https://rrd4j.dev.java.net/files/documents/4901/51531/rrd4j-2.0.5.tar.gz

gunzip rrd4j-2.0.5.tar.gz

mvn install:install-file -DgroupId=net.java.dev.rrd4j -DartifactId=rrd4j -Dversion=2.0.5 -Dpackaging=jar -Dfile=lib/rrd4j-2.0.5.jar -DgeneratePom=true

cd src
jar cf ../rrd4j-2.0.5-sources.jar .
cd ..

mvn install:install-file -DgroupId=net.java.dev.rrd4j -DartifactId=rrd4j -Dversion=2.0.5  -Dpackaging=jar -Dclassifier=sources -Dfile=rrd4j-2.0.5-sources.jar
