package net.esieben.hybuild.codec;

import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.EnumMapCodec;
import com.hypixel.hytale.codec.codecs.map.Float2ObjectMapCodec;
import com.hypixel.hytale.codec.codecs.map.Int2ObjectMapCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.codecs.map.Object2DoubleMapCodec;
import com.hypixel.hytale.codec.codecs.map.Object2FloatMapCodec;
import com.hypixel.hytale.codec.codecs.map.Object2IntMapCodec;
import com.hypixel.hytale.codec.codecs.map.Short2ObjectMapCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.common.map.IWeightedElement;
import com.hypixel.hytale.common.map.IWeightedMap;
import com.hypixel.hytale.server.core.codec.WeightedMapCodec;
import it.unimi.dsi.fastutil.floats.Float2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Factory methods producing {@link CodecToken} instances for collection types.
 *
 * <p>Standard Java collections ({@link Set}, {@link Map}) have convenience overloads that default
 * to {@link HashSet} / {@link HashMap} respectively. FastUtil primitive-keyed maps always require
 * an explicit supplier, since there is no single canonical implementation (e.g.
 * {@code Int2ObjectOpenHashMap::new} vs. {@code Int2ObjectArrayMap::new}).
 *
 * @see Types for primitives and common JVM types
 * @see MathTypes for vectors, ranges, and shapes
 */
public final class CollectionTypes {

  private CollectionTypes() {
  }

  // -------------------------------------------------------------------------------------------
  // Arrays
  // -------------------------------------------------------------------------------------------

  /**
   * Returns a token for a generic {@code T[]} array, encoding each element with the given token.
   *
   * <p>An array constructor reference is required so the codec can allocate arrays of the correct
   * component type at runtime:
   * <pre>{@code
   * .append("Tags",   CollectionTypes.arrayOf(Types.STRING, String[]::new), ...)
   * .append("Points", CollectionTypes.arrayOf(MathTypes.VECTOR_3D, Vector3d[]::new), ...)
   * }</pre>
   *
   * @param element          token for the array's element type
   * @param arrayConstructor method reference of the form {@code MyType[]::new}
   * @param <T>              element type
   */
  public static <T> CodecToken<T[]> arrayOf(CodecToken<T> element,
      IntFunction<T[]> arrayConstructor) {
    return () -> new ArrayCodec<>(element.getCodec(), arrayConstructor);
  }

  // -------------------------------------------------------------------------------------------
  // Sets
  // -------------------------------------------------------------------------------------------

  /**
   * Returns a token for a {@link Set}{@code <V>} backed by {@link HashSet}.
   *
   * <pre>{@code
   * .append("Flags", CollectionTypes.setOf(Types.STRING), MyData::setFlags, MyData::getFlags)
   * }</pre>
   *
   * @param element token for the set's element type
   * @param <V>     element type
   */
  public static <V> CodecToken<Set<V>> setOf(CodecToken<V> element) {
    return () -> new SetCodec<>(element.getCodec(), HashSet::new, false);
  }

  /**
   * Returns a token for a {@link Set}{@code <V>} backed by a custom supplier.
   *
   * <p>Use this when insertion order or a specific {@link Set} implementation matters:
   * <pre>{@code
   * CollectionTypes.setOf(Types.STRING, LinkedHashSet::new)
   * }</pre>
   *
   * @param element  token for the set's element type
   * @param supplier factory for the concrete {@link Set} instance
   * @param <V>      element type
   */
  public static <V> CodecToken<Set<V>> setOf(CodecToken<V> element,
      Supplier<? extends Set<V>> supplier) {
    return () -> new SetCodec<>(element.getCodec(), supplier, false);
  }

  // -------------------------------------------------------------------------------------------
  // String-keyed Maps
  // -------------------------------------------------------------------------------------------

  /**
   * Returns a token for a {@link Map}{@code <String, V>} backed by {@link HashMap}.
   *
   * <pre>{@code
   * .append("Properties", CollectionTypes.mapOf(Types.INTEGER), MyData::setProps, MyData::getProps)
   * }</pre>
   *
   * @param value token for the map's value type
   * @param <V>   value type
   */
  public static <V> CodecToken<Map<String, V>> mapOf(CodecToken<V> value) {
    return () -> new MapCodec<>(value.getCodec(), HashMap::new);
  }

