# Building caport

## Forge 1.8.9

Use a Java 8 JDK and the root Gradle wrapper (Gradle 2.14.1):

```sh
./gradlew setupDecompWorkspace getAssetIndex -x getAssets --max-workers=2
python3 tools/fetch-assets.py
./gradlew setupDecompWorkspace build --max-workers=2
```

Output: `build/libs/caport-forge-1.8.9-1.2.0.jar`.
`./gradlew runClient` starts the Forge development client.

## Fabric 26.3

Use a Java 25 JDK and the separate Fabric wrapper:

```sh
./fabric-26.3/gradlew -p fabric-26.3 build
./fabric-26.3/gradlew -p fabric-26.3 runClient
```

The build pins Fabric Loom 1.18.4, Loader 0.19.5, Fabric API 0.162.0+26.3,
and Gradle 9.7.1. Minecraft 26.3 is distributed with official readable names;
no legacy ForgeGradle or Yarn mapping configuration is used.
Output: `fabric-26.3/build/libs/caport-fabric-26.3-1.2.0.jar`.
The shared `core` sources and regression tests are compiled in both projects;
Forge lifecycle/configuration classes are excluded from the Fabric JAR.

The Fabric wrapper JAR comes from FabricMC's official `fabric-example-mod`
repository; the Gradle distribution is pinned with its official SHA-256 checksum.
Do not commit generated game/dependency files, client worlds, or installable JARs.

## Continuous integration

Review branches build/test both platforms without publishing. Main/tag builds
publish two distinct releases only after both builds pass. Release descriptions
contain changelogs; installation requirements and usage remain in the README.
The Fabric job additionally launches a development client on a dummy Xorg display with Mesa software graphics,
reaches the title screen, and shuts down through the opt-in smoke-test flag.
