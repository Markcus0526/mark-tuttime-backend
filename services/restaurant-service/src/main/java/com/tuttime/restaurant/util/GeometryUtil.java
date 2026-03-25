package com.tuttime.restaurant.util;

import com.tuttime.common.exception.BusinessException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;

public class GeometryUtil {

	private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

	public static Point createPoint(double longitude, double latitude) {
		return geometryFactory.createPoint(new Coordinate(longitude, latitude));
	}

	/**
	 * Calculates distance between two coordinates using Haversine formula.
	 *
	 * @param lat1 Latitude of first point
	 * @param lng1 Longitude of first point
	 * @param lat2 Latitude of second point
	 * @param lng2 Longitude of second point
	 * @return Distance in meters
	 */
	public static double calculateDistance(
			double lat1, double lng1,
			double lat2, double lng2) {

		final int EARTH_RADIUS_METERS = 6371000;

		double lat1Rad = Math.toRadians(lat1);
		double lat2Rad = Math.toRadians(lat2);
		double deltaLat = Math.toRadians(lat2 - lat1);
		double deltaLng = Math.toRadians(lng2 - lng1);

		double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
				Math.cos(lat1Rad) * Math.cos(lat2Rad) *
						Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);

		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return EARTH_RADIUS_METERS * c;
	}

	/**
	 * Validates latitude coordinate.
	 *
	 * @param latitude Latitude to validate
	 * @throws BusinessException if latitude is invalid
	 */
	public static void validateLatitude(BigDecimal latitude) {
		if (latitude == null) {
			throw new BusinessException("Latitude is required");
		}

		if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 ||
				latitude.compareTo(BigDecimal.valueOf(90)) > 0) {
			throw new BusinessException(
					"Latitude must be between -90 and 90");
		}
	}

	/**
	 * Validates longitude coordinate.
	 *
	 * @param longitude Longitude to validate
	 * @throws BusinessException if longitude is invalid
	 */
	public static void validateLongitude(BigDecimal longitude) {
		if (longitude == null) {
			throw new BusinessException("Longitude is required");
		}

		if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 ||
				longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
			throw new BusinessException(
					"Longitude must be between -180 and 180");
		}
	}

	/**
	 * Validates location accuracy.
	 *
	 * @param accuracy Accuracy in meters
	 * @throws BusinessException if accuracy is invalid
	 */
	public static void validateAccuracy(Integer accuracy) {
		if (accuracy != null && (accuracy < 1 || accuracy > 10000)) {
			throw new BusinessException(
					"Location accuracy must be between 1 and 10000 meters");
		}
	}

	/**
	 * Blurred coordinates result
	 */
	public static class BlurredCoordinates {
		private final BigDecimal latitude;
		private final BigDecimal longitude;

		public BlurredCoordinates(BigDecimal latitude, BigDecimal longitude) {
			this.latitude = latitude;
			this.longitude = longitude;
		}

		public BigDecimal getLatitude() {
			return latitude;
		}

		public BigDecimal getLongitude() {
			return longitude;
		}
	}
}