  /**
   * Returns a token for a {@link Map}{@code <String, V>} backed by a custom supplier.
   *
   * <p>Use this when insertion order or a specific {@link Map} implementation matters:
   * <pre>{@code
   * CollectionTypes.mapOf(Types.INTEGER, LinkedHashMap::new)
   * }</pre>
   *
   * @param value    token for the map's value type
   * @param supplier factory for the concrete {@link Map} instance
   * @param <V>      value type
   */
  public static <V> CodecToken<Map<String, V>> mapOf(CodecToken<V> value,
      Supplier<? extends Map<String, V>> supplier) {
    return () -> new MapCodec<>(value.getCodec(), supplier);
  }

  // -------------------------------------------------------------------------------------------
  // Enum-keyed Maps
  // -------------------------------------------------------------------------------------------

  /**
   * Returns a token for a {@link Map} whose keys are enum constants.
   *
   * <p>Keys are serialized using the enum's natural name style (typically
   * {@code UPPER_SNAKE_CASE}).
   * Missing keys are decoded as absent entries in the map rather than throwing:
   * <pre>{@code
   * .append("Bonuses", CollectionTypes.enumMapOf(Stat.class, Types.DOUBLE),
   *         MyData::setBonuses, MyData::getBonuses)
   * }</pre>
   *
   * @param keyClass class of the enum used as map keys
   * @param value    token for the map's value type
   * @param <K>      enum key type
   * @param <V>      value type
   */
  public static <K extends Enum<K>, V> CodecToken<Map<K, V>> enumMapOf(Class<K> keyClass,
      CodecToken<V> value) {
    return () -> new EnumMapCodec<>(keyClass, value.getCodec());
  }

  // -------------------------------------------------------------------------------------------
  // FastUtil primitive-keyed Maps
  // -------------------------------------------------------------------------------------------

  /**
   * Returns a token for a FastUtil {@link Int2ObjectMap}{@code <V>}.
   *
   * <p>Keys are serialized as stringified integers. Supply the desired implementation:
   * <pre>{@code
   * CollectionTypes.int2ObjectMapOf(Types.STRING, Int2ObjectOpenHashMap::new)
   * }</pre>
   *
   * @param value    token for the map's value type
   * @param supplier factory for the concrete {@link Int2ObjectMap} instance
   * @param <V>      value type
   */
  public static <V> CodecToken<Int2ObjectMap<V>> int2ObjectMapOf(CodecToken<V> value,
      Supplier<Int2ObjectMap<V>> supplier) {
    return () -> new Int2ObjectMapCodec<>(value.getCodec(), supplier);
  }

  /**
   * Returns a token for a FastUtil {@link Float2ObjectMap}{@code <V>}.
   *
   * <p>Keys are serialized as stringified floats. Supply the desired implementation:
   * <pre>{@code
   * CollectionTypes.float2ObjectMapOf(Types.STRING, Float2ObjectOpenHashMap::new)
   * }</pre>
   *
   * @param value    token for the map's value type
   * @param supplier factory for the concrete {@link Float2ObjectMap} instance
   * @param <V>      value type
   */
  public static <V> CodecToken<Float2ObjectMap<V>> float2ObjectMapOf(CodecToken<V> value,
      Supplier<Float2ObjectMap<V>> supplier) {
    return () -> new Float2ObjectMapCodec<>(value.getCodec(), supplier);
  }

  /**
   * Returns a token for a FastUtil {@link Short2ObjectMap}{@code <V>}.
   *
   * <p>Keys are serialized as stringified shorts. Supply the desired implementation:
   * <pre>{@code
   * CollectionTypes.short2ObjectMapOf(Types.STRING, Short2ObjectOpenHashMap::new)
   * }</pre>
   *
   * @param value    token for the map's value type
   * @param supplier factory for the concrete {@link Short2ObjectMap} instance
   * @param <V>      value type
   */
  public static <V> CodecToken<Short2ObjectMap<V>> short2ObjectMapOf(CodecToken<V> value,
      Supplier<Short2ObjectMap<V>> supplier) {
    return () -> new Short2ObjectMapCodec<>(value.getCodec(), supplier);
  }

