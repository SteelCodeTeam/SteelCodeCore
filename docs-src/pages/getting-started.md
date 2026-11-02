---
title: Getting started
order: 1
---
# Getting started

## Requirements

- Minecraft **1.21.1**, Java **21**
- An Architectury project (`common`, `fabric`, `neoforge`) using Architectury API **13**
- Fabric Loader 0.16+ with Fabric API, or NeoForge 21.1

## 1. Add the dependency

SteelCodeCore is published to the Codeberg Maven registry with sources and Javadoc, so your IDE shows the
documentation. The **last 10 versions** are kept there; jars of every version stay on the release pages.

```groovy
// build.gradle (root), inside subprojects { repositories { ... } }
maven {
    name = 'SteelCode'
    url = 'https://codeberg.org/api/packages/steelcode/maven'
    content { includeGroup 'dev.steelcode' }
}

// common/build.gradle
modCompileOnly "dev.steelcode:steelcodecore-common:0.1.0"

// fabric/build.gradle
modImplementation "dev.steelcode:steelcodecore-fabric:0.1.0"

// neoforge/build.gradle
modImplementation "dev.steelcode:steelcodecore-neoforge:0.1.0"
```

Do not shade or jar-in-jar the library: depend on it so players install it once, as a separate mod.

<div class="note">
Working on the library itself? <code>./gradlew publishToMavenLocal</code> in its repository and add
<code>mavenLocal()</code> to your repositories to test unreleased changes.
</div>

Declare the dependency so the library loads first:

```json
// fabric/src/main/resources/fabric.mod.json
"depends": {
  "steelcodecore": ">=0.1.0"
}
```

```toml
# neoforge/src/main/resources/META-INF/neoforge.mods.toml
[[dependencies.mymod]]
modId = "steelcodecore"
type = "required"
versionRange = "[0.1.0,)"
ordering = "AFTER"
side = "BOTH"
```

## 2. Create your registry

One registry per mod, in `common`. Declare content in static fields and call `register()` once:

```java
package com.example.mymod;

public final class MyContent {
    public static final CoreRegistry REG = SteelCodeCore.registry(MyMod.MOD_ID);

    public static final RegistrySupplier<Block> MARBLE =
            REG.block("marble").copy(Blocks.CALCITE).tool(MiningTool.PICKAXE).build();

    private MyContent() {}

    public static void init() {
        REG.register();
    }
}
```

```java
// common: called by both loader entrypoints
public final class MyMod {
    public static final String MOD_ID = "mymod";

    public static void init() {
        MyContent.init();
    }
}
```

<div class="note">
Static fields are initialized when the class is first touched, so make sure <code>MyContent.init()</code> runs
from your common initializer. Anything declared after <code>register()</code> throws an
<code>IllegalStateException</code>.
</div>

## 3. Set up datagen

The library writes every JSON with a vanilla data provider that behaves identically on both loaders. The usual
setup is to run it once from Fabric into a folder shared by `common`.

```java
// fabric/src/main/java/com/example/mymod/fabric/MyDatagen.java
public final class MyDatagen implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        SteelCodeCoreFabricDatagen.addProviders(generator.createPack(), MyContent.REG);
    }
}
```

```json
// fabric.mod.json
"entrypoints": {
  "main": ["com.example.mymod.fabric.MyModFabric"],
  "fabric-datagen": ["com.example.mymod.fabric.MyDatagen"]
}
```

```groovy
// fabric/build.gradle
loom {
    runs {
        datagen {
            inherit server
            name 'Data Generation'
            vmArg '-Dfabric-api.datagen'
            vmArg "-Dfabric-api.datagen.output-dir=${project(':common').file('src/main/generated')}"
            vmArg '-Dfabric-api.datagen.modid=mymod'
            runDir 'build/datagen'
        }
    }
}

// common/build.gradle
sourceSets.main.resources.srcDir 'src/main/generated'
```

Run it with:

```bash
./gradlew :fabric:runDatagen
```

Prefer NeoForge? Register `SteelCodeCoreNeoForgeDatagen.addProviders(event, MyContent.REG)` on your mod bus's
`GatherDataEvent` and use NeoForge's data run instead.

## 4. Add textures

Datagen writes models that point at textures; you provide the PNGs:

```
common/src/main/resources/assets/mymod/textures/block/marble.png
common/src/main/resources/assets/mymod/textures/item/...
```

Every datagen run logs the textures that are still missing, so you can work through the list. See
[Textures](textures.html) for the names each feature expects.

## 5. Run

```bash
./gradlew :fabric:runClient
./gradlew :neoforge:runClient
```

Your block has its name, model, loot table and pickaxe tag already in place. It shows up in your mod's creative
tab; if you have not created one, it is in no tab until you add `REG.tab(...)` or call
`REG.defaultTab(CreativeModeTabs.BUILDING_BLOCKS)`.
