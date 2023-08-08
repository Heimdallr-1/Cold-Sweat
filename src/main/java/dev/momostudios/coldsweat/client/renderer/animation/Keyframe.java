package dev.momostudios.coldsweat.client.renderer.animation;

public class Keyframe
{
    public float x;
    public float y;
    public float z;
    public float time;

    public Keyframe(float timestamp, double x, double y, double z)
    {
        this.time = timestamp;
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
    }

    @Override
    public String toString()
    {
        return """
                {
                    "time": %s,
                    "x": %s,
                    "y": %s,
                    "z": %s
                }
                """.formatted(time, x, y, z);
    }
}
