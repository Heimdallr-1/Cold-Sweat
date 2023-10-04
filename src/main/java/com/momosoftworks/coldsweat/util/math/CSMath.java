package com.momosoftworks.coldsweat.util.math;

import com.momosoftworks.coldsweat.util.world.BlockPos;
import com.momosoftworks.coldsweat.util.world.VoxelShape;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import javax.vecmath.Vector3d;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class CSMath
{
    private CSMath() {}

    public static float toRadians(float input) {
        return input * (float) (Math.PI / 180);
    }

    public static float toRadians(double input) {
        return (float) input * (float) (Math.PI / 180);
    }

    public static float toDegrees(float input) {
        return input * (float) (180 / Math.PI);
    }

    public static Vec3 scaleVec(Vec3 vec, double scale)
    {   return Vec3.createVectorHelper(vec.xCoord * scale, vec.yCoord * scale, vec.zCoord * scale);
    }

    public static Vec3 addVec(Vec3 vec1, Vec3 vec2)
    {   return Vec3.createVectorHelper(vec1.xCoord + vec2.xCoord, vec1.yCoord + vec2.yCoord, vec1.zCoord + vec2.zCoord);
    }

    public static Vec3 atCenterOf(BlockPos pos)
    {   return Vec3.createVectorHelper(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public static Vec3 getEulerAngles(Quaternion quat)
    {
        double ysqr = quat.i() * quat.i();

        // roll (x-axis rotation)
        double t0 = +2.0 * (quat.r() * quat.i() + quat.j() * quat.k());
        double t1 = +1.0 - 2.0 * (ysqr + quat.j() * quat.j());
        double roll = Math.atan2(t0, t1);

        // pitch (y-axis rotation)
        double t2 = +2.0 * (quat.r() * quat.j() - quat.k() * quat.i());
        t2 = Math.min(t2, 1.0);
        t2 = Math.max(t2, -1.0);
        double pitch = Math.asin(t2);

        // yaw (z-axis rotation)
        double t3 = +2.0 * (quat.r() * quat.k() + quat.i() * quat.j());
        double t4 = +1.0 - 2.0 * (ysqr + quat.k() * quat.k());
        double yaw = Math.atan2(t3, t4);

        return Vec3.createVectorHelper(roll, pitch, yaw);
    }

    public static Quaternion getQuaternion(double x, double y, double z)
    {
        double cy = Math.cos(z * 0.5);
        double sy = Math.sin(z * 0.5);
        double cp = Math.cos(y * 0.5);
        double sp = Math.sin(y * 0.5);
        double cr = Math.cos(x * 0.5);
        double sr = Math.sin(x * 0.5);

        return new Quaternion(
            (float) (sr * cp * cy - cr * sp * sy),
            (float) (cr * sp * cy + sr * cp * sy),
            (float) (cr * cp * sy - sr * sp * cy),
            (float) (cr * cp * cy + sr * sp * sy));
    }

    public static double clamp(double value, double min, double max)
    {   if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    public static float clamp(float value, float min, float max)
    {   if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    public static int clamp(int value, int min, int max)
    {   if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    public static int ceil(double value)
    {
        if (value >= 0)
        {   return (int) Math.ceil(value);
        }
        else
        {   int sign = getSign(value);
            double abs = Math.abs(value);
            return (int) Math.ceil(abs) * sign;
        }
    }

    public static int floor(double value)
    {
        if (value >= 0)
        {   return (int) Math.floor(value);
        }
        else
        {   int sign = getSign(value);
            double abs = Math.abs(value);
            return (int) Math.floor(abs) * sign;
        }
    }

    public static double max(double... values)
    {
        double max = values[0];
        for (double value : values)
        {
            if (value > max) max = value;
        }
        return max;
    }

    public static double min(double... values)
    {
        double min = values[0];
        for (double value : values)
        {
            if (value < min) min = value;
        }
        return min;
    }

    /**
     * Calculates if the given value is between two values (inclusive)
     */
    public static boolean withinRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    /**
     * Calculates if the given value is between two values (exclusive)
     */
    public static boolean isBetween(double value, double min, double max) {
        return value > min && value < max;
    }

    /**
     * Returns a number between the two given values {@code blendFrom} and {@code blendTo}, based on factor.<br>
     * If {@code factor} = rangeMin, returns {@code blendFrom}. If {@code factor} = {@code rangeMax}, returns {@code blendTo}.<br>
     * @param blendFrom The minimum value.
     * @param blendTo The maximum value.
     * @param factor The "progress" between blendFrom and blendTo.
     * @param rangeMin The minimum of the range of values over which to interpolate.
     * @param rangeMax The maximum of the range of values over which to interpolate.
     * @return The interpolated value.
     */
    public static double blend(double blendFrom, double blendTo, double factor, double rangeMin, double rangeMax)
    {
        if (factor <= rangeMin) return blendFrom;
        if (factor >= rangeMax) return blendTo;
        return ((1 / (rangeMax - rangeMin)) * (factor - rangeMin)) * (blendTo - blendFrom) + blendFrom;
    }

    /**
     * Floating-point overload for {@link #blend(double, double, double, double, double)}.
     */
    public static float blend(float blendFrom, float blendTo, float factor, float rangeMin, float rangeMax)
    {
        if (factor <= rangeMin) return blendFrom;
        if (factor >= rangeMax) return blendTo;
        return ((1 / (rangeMax - rangeMin)) * (factor - rangeMin)) * (blendTo - blendFrom) + blendFrom;
    }

    public static double blendLog(double blendFrom, double blendTo, double factor, double rangeMin, double rangeMax)
    {
        if (factor <= rangeMin) return blendFrom;
        if (factor >= rangeMax) return blendTo;
        return (blendTo - blendFrom) / Math.sqrt(rangeMax - rangeMin) * Math.sqrt(factor - rangeMin) + blendFrom;
    }

    public static float blendLog(float blendFrom, float blendTo, float factor, float rangeMin, float rangeMax)
    {
        if (factor <= rangeMin) return blendFrom;
        if (factor >= rangeMax) return blendTo;
        return (blendTo - blendFrom) / (float) Math.sqrt(rangeMax - rangeMin) * (float) Math.sqrt(factor - rangeMin) + blendFrom;
    }

    public static double averagePair(Pair<? extends Number, ? extends Number> pair)
    {
        return (pair.getFirst().doubleValue() + pair.getSecond().doubleValue()) / 2;
    }

    public static Pair<Double, Double> addPairs(Pair<? extends Number, ? extends Number> pair1, Pair<? extends Number, ? extends Number> pair2)
    {
        return new Pair<>(pair1.getFirst().doubleValue() + pair2.getFirst().doubleValue(), pair1.getSecond().doubleValue() + pair2.getSecond().doubleValue());
    }

    public static double getDistance(Entity entity, Vector3d pos)
    {
        return getDistance(entity, pos.x, pos.y, pos.z);
    }

    public static double getDistanceSqr(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        double xDistance = Math.abs(x1 - x2);
        double yDistance = Math.abs(y1 - y2);
        double zDistance = Math.abs(z1 - z2);
        return xDistance * xDistance + yDistance * yDistance + zDistance * zDistance;
    }

    public static double getDistance(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        return Math.sqrt(getDistanceSqr(x1, y1, z1, x2, y2, z2));
    }

    public static double getDistance(Vec3 pos1, Vec3 pos2)
    {
        return getDistance(pos1.xCoord, pos1.yCoord, pos1.zCoord, pos2.xCoord, pos2.yCoord, pos2.zCoord);
    }

    public static double getDistance(Entity entity, double x, double y, double z)
    {
        return getDistance(entity.posX, entity.posY + entity.height / 2, entity.posZ, x, y, z);
    }

    public static double getDistance(Vec3i pos1, Vec3i pos2)
    {
        return Math.sqrt(pos1.distSqr(pos2));
    }

    public static double average(Number... values)
    {
        double sum = 0;
        for (Number value : values)
        {
            sum += value.doubleValue();
        }
        return sum / values.length;
    }

    /**
     * Takes an average of the two values, with weight<br>
     * @param val1 The first value.
     * @param val2 The second value.
     * @param weight1 The weight of the first value.
     * @param weight2 The weight of the second value.
     * @return The weighted average.
     */
    public static double weightedAverage(double val1, double val2, double weight1, double weight2)
    {
        return (val1 * weight1 + val2 * weight2) / (weight1 + weight2);
    }

    /**
     * Takes an average of all the values in the given list, with weight<br>
     * <br>
     * @param values The map of values to average (value, weight).
     * @return The average of the values in the given array.
     */
    public static <A extends Number, B extends Number> double weightedAverage(List<Pair<A, B>> values)
    {
        double sum = 0;
        double weightSum = 0;
        for (Pair<A, B> entry : values)
        {
            double weight = entry.getSecond().doubleValue();
            sum += entry.getFirst().doubleValue() * weight;
            weightSum += weight;
        }
        return sum / Math.max(1, weightSum);
    }

    public static Vector3d vectorToVec(Vector3d vec)
    {
        return new Vector3d(vec.x, vec.y, vec.z);
    }

    /**
     * Returns a {@link Direction} from the given vector.
     * @return The direction.
     */
    public static Direction getDirectionFrom(double x, double y, double z)
    {
        Direction direction = Direction.NORTH;
        double f = Float.MIN_VALUE;

        for (Direction direction1 : Direction.values())
        {
            double f1 = x * direction1.getStepX() + y * direction1.getStepY() + z * direction1.getStepZ();

            if (f1 > f)
            {
                f = f1;
                direction = direction1;
            }
        }

        return direction;
    }

    public static Direction getDirectionFrom(Vector3d vec3)
    {
        return getDirectionFrom(vec3.x, vec3.y, vec3.z);
    }

    public static Direction getDirectionFrom(BlockPos from, BlockPos to)
    {
        return getDirectionFrom(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());
    }

    public static <T> void breakableForEach(Collection<T> collection, BiConsumer<T, InterruptableStreamer<T>> consumer)
    {
        new InterruptableStreamer<T>(collection).run(consumer);
    }

    /**
     * Simple try/catch block that ignores errors.
     * @param runnable The code to run upon success.
     */
    public static void tryCatch(Runnable runnable)
    {
        try
        {
            runnable.run();
        }
        catch (Throwable ignored) {}
    }

    /**
     * @return 1 if the given value is positive, -1 if it is negative, and 0 if it is 0.
     */
    public static int getSign(double value)
    {
        if (value == 0) return 0;
        return value < 0 ? -1 : 1;
    }

    /**
     * @return 1 if the given value is above the range, -1 if it is below the range, and 0 if it is within the range.
     */
    public static int getSignForRange(double value, double min, double max)
    {
        return value > max ? 1 : value < min ? -1 : 0;
    }

    /**
     * Limits the decimal places of the value to the given amount.
     * @param value The value to limit.
     * @param sigFigs The amount of decimal places to limit to.
     * @return The value with the decimal places limited.
     */
    public static double sigFigs(double value, int sigFigs)
    {
        return (int) (value * Math.pow(10.0, sigFigs)) / Math.pow(10.0, sigFigs);
    }

    public static double round(double value, int places)
    {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static int blendColors(int color1, int color2, float ratio)
    {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);
        return r << 16 | g << 8 | b;
    }

    /**
     * @return The value that is farther from 0.
     */
    public static double maxAbs(double... values)
    {
        double mostExtreme = 0;
        for (double value : values)
        {
            if (Math.abs(value) > Math.abs(mostExtreme))
            {
                mostExtreme = value;
            }
        }
        return mostExtreme;
    }

    /**
     * @return The value that is closer to 0.
     */
    public static double minAbs(double... values)
    {
        double smallest = values[0];
        for (double value : values)
        {
            if (Math.abs(value) < Math.abs(smallest))
            {
                smallest = value;
            }
        }
        return smallest;
    }

    public static boolean equalAbs(double value1, double value2)
    {
        return Math.abs(value1) == Math.abs(value2);
    }

    public static boolean greaterAbs(double value1, double value2)
    {
        return Math.abs(value1) > Math.abs(value2);
    }

    public static boolean lessAbs(double value1, double value2)
    {
        return Math.abs(value1) < Math.abs(value2);
    }

    public static boolean greaterEqualAbs(double value1, double value2)
    {
        return Math.abs(value1) >= Math.abs(value2);
    }

    public static boolean lessEqualAbs(double value1, double value2)
    {
        return Math.abs(value1) <= Math.abs(value2);
    }

    /**
     * Lowers the absolute value of the given number by [amount].
     */
    public static double shrink(double value, double amount)
    {   return Math.max(0, Math.abs(value) - amount) * getSign(value);
    }

    public static int shrink(int value, int amount)
    {   return value > 0 ? Math.max(0, value - amount) : Math.min(0, value + amount);
    }

    public static double grow(double value, double amount)
    {   return Math.abs(value) + amount * getSign(value);
    }

    public static int grow(int value, int amount)
    {   return value > 0 ? value + amount : value - amount;
    }

    /**
     * @return A Vector3d at the center of the given BlockPos.
     */
    public static Vector3d getCenterPos(BlockPos pos)
    {
        return new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    /**
     * Rotates a VoxelShape to the given direction, assuming it is facing north.
     * @param to The direction to rotate to.
     * @param shape The shape to rotate.
     * @return The rotated shape.
     */
    public static VoxelShape rotateShape(Direction to, VoxelShape shape)
    {
        switch (to)
        {
            case NORTH:
                return shape;
            case SOUTH:
                return new VoxelShape(shape.maxX, shape.minY, shape.maxZ, shape.minX, shape.maxY, shape.minZ);
            case EAST:
                return new VoxelShape(shape.maxZ, shape.minY, shape.minX, shape.minZ, shape.maxY, shape.maxX);
            case WEST:
                return new VoxelShape(shape.minZ, shape.minY, shape.maxX, shape.maxZ, shape.maxY, shape.minX);
            default:
                return null;
        }
    }

    /**
     * "Flattens" the given VoxelShape into a 2D projection along the given axis.
     * @param axis The axis to flatten along.
     * @param shape The shape to flatten.
     * @return The flattened shape.
     */
    public static VoxelShape flattenShape(Direction.Axis axis, VoxelShape shape)
    {   return new VoxelShape(shape.getFaceShape(axis));
    }

    public static boolean withinCube(BlockPos pos1, BlockPos pos2, double maxDistance)
    {   return Math.abs(pos1.getX() - pos2.getX()) <= maxDistance
            && Math.abs(pos1.getY() - pos2.getY()) <= maxDistance
            && Math.abs(pos1.getZ() - pos2.getZ()) <= maxDistance;
    }

    public static <K, V> Map<K, V> mapOf()
    {   return new MapN<>();
    }

    public static <K, V> Map<K, V> mapOf(K k, V v)
    {   return new MapN<>(k, v);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2)
    {   return new MapN<>(k1, v1, k2, v2);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3)
    {   return new MapN<>(k1, v1, k2, v2, k3, v3);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4)
    {   return new MapN<>(k1, v1, k2, v2, k3, v3, k4, v4);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5)
    {   return new MapN<>(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                                         K k6, V v6)
    {   return new MapN<>(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                                         K k6, V v6, K k7, V v7)
    {   return new MapN<>(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                                         K k6, V v6, K k7, V v7, K k8, V v8)
    {   return new MapN<>(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                                         K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9)
    {   return new MapN<>(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                                         K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10)
    {   return new MapN<>(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);
    }
}
