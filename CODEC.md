# Codec Helpers

The codec helper classes (`SimpleBuilderCodec`, `CodecToken`, `Types`, `MathTypes`,
`CollectionTypes`) are bundled inside the hy-build plugin JAR and extracted automatically into
`build/generated-sources/hybuild-codec/` when your project is compiled. No extra dependency is
needed — just apply `net.esieben.hybuild` and the classes are available under
`net.esieben.hybuild.codec.*`.

---

## How it works

When the `java` plugin is present, hy-build registers a `generateCodecHelpers` task that writes the
five Java source files into your build directory and registers that directory as an additional
source
root. `compileJava` depends on `generateCodecHelpers`, so the classes are always ready before
compilation starts.

The sources are compiled **in your project**, against your local `HytaleServer.jar`.

---

## Quick start

```java
import net.esieben.hybuild.codec.SimpleBuilderCodec;
import net.esieben.hybuild.codec.Types;
import net.esieben.hybuild.codec.MathTypes;
import net.esieben.hybuild.codec.CollectionTypes;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import lombok.Data;

@Data
public class SpawnConfig {

  public static final BuilderCodec<SpawnConfig> CODEC =
      SimpleBuilderCodec.of(SpawnConfig.class, SpawnConfig::new)
          .append("Name", Types.STRING, SpawnConfig::setName, SpawnConfig::getName)
          .append("Cooldown", Types.INTEGER, SpawnConfig::setCooldown, SpawnConfig::getCooldown)
          .append("Position", MathTypes.VECTOR_3D, SpawnConfig::setPosition,
              SpawnConfig::getPosition)
          .build();

  String name = "default";
  int cooldown = 60;
  Vector3d position = new Vector3d(0, 64, 0);
}
```

Hytale loads configs from `src/main/resources/` automatically when the class is registered as a
config. The server writes a default file on first run if none exists.

---

## Rules

- **Keys must start with an uppercase letter.** `"name"` throws at runtime; `"Name"` is correct.
- Keys must be unique within a codec.
- The factory passed to `SimpleBuilderCodec.of(...)` must produce a blank (zero-argument) instance.
  Lombok's `@Data` on a class with no `final` / `@NonNull` fields generates a no-arg constructor
  automatically.

---

## Type reference

### `Types` — primitives and common JVM types

| Constant / method               | Java type           | JSON / BSON form                              |
|---------------------------------|---------------------|-----------------------------------------------|
| `Types.STRING`                  | `String`            | string                                        |
| `Types.BOOLEAN`                 | `Boolean`           | boolean                                       |
| `Types.BYTE`                    | `Byte`              | integer (range-checked)                       |
| `Types.SHORT`                   | `Short`             | integer (range-checked)                       |
| `Types.INTEGER`                 | `Integer`           | integer                                       |
| `Types.LONG`                    | `Long`              | integer                                       |
| `Types.FLOAT`                   | `Float`             | number                                        |
| `Types.DOUBLE`                  | `Double`            | number                                        |
| `Types.STRING_ARRAY`            | `String[]`          | array of strings                              |
| `Types.BYTE_ARRAY`              | `byte[]`            | BSON binary / base-64 string                  |
| `Types.INT_ARRAY`               | `int[]`             | array of integers                             |
| `Types.LONG_ARRAY`              | `long[]`            | array of integers                             |
| `Types.FLOAT_ARRAY`             | `float[]`           | array of numbers                              |
| `Types.DOUBLE_ARRAY`            | `double[]`          | array of numbers                              |
| `Types.COLOR`                   | `Color`             | `"#rrggbb"` hex string                        |
| `Types.COLOR_ALPHA`             | `ColorAlpha`        | `"#rrggbbaa"` hex string                      |
| `Types.UUID`                    | `UUID`              | BSON UUID binary / base-64 string             |
| `Types.BOOL_DOUBLE_PAIR`        | `BoolDoublePair`    | number, or `"~number"` for the `true` variant |
| `Types.BSON_DOCUMENT`           | `BsonDocument`      | raw object, passed through unchanged          |
| `Types.enumOf(MyEnum.class)`    | `MyEnum`            | enum name string (e.g. `"HARD"`)              |
| `Types.inetSocketAddress(port)` | `InetSocketAddress` | `"host:port"` string                          |

> **`ColorAlpha` constructor order:** `(alpha, red, green, blue)` — not the intuitive RGBA order.
> `new ColorAlpha((byte) 200, (byte) 255, (byte) 128, (byte) 0)` → `"#ff8000c8"` (alpha = `c8`).

> **`float` precision:** `float` values serialize with full double precision in JSON
> (e.g. `3.14f` → `3.140000104904175`). This is normal — the value round-trips correctly.

