package com.nowhere.backend.util;

public class GeofenceUtil {

    private static final double EARTH_RADIUS_METERS = 6371000.0;

    private GeofenceUtil() {}

    /**
     * Haversine 공식으로 두 좌표 간 거리를 미터 단위로 계산
     */
    public static double calcDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * 사용자 좌표가 장소 반경(geofenceRadius) 내에 있는지 확인
     */
    public static boolean isInsideGeofence(
            double userLat, double userLon,
            double placeLat, double placeLon,
            int radiusMeters
    ) {
        return calcDistanceMeters(userLat, userLon, placeLat, placeLon) <= radiusMeters;
    }
}
