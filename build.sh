#TODO pre-fill lib with jars
rm Aigents.jar
mkdir bin
cp -r resources/* bin
cp -r lib/* bin
javac -cp ".:./lib/javax.json-1.0.2.jar:./lib/mail.jar:./lib/servlet.jar:./lib/jfxrt.jar" -d ./bin -target 1.6 -source 1.6 -Xlint:deprecation $(find ./net/* | grep .java)
cd bin
jar cvfm ../Aigents.jar manifest.mf *
cd ..
