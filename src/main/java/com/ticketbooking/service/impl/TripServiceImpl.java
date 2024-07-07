package com.ticketbooking.service.impl;

import com.ticketbooking.dto.PageResponse;
import com.ticketbooking.exception.ExistingResourceException;
import com.ticketbooking.exception.InvalidInputException;
import com.ticketbooking.exception.ResourceNotFoundException;
import com.ticketbooking.model.Trip;
import com.ticketbooking.repo.TripRepo;
import com.ticketbooking.service.TripService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepo tripRepo;

    @Override
    public Trip findById(Long id) {
        return tripRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Not found Trip<%d>".formatted(id)));
    }

    @Override
    @Cacheable(cacheNames = {"trips"})
    public List<Trip> findAll() {
        return tripRepo.findAll();
    }

    @Override
    @Cacheable(cacheNames = {"trips_paging"}, key = "{#page, #limit}")
    public PageResponse<Trip> findAll(Integer page, Integer limit) {
        Page<Trip> pageSlice = tripRepo.findAll(PageRequest.of(page, limit));
        PageResponse<Trip> pageResponse = new PageResponse<>();
        pageResponse.setDataList(pageSlice.getContent());
        pageResponse.setPageCount(pageSlice.getTotalPages());
        pageResponse.setTotalElements(pageSlice.getTotalElements());
        return pageResponse;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"trips", "trips_paging"}, allEntries = true)
    public Trip save(Trip trip) {

        // check if source == destination
        if (trip.getSource().getId() == trip.getDestination().getId()) {
            throw new InvalidInputException("Start location <%s> and End location <%s> cannot be the same"
                    .formatted(
                            trip.getSource().getName(),
                            trip.getDestination().getName()
                    )
            );
        }

        // check duplicate Trip
        List<Trip> duplicateTrips = tripRepo.findDuplicateDepartureTimeTrip(
                trip.getDriver().getId(),
                trip.getCoach().getId(),
                trip.getSource().getId(),
                trip.getDestination().getId(),
                trip.getDepartureDateTime());

        if (!duplicateTrips.isEmpty()) {
            Trip duplicateTrip = duplicateTrips.get(0);
            String duplicateMsg = "Trip Driver<%s>, Coach<%s>, From<%s> To<%s>, At<%s> is existed!"
                    .formatted(
                            duplicateTrip.getDriver().getFullName(),
                            duplicateTrip.getCoach().getName(),
                            duplicateTrip.getSource().getName(),
                            duplicateTrip.getDestination().getName(),
                            duplicateTrip.getDepartureDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    );
            throw new ExistingResourceException(duplicateMsg);
        }

        return tripRepo.save(trip);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"trips", "trips_paging"}, allEntries = true)
    public Trip update(Trip trip) {

        // check if source == destination
        if (trip.getSource().getId() == trip.getDestination().getId()) {
            throw new InvalidInputException("Start location <%s> and End location <%s> cannot be the same"
                    .formatted(
                            trip.getSource().getName(),
                            trip.getDestination().getName()
                    )
            );
        }

        // check other duplicate Trip
        List<Trip> duplicateTrips = tripRepo.findOtherDuplicateDepartureTimeTrip(
                trip.getId(),
                trip.getDriver().getId(),
                trip.getCoach().getId(),
                trip.getSource().getId(),
                trip.getDestination().getId(),
                trip.getDepartureDateTime());

        if (!duplicateTrips.isEmpty()) {
            Trip duplicateTrip = duplicateTrips.get(0);
            String duplicateMsg = "Trip Driver<%s>, Coach<%s>, From<%s> To<%s>, At<%s> is existed!"
                    .formatted(
                            duplicateTrip.getDriver().getFullName(),
                            duplicateTrip.getCoach().getName(),
                            duplicateTrip.getSource().getName(),
                            duplicateTrip.getDestination().getName(),
                            duplicateTrip.getDepartureDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    );
            throw new ExistingResourceException(duplicateMsg);
        }

        return tripRepo.save(trip);
    }

    @Override
    @CacheEvict(cacheNames = {"trips", "trips_paging"}, allEntries = true)
    public String delete(Long id) {

        Trip foundTrip = findById(id);

        if (!foundTrip.getBookings().isEmpty()) {
            throw new ExistingResourceException("Trip<%d> is in used, can't be deleted".formatted(id));
        }

        tripRepo.deleteById(id);

        return "Delete Trip<%d> successfully".formatted(id);
    }

    @Override
    public List<Trip> findAllBySourceAndDest(Long sourceId, Long destId, String chosenFromDate, String chosenToDate) {
        LocalDate fromDate = LocalDate.parse(chosenFromDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalDate toDate = LocalDate.parse(chosenToDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return tripRepo.findAllBySourceIdAndDestinationId(sourceId, destId, fromDate, toDate);
    }
}
