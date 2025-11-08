package data.scripts.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MPC_genUtils {

    // stolen from jaydeepiracy with permission

    public static boolean shuffleLocation(StarSystemAPI newSystem, Boolean joinConstellation, Float definedMinX, Float definedMaxX, Float definedMinY, Float definedMaxY) {
        //Will attempt to join an existing constellation, and will prefer one that matches the age and type of the system
        //Can handle null defined bounds
        if (joinConstellation) {
            StarAge age;
            Constellation.ConstellationType type;
            Constellation systemConstellation = newSystem.getConstellation();

            if (systemConstellation != null) {
                age = newSystem.getConstellation().getAge();
                type = newSystem.getConstellation().getType();
            } else {
                age = null;
                type = null;
            }

            int degreesOfFreedom = 0;
            List<Constellation> constellations = getConstellations(degreesOfFreedom);
            if (!(age == null || type == null)) {
                constellations = constellations.stream()
                        .filter(c -> c.getSystems().stream().allMatch(s -> s.getConstellation().getAge() == age && s.getConstellation().getType() == type))
                        .toList();
            }

            while (constellations.isEmpty()) {
                degreesOfFreedom++;
                constellations = getConstellations(degreesOfFreedom);
                if (degreesOfFreedom <= 2) {
                    if (!(age == null || type == null)) {
                        constellations = constellations.stream()
                                .filter(c -> c.getSystems().stream().allMatch(s -> s.getConstellation().getAge() == age && s.getConstellation().getType() == type))
                                .toList();
                    }
                }
                if (degreesOfFreedom == 4) {
                    return false;
                }
            }

            for (Constellation constellation : constellations) {
                List<Float> xCoords = constellation.getSystems().stream()
                        .map(StarSystemAPI::getLocation)
                        .map(Vector2f::getX)
                        .toList();
                List<Float> yCoords = constellation.getSystems().stream()
                        .map(StarSystemAPI::getLocation)
                        .map(Vector2f::getY)
                        .toList();

                float minX = xCoords.stream().min(Float::compareTo).orElseThrow();
                float maxX = xCoords.stream().max(Float::compareTo).orElseThrow();
                float lowerBoundX = minX - 500;
                float upperBoundX = maxX + 500;

                float minY = yCoords.stream().min(Float::compareTo).orElseThrow();
                float maxY = yCoords.stream().max(Float::compareTo).orElseThrow();
                float lowerBoundY = minY - 500;
                float upperBoundY = maxY + 500;

                for (int i = 0; i < 15000; i++) {
                    Vector2f point = new Vector2f(
                            Math.round((Math.random() * (upperBoundX - lowerBoundX)) + lowerBoundX),
                            Math.round((Math.random() * (upperBoundY - lowerBoundY)) + lowerBoundY)
                    );

                    boolean doesPointIntersectWithAnySystems = constellation.getSystems().stream()
                            .anyMatch(system -> doCirclesIntersect(point, newSystem.getMaxRadiusInHyperspace() + 50, system.getLocation(), system.getMaxRadiusInHyperspace() + 50));

                    if (!doesPointIntersectWithAnySystems && !isPointNearCore(point) && isPointWithinBounds(point, definedMinX, definedMaxX, definedMinY, definedMaxY)) {
                        newSystem.setConstellation(constellation);
                        constellation.getSystems().add(newSystem);
                        newSystem.getLocation().set(point);

                        return true;
                    }
                }
            }
            //Will not attempt to join an existing constellation, placed somewhere within the bounds defined
            //Can handle null defined bounds
        } else {
            for (int i = 0; i < 15000; i++) {
                Vector2f point = new Vector2f(
                        Math.round((Math.random() * (definedMaxX - definedMinX)) + definedMinX),
                        Math.round((Math.random() * (definedMaxY - definedMinY)) + definedMinY)
                );

                boolean doesPointIntersectWithAnySystems = Global.getSector().getStarSystems().stream()
                        .anyMatch(system -> doCirclesIntersect(point, newSystem.getMaxRadiusInHyperspace() + 50, system.getLocation(), system.getMaxRadiusInHyperspace() + 50));

                if (!doesPointIntersectWithAnySystems) {
                    newSystem.getLocation().set(point);

                    return true;
                }
            }
        }
        //Backup, will attempt to move the system to somewhere within the sector. Has a small chance to end up in the abyss.
        //Can handle null defined bounds
        for (int i = 0; i < 150000; i++) {
            Vector2f point = new Vector2f(
                    Math.round((Math.random() * (78000f + 78000f)) - 78000f),
                    Math.round((Math.random() * (48000f + 48000f)) - 48000f)
            );

            boolean doesPointIntersectWithAnySystems = Global.getSector().getStarSystems().stream()
                    .anyMatch(system -> doCirclesIntersect(point, newSystem.getMaxRadiusInHyperspace() + 50, system.getLocation(), system.getMaxRadiusInHyperspace() + 50));

            if (!doesPointIntersectWithAnySystems && !isPointNearCore(point) && isPointWithinBounds(point, definedMinX, definedMaxX, definedMinY, definedMaxY)) {
                newSystem.getLocation().set(point);

                return true;
            }
        }

        return false;
    }

    public static boolean doCirclesIntersect(Vector2f centerA, float radiusA, Vector2f centerB, float radiusB) {
        return Math.hypot(centerA.x - centerB.x, centerA.y - centerB.y) <= (radiusA + radiusB);
    }

    public static boolean isPointNearCore(Vector2f point) {
        return (((point.x >= -24000) && (point.x <= 12000)) && ((point.y >= -18000) && (point.y <= 12000)));
    }

    public static boolean isPointWithinBounds(Vector2f point, Float definedMinX, Float definedMaxX, Float definedMinY, Float definedMaxY) {
        return (((definedMinX == null || point.x >= definedMinX) && (definedMaxX == null || point.x <= definedMaxX)) &&
                ((definedMinY == null || point.y >= definedMinY) && (definedMaxY == null || point.y <= definedMaxY)));
    }

    public static List<Constellation> getConstellations(int degreesOfFreedom) {
        List<Constellation> constellations = (Global.getSector().getStarSystems().stream()
                .map(StarSystemAPI::getConstellation)
                .filter(Objects::nonNull)
                .distinct()
                .toList())
                .stream()
                .sorted(Comparator.comparingDouble(c -> -Misc.getDistanceLY(c.getLocation(), new Vector2f(0, 0))))
                .collect(Collectors.toList());

        if (degreesOfFreedom == 0  || degreesOfFreedom == 1) {
            constellations = constellations.stream()
                    .filter(c -> c.getSystems().stream().allMatch(s -> s.getLastPlayerVisitTimestamp() == 0L))
                    .toList();
        }
        if (degreesOfFreedom == 0) {
            constellations = constellations.stream()
                    .filter(c -> c.getSystems().stream().noneMatch(s -> s.hasTag("$jdp_postgenSystem")))
                    .toList();
        }

        return constellations;
    }

}
