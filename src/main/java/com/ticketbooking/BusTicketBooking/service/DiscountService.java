package com.ticketbooking.BusTicketBooking.service;

import com.ticketbooking.BusTicketBooking.entity.Discount;
import com.ticketbooking.BusTicketBooking.payload.response.PageResponse;

import java.util.List;

public interface DiscountService {
    Discount findById(Long id);

    Discount findByCode(String code);

    List<Discount> findAll();

    List<Discount> findAllAvailable();

    PageResponse<Discount> findAll(Integer page, Integer limit);

    Discount save(Discount discount);

    Discount update(Discount discount);

    String delete(Long id);

    Boolean checkDuplicateDiscountInfo(String mode, Long discountId, String field, String value);
}
