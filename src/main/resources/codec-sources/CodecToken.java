package net.esieben.hybuild.codec;

import com.hypixel.hytale.codec.Codec;

/**
 * A typed handle that carries a {@link Codec}{@code <F>} and pins the type parameter {@code F} at
 * the call site, enabling compile-time verification of setter and getter compatibility.
 *
 * <p>Predefined tokens for all built-in types are available in {@link Types}. Custom codecs can
 * be passed as a lambda directly ({@code () -> myCodec}); for enums use
 * {@link Types#enumOf Types.enumOf(Class)}.
 *
 * @param <F> the field value type
 */
@FunctionalInterface
public interface CodecToken<F> {

  Codec<F> getCodec();
}