---

### `MathTypes` — vectors, ranges, shapes

| Constant                       | Java type             | JSON form                                           |
|--------------------------------|-----------------------|-----------------------------------------------------|
| `MathTypes.VECTOR_3D`          | `Vector3d`            | `[x, y, z]`                                         |
| `MathTypes.VECTOR_3I`          | `Vector3i`            | `[x, y, z]`                                         |
| `MathTypes.VECTOR_2D`          | `Vector2d`            | `[x, y]`                                            |
| `MathTypes.RELATIVE_VECTOR_2I` | `RelativeVector2i`    | `{"Vector": {"X": n, "Y": n}, "Relative": bool}`    |
| `MathTypes.RELATIVE_VECTOR_2D` | `RelativeVector2d`    | `{"Vector": {"X": n, "Y": n}, "Relative": bool}`    |
| `MathTypes.RELATIVE_VECTOR_2L` | `RelativeVector2l`    | `{"Vector": {"X": n, "Y": n}, "Relative": bool}`    |
| `MathTypes.RELATIVE_VECTOR_3I` | `RelativeVector3i`    | `{"Vector": {"X":n,"Y":n,"Z":n}, "Relative": bool}` |
| `MathTypes.RELATIVE_VECTOR_3D` | `RelativeVector3d`    | `{"Vector": {"X":n,"Y":n,"Z":n}, "Relative": bool}` |
| `MathTypes.RELATIVE_VECTOR_3L` | `RelativeVector3l`    | `{"Vector": {"X":n,"Y":n,"Z":n}, "Relative": bool}` |
| `MathTypes.FLOAT_RANGE`        | `FloatRange`          | `[min, max]`                                        |
| `MathTypes.INT_RANGE`          | `IntRange`            | `[min, max]` (stored as doubles)                    |
| `MathTypes.SHAPE`              | `Shape` (polymorphic) | `{"Id": "TypeName", ...fields}`                     |
| `MathTypes.BOX`                | `Box`                 | `{"Min": {…}, "Max": {…}}`                          |
| `MathTypes.ELLIPSOID`          | `Ellipsoid`           | `{"RadiusX": n, "RadiusY": n, "RadiusZ": n}`        |
| `MathTypes.CYLINDER`           | `Cylinder`            | `{"Height": n, "RadiusX": n, "RadiusZ": n}`         |
| `MathTypes.ORIGIN_SHAPE`       | `OriginShape<Shape>`  | `{"Origin": {…}, "Shape": {…}}`                     |

> **Polymorphic `Shape`:** the `Id` field is the type discriminator. When decoding, Hytale uses `Id`
> to pick the right codec, so writing `"Id": "Box"` into a `Shape` field works correctly.

---

### `CollectionTypes` — arrays, sets, maps

| Factory                                                    | Java type                  | JSON form                 |
|------------------------------------------------------------|----------------------------|---------------------------|
| `arrayOf(token, T[]::new)`                                 | `T[]`                      | array                     |
| `setOf(token)`                                             | `Set<V>` (HashSet)         | array                     |
| `setOf(token, supplier)`                                   | `Set<V>` (custom)          | array                     |
| `mapOf(token)`                                             | `Map<String, V>` (HashMap) | object                    |
| `mapOf(token, supplier)`                                   | `Map<String, V>` (custom)  | object                    |
| `enumMapOf(MyEnum.class, token)`                           | `Map<MyEnum, V>`           | object (enum name keys)   |
| `int2ObjectMapOf(token, Int2ObjectOpenHashMap::new)`       | `Int2ObjectMap<V>`         | object (stringified keys) |
| `float2ObjectMapOf(token, Float2ObjectOpenHashMap::new)`   | `Float2ObjectMap<V>`       | object (stringified keys) |
| `short2ObjectMapOf(token, Short2ObjectOpenHashMap::new)`   | `Short2ObjectMap<V>`       | object (stringified keys) |
| `object2IntMapOf(token, Object2IntOpenHashMap::new)`       | `Object2IntMap<K>`         | object                    |
| `object2FloatMapOf(token, Object2FloatOpenHashMap::new)`   | `Object2FloatMap<K>`       | object                    |
| `object2DoubleMapOf(token, Object2DoubleOpenHashMap::new)` | `Object2DoubleMap<K>`      | object                    |
| `weightedMapOf(() -> MyEntry.CODEC, new MyEntry[0])`       | `IWeightedMap<T>`          | array of objects          |

> **Weighted maps:** the element type must implement `IWeightedElement` (single method
> `double getWeight()`). Lombok's `@Data` with a `double weight` field satisfies this automatically.
> The empty array (`new MyEntry[0]`) is a Java generics workaround — pass any zero-length typed
> array.

