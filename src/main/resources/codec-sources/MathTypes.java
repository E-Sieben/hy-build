package net.esieben.hybuild.codec;

import com.hypixel.hytale.math.codec.FloatRangeArrayCodec;
import com.hypixel.hytale.math.codec.IntRangeArrayCodec;
import com.hypixel.hytale.math.codec.Vector2dArrayCodec;
import com.hypixel.hytale.math.codec.Vector3dArrayCodec;
import com.hypixel.hytale.math.codec.Vector3iArrayCodec;
import com.hypixel.hytale.math.range.FloatRange;
import com.hypixel.hytale.math.range.IntRange;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.shape.Cylinder;
import com.hypixel.hytale.math.shape.Ellipsoid;
import com.hypixel.hytale.math.shape.OriginShape;
import com.hypixel.hytale.math.shape.Shape;
import com.hypixel.hytale.math.vector.Vector2d;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.math.vector.relative.RelativeVector2d;
import com.hypixel.hytale.math.vector.relative.RelativeVector2i;
import com.hypixel.hytale.math.vector.relative.RelativeVector2l;
import com.hypixel.hytale.math.vector.relative.RelativeVector3d;
import com.hypixel.hytale.math.vector.relative.RelativeVector3i;
import com.hypixel.hytale.math.vector.relative.RelativeVector3l;
import com.hypixel.hytale.server.core.codec.ShapeCodecs;

/**
 * Pre-built {@link CodecToken} constants for Hytale math types: vectors, ranges, and shapes.
 *
 * <p>Example usage inside a {@link SimpleBuilderCodec}:
 * <pre>{@code
 * SimpleBuilderCodec.of(MyData.class, MyData::new)
 *     .append("Position", MathTypes.VECTOR_3D,          MyData::setPosition, MyData::getPosition)
 *     .append("Origin",   MathTypes.RELATIVE_VECTOR_3D, MyData::setOrigin,   MyData::getOrigin)
 *     .append("Radius",   MathTypes.FLOAT_RANGE,        MyData::setRadius,   MyData::getRadius)
 *     .append("Bounds",   MathTypes.BOX,                MyData::setBounds,   MyData::getBounds)
 *     .build();
 * }</pre>
 *
 * @see Types for primitives and common JVM types
 * @see CollectionTypes for array, set, map, and weighted-map factories
 */
public final class MathTypes {

  /**
   * Encodes {@link Vector3d} as {@code [x, y, z]}.
   */
  public static final CodecToken<Vector3d> VECTOR_3D = Vector3dArrayCodec::new;

  // -------------------------------------------------------------------------------------------
  // Absolute vectors — encoded as a compact JSON array, e.g. [x, y, z]
  // -------------------------------------------------------------------------------------------
  /**
   * Encodes {@link Vector3i} as {@code [x, y, z]}.
   */
  public static final CodecToken<Vector3i> VECTOR_3I = Vector3iArrayCodec::new;
  /**
   * Encodes {@link Vector2d} as {@code [x, y]}.
   */
  public static final CodecToken<Vector2d> VECTOR_2D = Vector2dArrayCodec::new;
  /**
   * Encodes {@link RelativeVector2i}; each component may be absolute or relative to a base.
   */
  public static final CodecToken<RelativeVector2i> RELATIVE_VECTOR_2I = () -> RelativeVector2i.CODEC;

  // -------------------------------------------------------------------------------------------
  // Relative vectors — each component can be an absolute value or relative (prefixed with ~)
  //
  // Example JSON:  { "x": 10, "y": "~5", "z": 0 }
  //   x = absolute 10, y = base + 5, z = absolute 0
  // -------------------------------------------------------------------------------------------
  /**
   * Encodes {@link RelativeVector2d}; each component may be absolute or relative to a base.
   */
  public static final CodecToken<RelativeVector2d> RELATIVE_VECTOR_2D = () -> RelativeVector2d.CODEC;
  /**
   * Encodes {@link RelativeVector2l}; each component may be absolute or relative to a base.
   */
  public static final CodecToken<RelativeVector2l> RELATIVE_VECTOR_2L = () -> RelativeVector2l.CODEC;
  /**
   * Encodes {@link RelativeVector3i}; each component may be absolute or relative to a base.
   */
  public static final CodecToken<RelativeVector3i> RELATIVE_VECTOR_3I = () -> RelativeVector3i.CODEC;
  /**
   * Encodes {@link RelativeVector3d}; each component may be absolute or relative to a base.
   */
  public static final CodecToken<RelativeVector3d> RELATIVE_VECTOR_3D = () -> RelativeVector3d.CODEC;
  /**
   * Encodes {@link RelativeVector3l}; each component may be absolute or relative to a base.
   */
  public static final CodecToken<RelativeVector3l> RELATIVE_VECTOR_3L = () -> RelativeVector3l.CODEC;
  /**
   * Encodes {@link FloatRange} as {@code [min, max]}.
   */
  public static final CodecToken<FloatRange> FLOAT_RANGE = FloatRangeArrayCodec::new;

  // -------------------------------------------------------------------------------------------
  // Ranges — encoded as [min, max]
  // -------------------------------------------------------------------------------------------
  /**
   * Encodes {@link IntRange} as {@code [min, max]}.
   */
  public static final CodecToken<IntRange> INT_RANGE = IntRangeArrayCodec::new;
  /**
   * Polymorphic codec that can decode any registered {@link Shape} subtype. Use this when the
   * concrete shape type is not known ahead of time.
   */
  public static final CodecToken<Shape> SHAPE = () -> ShapeCodecs.SHAPE;

  // -------------------------------------------------------------------------------------------
  // Shapes — polymorphic; the serialized form includes a type discriminator field
  // -------------------------------------------------------------------------------------------
  /**
   * Encodes an axis-aligned {@link Box}.
   */
  public static final CodecToken<Box> BOX = () -> ShapeCodecs.BOX;
  /**
   * Encodes an {@link Ellipsoid}.
   */
  public static final CodecToken<Ellipsoid> ELLIPSOID = () -> ShapeCodecs.ELLIPSOID;
  /**
   * Encodes a {@link Cylinder}.
   */
  public static final CodecToken<Cylinder> CYLINDER = () -> ShapeCodecs.CYLINDER;
  /**
   * Encodes an {@link OriginShape} wrapping any {@link Shape}, pairing the shape with a local
   * origin offset.
   */
  public static final CodecToken<OriginShape<Shape>> ORIGIN_SHAPE = () -> ShapeCodecs.ORIGIN_SHAPE;

  private MathTypes() {
  }
}
