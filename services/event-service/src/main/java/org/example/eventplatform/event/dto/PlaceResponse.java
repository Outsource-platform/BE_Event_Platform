package org.example.eventplatform.event.dto;

/** Các bản ghi trả về cho danh mục địa giới công khai. */
public final class PlaceResponse {

    private PlaceResponse() {
    }

    public record ProvinceView(int code, String name, String fullName) {
    }

    /** {@code troupeCount} là số đơn vị đang hoạt động đặt tại phường/xã này. */
    public record WardView(int code, String name, int troupeCount) {
    }
}