---

## Full JSON reference

The JSON below is the output of a config class that exercises every supported codec type.
Use it as a cheat-sheet when writing config files by hand.

```json
{
  "StandardCommand": "!help",
  "BoolVal": true,
  "ByteVal": 42,
  "ShortVal": 100,
  "StandardDamage": 10,
  "DamageMultiplier": 1.5,
  "FloatVal": 3.140000104904175,
  "LongVal": 9999,
  "PetNames": [
    "Rex",
    "Buddy",
    "Max"
  ],
  "ByteArray": {
    "$binary": "AQID",
    "$type": "00"
  },
  "SomeNumArray": [
    10,
    20,
    30
  ],
  "DoubleArray": [
    1.1,
    2.2,
    3.3
  ],
  "FloatArray": [
    0.10000000149011612,
    0.20000000298023224,
    0.30000001192092896
  ],
  "LongArray": [
    100,
    200,
    300
  ],
  "Color": "#ff8000",
  "ColorAlpha": "#ff8000c8",
  "Uuid": {
    "$binary": "VQ6EAOKbQdSnFkRmVUQAAA==",
    "$type": "04"
  },
  "BoolDoublePair": "~1.5",
  "BsonDocument": {},
  "Difficulty": "Medium",
  "ServerAddress": "localhost:25565",
  "Position": [
    1.0,
    64.0,
    1.0
  ],
  "BlockPos": [
    10,
    64,
    10
  ],
  "Offset2d": [
    0.5,
    0.5
  ],
  "RelVec2i": {
    "Vector": {
      "X": 5,
      "Y": 3
    },
    "Relative": false
  },
  "RelVec2d": {
    "Vector": {
      "X": 1.0,
      "Y": 2.0
    },
    "Relative": true
  },
  "RelVec2l": {
    "Vector": {
      "X": 10,
      "Y": 20
    },
    "Relative": false
  },
  "RelVec3i": {
    "Vector": {
      "X": 1,
      "Y": 2,
      "Z": 3
    },
    "Relative": false
  },
  "RelVec3d": {
    "Vector": {
      "X": 0.5,
      "Y": 1.0,
      "Z": 0.5
    },
    "Relative": true
  },
  "RelVec3l": {
    "Vector": {
      "X": 100,
      "Y": 64,
      "Z": 100
    },
    "Relative": false
  },
  "FloatRange": [
    0.5,
    2.0
  ],
  "IntRange": [
    1.0,
    10.0
  ],
  "Shape": {
    "Id": "Ellipsoid",
    "RadiusX": 1.0,
    "RadiusY": 1.0,
    "RadiusZ": 1.0
  },
  "Box": {
    "Min": {
      "X": 0.0,
      "Y": 0.0,
      "Z": 0.0
    },
    "Max": {
      "X": 1.0,
      "Y": 1.0,
      "Z": 1.0
    }
  },
  "Ellipsoid": {
    "RadiusX": 1.0,
    "RadiusY": 2.0,
    "RadiusZ": 1.5
  },
  "Cylinder": {
    "Height": 2.0,
    "RadiusX": 1.0,
    "RadiusZ": 1.0
  },
  "OriginShape": {
    "Origin": {
      "X": 0.0,
      "Y": 1.0,
      "Z": 0.0
    },
    "Shape": {
      "Id": "Box",
      "Min": {
        "X": 0.0,
        "Y": 0.0,
        "Z": 0.0
      },
      "Max": {
        "X": 1.0,
        "Y": 1.0,
        "Z": 1.0
      }
    }
  },
  "VecArray": [
    [
      1.0,
      0.0,
      0.0
    ],
    [
      0.0,
      1.0,
      0.0
    ]
  ],
  "StringSet": [
    "alpha",
    "beta",
    "gamma"
  ],
  "StringIntMap": {
    "Speed": 5,
    "Damage": 10
  },
  "EnumMap": {
    "Easy": 1,
    "Medium": 2,
    "High": 3
  },
  "Int2ObjMap": {
    "2": "Two",
    "1": "One"
  },
  "Float2ObjMap": {
    "1.0": "One",
    "0.5": "Half"
  },
  "Short2ObjMap": {
    "2": "Two",
    "1": "One"
  },
  "Obj2IntMap": {
    "Apples": 5,
    "Oranges": 3
  },
  "Obj2FloatMap": {
    "Weight": 0.800000011920929,
    "Speed": 1.5
  },
  "Obj2DoubleMap": {
    "Y": 2.0,
    "X": 1.0
  },
  "WeightedMap": [
    {
      "Name": "Common",
      "Weight": 0.7
    },
    {
      "Name": "Rare",
      "Weight": 0.3
    }
  ]
}
```
