package net.esieben.hybuild.codec;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A fluent facade over {@link BuilderCodec.Builder} that simplifies field registration on a
 * {@link BuilderCodec}.
 *
 * <p>Fields are registered via {@link #append append}, which takes a {@link CodecToken} to
 * identify the field type. All type verification happens at compile time: the token's type
 * parameter constrains the setter and getter, so passing mismatched accessors is a compile error.
 * Predefined tokens for all supported built-in types live in {@link Types}; custom codecs can be
 * passed directly as a lambda ({@code () -> myCodec}), and enums use
 * {@link Types#enumOf Types.enumOf(Class)}.
 *
 * <p>Three {@code append} overloads exist with progressively more options:
 * <ol>
 *   <li>key + token + accessor pair (no doc, not required)</li>
 *   <li>key + doc + token + accessor pair (not required)</li>
 *   <li>key + doc + required flag + token + accessor pair</li>
 * </ol>
 *
 * <p>Call {@link #build()} once all fields are registered to obtain the finished
 * {@link BuilderCodec}.
 *
 * @param <T> the type this codec handles
 */
public class SimpleBuilderCodec<T> {

  private final BuilderCodec.Builder<T> builder;

  private SimpleBuilderCodec(Class<T> clazz, Supplier<T> factory) {
    this.builder = BuilderCodec.builder(clazz, factory);
  }

  /**
   * Creates a new {@code SimpleBuilderCodec} targeting {@code clazz}, using {@code factory} to
   * construct blank instances during decoding.
   */
  public static <T> SimpleBuilderCodec<T> of(Class<T> clazz, Supplier<T> factory) {
    return new SimpleBuilderCodec<>(clazz, factory);
  }

  // ── append ───────────────────────────────────────────────────────────────

  /**
   * Registers a field.
   */
  public <F> SimpleBuilderCodec<T> append(
      String key, CodecToken<F> token,
      BiConsumer<T, F> setter, Function<T, F> getter) {
    return addField(key, "", false, token.getCodec(), setter, getter);
  }

  /**
   * Registers a field with documentation.
   */
  public <F> SimpleBuilderCodec<T> append(
      String key, String doc, CodecToken<F> token,
      BiConsumer<T, F> setter, Function<T, F> getter) {
    return addField(key, doc, false, token.getCodec(), setter, getter);
  }

  /**
   * Registers a field with documentation and an optional required constraint.
   *
   * @param required when {@code true}, decoding will fail if this key is absent
   */
  public <F> SimpleBuilderCodec<T> append(
      String key, String doc, boolean required, CodecToken<F> token,
      BiConsumer<T, F> setter, Function<T, F> getter) {
    return addField(key, doc, required, token.getCodec(), setter, getter);
  }

  // ── Terminal ─────────────────────────────────────────────────────────────

  /**
   * Finalizes field registration and returns the finished {@link BuilderCodec}.
   */
  public BuilderCodec<T> build() {
    return builder.build();
  }

  // ── Private helpers ──────────────────────────────────────────────────────

  private <F> SimpleBuilderCodec<T> addField(
      String key, String doc, boolean required, Codec<F> codec,
      BiConsumer<T, F> setter, Function<T, F> getter) {
    if (doc.isEmpty()) {
      builder.append(new KeyedCodec<>(key, codec, required), setter, getter).add();
    } else {
      builder.append(new KeyedCodec<>(key, codec, required), setter, getter).documentation(doc)
          .add();
    }
    return this;
  }
}
