package com.tutorhub.mappicker;

import java.io.Serializable;
import java.util.Objects;

/**
 * Kết quả địa chỉ được chọn từ {@link MapPickerDialog}.
 *
 * Đây là một value object bất biến (immutable): address (chuỗi địa chỉ
 * đã định dạng bởi Goong), lat/lng (tọa độ WGS84).
 */
public final class MapAddressResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String address;
    private final double lat;
    private final double lng;

    public MapAddressResult(String address, double lat, double lng) {
        this.address = address == null ? "" : address;
        this.lat = lat;
        this.lng = lng;
    }

    public String getAddress() {
        return address;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MapAddressResult)) return false;
        MapAddressResult that = (MapAddressResult) o;
        return Double.compare(lat, that.lat) == 0
                && Double.compare(lng, that.lng) == 0
                && Objects.equals(address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, lat, lng);
    }

    @Override
    public String toString() {
        return "MapAddressResult{address='" + address + "', lat=" + lat + ", lng=" + lng + '}';
    }
}
