svn co http://svn.codehaus.org/mojo/tags/findbugs-maven-plugin-2.3.1

cd findbugs-maven-plugin-2.3.1

patch -p0 < ../findbugs-maven-plugin-2.3.1-allow-resources-from-build-tools-sibling-module.patch

mvn install

cd ..

rm -rf findbugs-maven-plugin-2.3.1
