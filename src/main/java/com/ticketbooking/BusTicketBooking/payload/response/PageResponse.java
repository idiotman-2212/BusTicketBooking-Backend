package com.ticketbooking.BusTicketBooking.payload.response;

import lombok.Data;

import java.util.List;

@Data
public class PageResponse<T> {
    private List<T> dataList;
    private Integer pageCount;
    private Long totalElements;
}
