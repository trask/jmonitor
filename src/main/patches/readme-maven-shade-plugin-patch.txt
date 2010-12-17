svn co http://svn.apache.org/repos/asf/maven/plugins/tags/maven-shade-plugin-1.3.1

cd maven-shade-plugin-1.3.1

patch -p0 < ../maven-shade-plugin-1.3.1-arbitrary-string-replacement.patch

mvn install

cd ..

rm -rf maven-shade-plugin-1.3.1
