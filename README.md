# Aigents Java Core Platform

## Pre-requisites
1. Clone this repostory as at **aigents-java**
2. Create folder **lib** in the repository root at **aigents-java**
3. Download the following **jar** libraries:
- **mail.jar** (Java Mail)
- **servlet.jar** (Java Servlet)
- **javax.json-1.0.2.jar** (JSON Libarary)
- **jfxrt.jar** (Java FX)
- **appbundler-1.0.jar** (Java App Bundler for Mac)
- **org** (Jar Class Loader)
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
1. Run build script with Linux shell as **sh build**
2. Pick built Java jar file as **Aigents.jar**
3. Run Aigents GUI with java command as **java -jar Aigents.jar**

## Configuration instructions
If needed, configure personal or server Aigents application following [Aigents Server requirements configuration, operation and API use](https://aigents.com/download/latest/readme.html) instructions
