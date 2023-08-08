package dev.momostudios.coldsweat.core.advancement.trigger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import dev.momostudios.coldsweat.api.util.Temperature;
import dev.momostudios.coldsweat.config.ConfigSettings;

import java.util.List;

public class TriggerHelper
{
    public static Either<Double, Pair<Double, Double>> getTempValueOrRange(JsonObject entry)
    {
        if (entry.has("value"))
        {
            double value = entry.get("value").getAsDouble();
            return Either.left(value);
        }
        else
        {
            JsonObject range = entry.getAsJsonObject("range");
            double below = Double.MAX_VALUE;
            double above = -Double.MAX_VALUE;

            if (range != null)
            {
                if (range.has("below"))
                {
                    try
                    {   below = range.get("below").getAsDouble();
                    }
                    catch (Exception e)
                    {
                        String builtinValue = range.get("below").getAsString();
                        if (builtinValue.equals("max_habitable"))
                            below = ConfigSettings.MAX_TEMP.get();
                        else if (builtinValue.equals("min_habitable"))
                            below = ConfigSettings.MIN_TEMP.get();
                    }
                }

                if (range.has("above"))
                {
                    try
                    {   above = range.get("above").getAsDouble();
                    }
                    catch (Exception e)
                    {
                        String builtinValue = range.get("above").getAsString();
                        if (builtinValue.equals("max_habitable"))
                            above = ConfigSettings.MAX_TEMP.get();
                        else if (builtinValue.equals("min_habitable"))
                            above = ConfigSettings.MIN_TEMP.get();
                    }
                }
            }

            return Either.right(new Pair<>(below, above));
        }
    }

    public static JsonArray serializeConditions(List<TempCondition> conditions)
    {
        JsonArray values = new JsonArray();

        for (TriggerHelper.TempCondition condition : conditions)
        {
            double above = condition.above();
            double below = condition.below();
            JsonObject value = new JsonObject();
            value.addProperty("type", condition.type().getID());
            if (above == below)
            {   value.addProperty("value", above);
            }
            else
            {   JsonObject range = new JsonObject();
                range.addProperty("below", below);
                range.addProperty("above", above);
                value.add("range", range);
                values.add(value);
            }
        }
        return values;
    }

    record TempCondition(Temperature.Type type, double below, double above)
    {
        public boolean matches(double value)
        {   return below > above
                 ? value >= above && value <= below
                 : value >= above || value <= below;
        }
    }
}