  // -------------------------------------------------------------------------------------------
  // FastUtil object-keyed primitive-value Maps
  // -------------------------------------------------------------------------------------------

  /**
   * Returns a token for a FastUtil {@link Object2IntMap}{@code <K>}.
   *
   * <p>Keys are encoded to strings via the provided key token's codec. Supply the desired
   * implementation:
   * <pre>{@code
   * CollectionTypes.object2IntMapOf(Types.STRING, Object2IntOpenHashMap::new)
   * }</pre>
   *
   * @param key      token for the map's key type (must encode to a string representation)
   * @param supplier factory for the concrete {@link Object2IntMap} instance
   * @param <K>      key type
   */
  public static <K> CodecToken<Object2IntMap<K>> object2IntMapOf(CodecToken<K> key,
      Supplier<Object2IntMap<K>> supplier) {
    return () -> new Object2IntMapCodec<>(key.getCodec(), supplier);
  }

  /**
   * Returns a token for a FastUtil {@link Object2FloatMap}{@code <K>}.
   *
   * <p>Keys are encoded to strings via the provided key token's codec. Supply the desired
   * implementation:
   * <pre>{@code
   * CollectionTypes.object2FloatMapOf(Types.STRING, Object2FloatOpenHashMap::new)
   * }</pre>
   *
   * @param key      token for the map's key type (must encode to a string representation)
   * @param supplier factory for the concrete {@link Object2FloatMap} instance
   * @param <K>      key type
   */
  public static <K> CodecToken<Object2FloatMap<K>> object2FloatMapOf(CodecToken<K> key,
      Supplier<Object2FloatMap<K>> supplier) {
    return () -> new Object2FloatMapCodec<>(key.getCodec(), supplier);
  }

  /**
   * Returns a token for a FastUtil {@link Object2DoubleMap}{@code <K>}.
   *
   * <p>Keys are encoded to strings via the provided key token's codec. Supply the desired
   * implementation:
   * <pre>{@code
   * CollectionTypes.object2DoubleMapOf(Types.STRING, Object2DoubleOpenHashMap::new)
   * }</pre>
   *
   * @param key      token for the map's key type (must encode to a string representation)
   * @param supplier factory for the concrete {@link Object2DoubleMap} instance
   * @param <K>      key type
   */
  public static <K> CodecToken<Object2DoubleMap<K>> object2DoubleMapOf(CodecToken<K> key,
      Supplier<Object2DoubleMap<K>> supplier) {
    return () -> new Object2DoubleMapCodec<>(key.getCodec(), supplier);
  }

  // -------------------------------------------------------------------------------------------
  // Weighted Maps
  // -------------------------------------------------------------------------------------------

  /**
   * Returns a token for an {@link IWeightedMap}{@code <T>}, decoded from a JSON array.
   *
   * <p>Each array element is decoded with the given element token; the element's own
   * {@link IWeightedElement#getWeight()} is used to build the weighted distribution. An empty typed
   * array is required so the codec can allocate internal arrays correctly:
   * <pre>{@code
   * .append("Loot", CollectionTypes.weightedMapOf(
   *         () -> LootEntry.CODEC, new LootEntry[0]),
   *         MyData::setLoot, MyData::getLoot)
   * }</pre>
   *
   * @param element    token for the weighted element type
   * @param emptyArray a zero-length array of the element type, e.g. {@code new MyType[0]}
   * @param <T>        element type, must implement {@link IWeightedElement}
   */
  public static <T extends IWeightedElement> CodecToken<IWeightedMap<T>> weightedMapOf(
      CodecToken<T> element, T[] emptyArray) {
    return () -> new WeightedMapCodec<>(element.getCodec(), emptyArray);
  }
}
