# Aigents Java Core Platform

## Pre-requisites
1. Ensure that the following software is installed for build and test purposes:
- Java JDK 6,7,8 (any version, Oracle Java strongly recommended) - used for build and run
- PHP (any version with cURL for PHP installed) - used for tests
- Python (Python 2 or Python 3) - used for tests
2. Clone this repostory as at **aigents-java**
3. Create folder **lib** in the repository root at **aigents-java**
4. Download the following **jar** libraries and class files to **lib** folder:
- **mail.jar** ([Java Mail 1.4.7](http://www.java2s.com/Code/Jar/j/Downloadjavaxmailapi147jar.htm)) - needed for Aigents e-mail operations
- **servlet.jar** ([Java Servlet 2.5](http://www.java2s.com/Code/Jar/s/Downloadservlet25jar.htm)) - needed for Aigents/Webstructor exposure as a servlet
- **javax.json-1.0.2.jar** ([JSON Libarary](http://www.java2s.com/Code/Jar/j/Downloadjavaxjson102jar.htm)) - needed for Aigents JSON interoperability
- **pdfbox-app-2.0.0.jar** ([Apache PDFBox Application](https://pdfbox.apache.org/) [version 2.0.0](https://mvnrepository.com/artifact/org.apache.pdfbox/pdfbox-app/2.0.0)) - needed to parse PDF documents
- **jfxrt.jar** ([Java FX](https://www.oracle.com/technetwork/java/javafx2-archive-download-1939373.html)) - needed for Aigents Desktop User Interface
- **appbundler-1.0.jar** ([Java App Bundler for Mac](https://docs.oracle.com/javase/7/docs/technotes/guides/jweb/packagingAppsForMac.html)) - needed for Aigents OSX Application
- **org** ([Jar Class Loader](https://github.com/raisercostin/eclipse-jarinjarloader)) - need for launching Aigents from Jar file automatically, have to be placed in the following structure under **lib** root: 
```
org
└── eclipse
    └── jdt
        └── internal
            └── jarinjarloader
                ├── JIJConstants.class
                ├── JarRsrcLoader$ManifestInfo.class
                ├── JarRsrcLoader.class
                ├── RsrcURLConnection.class
                ├── RsrcURLStreamHandler.class
                └── RsrcURLStreamHandlerFactory.class
```

## Build instructions
1. Run build script with Linux shell as **sh build.sh**
2. Run test script with Linux shell as *sh test.sh*
- See test pre-requisites in the "test" shell script file
- Successful tests pass will be indicated with **SUCCESS!** in the very end
3. Pick built Java jar file as **Aigents.jar**
4. Run Aigents GUI with java command as **java -jar Aigents.jar**
5. If needed, bundle Aigents Desktop App for Mac accordingly to [instructions](https://docs.oracle.com/javase/7/docs/technotes/guides/jweb/packagingAppsForMac.html)
6. If needed, build Aigents Desktop for Windows Executable from Jar using [Excelsior JET Technology](https://www.excelsiorjet.com/kb/35/howto-create-a-single-exe-from-your-java-application)  
7. If needed, configure Aigents Power Server following [Aigents Server requirements configuration, operation and API use](https://aigents.com/download/latest/readme.html) instructions

