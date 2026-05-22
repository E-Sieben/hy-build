package net.esieben.hybuild.codec;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.codecs.BsonDocumentCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.InetSocketAddressCodec;
import com.hypixel.hytale.codec.codecs.UUIDBinaryCodec;
import com.hypixel.hytale.common.tuple.BoolDoublePair;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.ColorAlpha;
import com.hypixel.hytale.server.core.codec.BoolDoublePairCodec;
import com.hypixel.hytale.server.core.codec.protocol.ColorAlphaCodec;
import com.hypixel.hytale.server.core.codec.protocol.ColorCodec;
import java.net.InetSocketAddress;
import java.util.UUID;
import org.bson.BsonDocument;

/**
 * Pre-built {@link CodecToken} constants for primitive, color, and common JVM types.
 *
 * <p>Pass any constant directly as the codec argument of a {@link SimpleBuilderCodec} field:
 * <pre>{@code
 * SimpleBuilderCodec.of(MyData.class, MyData::new)
 *     .append("Name",    Types.STRING,  MyData::setName,  MyData::getName)
 *     .append("Count",   Types.INTEGER, MyData::setCount, MyData::getCount)
 *     .append("Enabled", Types.BOOLEAN, MyData::setEnabled, MyData::isEnabled)
 *     .build();
 * }</pre>
 *
 * <p>For types not listed here, see:
 * <ul>
 *   <li>{@link MathTypes} — vectors, ranges, and shapes
 *   <li>{@link CollectionTypes} — arrays, sets, maps, and weighted maps
 * </ul>
 *
 * <p>For a type with no pre-built constant, pass a codec lambda inline:
 * <pre>{@code
 * .append("Data", () -> myCustomCodec, ...)
 * }</pre>
 */
public final class Types {

  /**
   * Encodes as a BSON / JSON string.
   */
  public static final CodecToken<String> STRING = () -> Codec.STRING;

  // -------------------------------------------------------------------------------------------
  // Primitives and their boxed equivalents
  // -------------------------------------------------------------------------------------------
  /**
   * Encodes as a BSON / JSON boolean.
   */
  public static final CodecToken<Boolean> BOOLEAN = () -> Codec.BOOLEAN;
  /**
   * Encodes as a BSON / JSON integer; validates that the value fits in one byte.
   */
  public static final CodecToken<Byte> BYTE = () -> Codec.BYTE;
  /**
   * Encodes as a BSON / JSON integer; validates that the value fits in a short.
   */
  public static final CodecToken<Short> SHORT = () -> Codec.SHORT;
  /**
   * Encodes as a BSON / JSON integer.
   */
  public static final CodecToken<Integer> INTEGER = () -> Codec.INTEGER;
  /**
   * Encodes as a BSON / JSON number. Supports {@code "NaN"}, {@code "Infinity"},
   * {@code "-Infinity"} string forms.
   */
  public static final CodecToken<Double> DOUBLE = () -> Codec.DOUBLE;
  /**
   * Encodes as a BSON / JSON number. Supports {@code "NaN"}, {@code "Infinity"},
   * {@code "-Infinity"} string forms.
   */
  public static final CodecToken<Float> FLOAT = () -> Codec.FLOAT;
  /**
   * Encodes as a BSON / JSON integer.
   */
  public static final CodecToken<Long> LONG = () -> Codec.LONG;
  /**
   * Encodes as a BSON / JSON array of strings.
   */
  public static final CodecToken<String[]> STRING_ARRAY = () -> Codec.STRING_ARRAY;

  // -------------------------------------------------------------------------------------------
  // Primitive arrays
  // -------------------------------------------------------------------------------------------
  /**
   * Encodes as a BSON binary / JSON base-64 string.
   */
  public static final CodecToken<byte[]> BYTE_ARRAY = () -> Codec.BYTE_ARRAY;
  /**
   * Encodes as a BSON / JSON array of integers.
   */
  public static final CodecToken<int[]> INT_ARRAY = () -> Codec.INT_ARRAY;
  /**
   * Encodes as a BSON / JSON array of numbers.
   */
  public static final CodecToken<double[]> DOUBLE_ARRAY = () -> Codec.DOUBLE_ARRAY;
  /**
   * Encodes as a BSON / JSON array of numbers.
   */
  public static final CodecToken<float[]> FLOAT_ARRAY = () -> Codec.FLOAT_ARRAY;
  /**
   * Encodes as a BSON / JSON array of integers.
   */
  public static final CodecToken<long[]> LONG_ARRAY = () -> Codec.LONG_ARRAY;
  /**
   * Encodes {@link Color} as a hex or RGB string (e.g. {@code "#ff8800"} or
   * {@code "rgb(255,136,0)"}).
   */
  public static final CodecToken<Color> COLOR = ColorCodec::new;

  // -------------------------------------------------------------------------------------------
  // Colors
  // -------------------------------------------------------------------------------------------
  /**
   * Encodes {@link ColorAlpha} as a hex or RGBA string (e.g. {@code "#ff8800cc"} or
   * {@code "rgba(255,136,0,0.8)"}).
   */
  public static final CodecToken<ColorAlpha> COLOR_ALPHA = ColorAlphaCodec::new;
  /**
   * Encodes {@link UUID} as a BSON UUID binary (subtype {@code UUID_STANDARD}) or a base-64 string
   * in JSON.
   */
  public static final CodecToken<UUID> UUID = UUIDBinaryCodec::new;

  // -------------------------------------------------------------------------------------------
  // Misc JVM types
  // -------------------------------------------------------------------------------------------
  /**
   * Encodes {@link BoolDoublePair} as a number, optionally prefixed with {@code ~} to set the
   * boolean flag (e.g. {@code 1.5} → {@code false, 1.5}; {@code "~1.5"} → {@code true, 1.5}).
   */
  public static final CodecToken<BoolDoublePair> BOOL_DOUBLE_PAIR = BoolDoublePairCodec::new;
  /**
   * Passes a raw {@link BsonDocument} through the codec layer unchanged. Useful for fields whose
   * schema is defined dynamically at runtime.
   */
  public static final CodecToken<BsonDocument> BSON_DOCUMENT = BsonDocumentCodec::new;

  private Types() {
  }

  // -------------------------------------------------------------------------------------------
  // Factories for parameterized types
  // -------------------------------------------------------------------------------------------

  /**
   * Returns a token for an enum type.
   *
   * <p>Enum constants are mapped to strings using the enum's natural name style (typically
   * {@code UPPER_SNAKE_CASE}). Example usage:
   * <pre>{@code
   * .append("Direction", Types.enumOf(Direction.class), MyData::setDirection, MyData::getDirection)
   * }</pre>
   *
   * @param enumClass the enum class to create a codec for
   * @param <E>       the enum type
   */
  public static <E extends Enum<E>> CodecToken<E> enumOf(Class<E> enumClass) {
    return () -> new EnumCodec<>(enumClass);
  }

  /**
   * Returns a token for {@link InetSocketAddress}, decoded from a {@code "host:port"} string.
   *
   * <p>{@code defaultPort} is used when the serialized string omits the port. Example:
   * <pre>{@code
   * .append("ServerAddress", Types.inetSocketAddress(25565), MyData::setAddress, MyData::getAddress)
   * }</pre>
   *
   * @param defaultPort fallback port when the serialized value contains only a host
   */
  public static CodecToken<InetSocketAddress> inetSocketAddress(int defaultPort) {
    return () -> new InetSocketAddressCodec(defaultPort);
  }
}
