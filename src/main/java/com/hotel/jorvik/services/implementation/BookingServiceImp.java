package com.hotel.jorvik.services.implementation;

import com.hotel.jorvik.models.*;
import com.hotel.jorvik.models.DTO.bookings.AllBookingsResponse;
import com.hotel.jorvik.models.DTO.bookings.CurrentRoomResponse;
import com.hotel.jorvik.models.DTO.bookings.EntertainmentReservationResponse;
import com.hotel.jorvik.models.DTO.bookings.RoomReservationsResponse;
import com.hotel.jorvik.repositories.*;
import com.hotel.jorvik.security.SecurityTools;
import com.hotel.jorvik.security.implementation.EmailSender;
import com.hotel.jorvik.services.BookingService;
import com.hotel.jorvik.services.RoomService;
import com.hotel.jorvik.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.hotel.jorvik.util.Tools.*;

@Service
@RequiredArgsConstructor
public class BookingServiceImp implements BookingService {

    private final RoomService roomService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final RoomReservationRepository roomReservationRepository;
    private final EntertainmentReservationRepository entertainmentReservationRepository;
    private final EntertainmentRepository entertainmentRepository;
    private final EntertainmentTypeRepository entertainmentTypeRepository;
    private final SecurityTools securityTools;
    private final EmailSender emailSender;

    @Override
    public RoomReservation bookRoom(String from, String to, int roomTypeId) {
        Date sqlFromDate = parseDate(from);
        Date sqlToDate = parseDate(to);

        List<Room> rooms = roomService.getAllByAvailableTimeAndType(from, to, roomTypeId);
        if (!rooms.iterator().hasNext()) {
            throw new IllegalArgumentException("No rooms available");
        }
        Room room = rooms.iterator().next();

        if (userService.getUserRoomReservationsCount() >= 5) {
            throw new IllegalArgumentException("You can't book more than 5 rooms");
        }
        User user = securityTools.retrieveUserData();

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        RoomReservation roomReservation = new RoomReservation(sqlFromDate, sqlToDate, timestamp, room, user);
        roomReservationRepository.save(roomReservation);
        if (user.getVerified() != null){
            emailSender.sendEmail(
                    user.getEmail(),
                    "Room reservation",
                    "You have successfully booked room number " + room.getNumber() + " from " + from + " to " + to);
        }
        return roomReservation;
    }

    @Override
    public RoomReservation bookRoomByAdmin(String dateFrom, String dateTo, int roomId, int userId) {
        Date sqlFromDate = parseDate(dateFrom);
        Date sqlToDate = parseDate(dateTo);

        User user = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("No user found"));
        Room room = roomService.getById(roomId);

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        RoomReservation roomReservation = new RoomReservation(sqlFromDate, sqlToDate, timestamp, room, user);
        roomReservationRepository.save(roomReservation);
        if (user.getVerified() != null) {
            emailSender.sendEmail(
                    user.getEmail(),
                    "Room reservation",
                    "You have new booking at Jorvik Hotel. Room number " + room.getNumber() + ", from " + dateFrom + " to " + dateTo);
        }
        return roomReservation;
    }

    @Override
    public EntertainmentReservation bookEntertainment(String entertainmentType, String dateFrom, String timeFrom, String dateTo, String timeTo, int entertainmentId){
        Timestamp dateTimeFrom = parseDate(dateFrom, timeFrom);
        Timestamp dateTimeTo = parseDate(dateTo, timeTo);

        User user = securityTools.retrieveUserData();

        List<EntertainmentType> entertainmentTypes = entertainmentTypeRepository.findAll();
        Optional<EntertainmentType> type = entertainmentTypes.stream()
                .filter(entertainmentType1 -> entertainmentType1
                        .getName()
                        .equals(entertainmentType))
                .findFirst();
        if (type.isEmpty()) {
            throw new IllegalArgumentException("Entertainment type not found");
        }

        List<Entertainment> entertainments = entertainmentRepository
                .findAvailableEntertainmentsByTypeAndTime(type.get().getId(), dateTimeFrom, dateTimeTo);
        Optional<Entertainment> entertainment = entertainments.stream()
                .filter(entertainment1 -> entertainment1.getId() == entertainmentId)
                .findFirst();

        if (entertainment.isEmpty()) {
            throw new IllegalArgumentException("Entertainment not found");
        }

        if (userService.getUserEntertainmentReservationsCount() >= 10) {
            throw new IllegalArgumentException("You can't book more than 10 rooms");
        }

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        EntertainmentReservation entertainmentReservation = new EntertainmentReservation(dateTimeFrom, dateTimeTo, timestamp, user, entertainment.get());
        entertainmentReservationRepository.save(entertainmentReservation);

        if (user.getVerified() != null) {
            emailSender.sendEmail(
                    user.getEmail(),
                    "Entertainment reservation",
                    "You have successfully booked " + entertainment.get().getDescription() + " from " + dateFrom + " " + timeFrom + " to " + dateTo + " " + timeTo);
        }
        return entertainmentReservation;
    }

    @Override
    public RoomReservation getRoomReservation(int reservationId) {
        return roomReservationRepository.findById(reservationId).orElseThrow(() -> new NoSuchElementException("No reservation found"));
    }

    @Override
    public EntertainmentReservation getEntertainmentReservation(int reservationId) {
        return entertainmentReservationRepository.findById(reservationId).orElseThrow(() -> new NoSuchElementException("No reservation found"));
    }

    @Override
    public Room getLastBooking() {
        User user = securityTools.retrieveUserData();
        RoomReservation roomReservation = roomReservationRepository.findFirstByUserOrderByBookedAtDesc(user);
        if (roomReservation == null) {
            throw new NoSuchElementException("No bookings found");
        }
        return roomReservation.getRoom();
    }

    @Override
    public List<RoomReservationsResponse> getBookingsForPeriod(String dateFrom, String dateTo) {
        List<RoomReservation> reservations = roomReservationRepository.findAllByFromDateBetweenOrToDateBetween(parseDate(dateFrom), parseDate(dateTo), parseDate(dateFrom), parseDate(dateTo));
        List<RoomReservationsResponse> responses = new ArrayList<>();
        for (RoomReservation reservation : reservations) {
            responses.add(
                    RoomReservationsResponse.builder()
                            .reservationId(reservation.getId())
                            .clientName(reservation.getUser().getFirstName() + " " + reservation.getUser().getLastName())
                            .clientPhoneNumber(reservation.getUser().getPhone())
                            .datePeriod(reservation.getFromDate().toString() + " - " + reservation.getToDate().toString())
                            .roomNumber(reservation.getRoom().getNumber())
                            .bookingStatus(getRoomBookingStatus(reservation))
                            .rate(getRoomPaymentAmount(reservation.getRoom().getRoomType(), reservation.getFromDate().toString(), reservation.getToDate().toString()))
                            .build()
            );
        }
        return responses;
    }

    @Override
    public List<EntertainmentReservationResponse> getEntertainmentBookingsForPeriod(String dateFrom, String dateTo) {
        List<EntertainmentReservation> reservations = entertainmentReservationRepository.findAllByDateFromBetweenOrDateToBetween(parseDate(dateFrom), parseDate(dateTo), parseDate(dateFrom), parseDate(dateTo));
        List<EntertainmentReservationResponse> responses = new ArrayList<>();

        for (EntertainmentReservation reservation : reservations) {
            SimpleDateFormat dateSdf = new SimpleDateFormat("yyyy-MM-dd");
            String dateStrFrom = dateSdf.format(reservation.getDateFrom());
            String dateStrTo = dateSdf.format(reservation.getDateTo());
            SimpleDateFormat timeSdf = new SimpleDateFormat("HH-mm");
            String timeStrFrom = timeSdf.format(reservation.getDateFrom());
            String timeStrTo = timeSdf.format(reservation.getDateTo());
            responses.add(
                    EntertainmentReservationResponse.builder()
                            .reservationId(reservation.getId())
                            .clientName(reservation.getUser().getFirstName() + " " + reservation.getUser().getLastName())
                            .clientPhoneNumber(reservation.getUser().getPhone())
                            .datePeriod(reservation.getDateFrom().toString() + " - " + reservation.getDateTo().toString())
                            .entertainmentType(reservation.getEntertainment().getEntertainmentType().getName())
                            .entertainmentElement(reservation.getEntertainment().getDescription())
                            .bookingStatus(getEntertainmentBookingStatus(reservation))
                            .rate(getEntertainmentPaymentAmount(
                                    reservation.getEntertainment().getEntertainmentType(),
                                    dateStrFrom,
                                    timeStrFrom,
                                    dateStrTo,
                                    timeStrTo))
                            .build()
            );
        }
        return responses;
    }

    @Override
    public List<AllBookingsResponse> getAll() {
        User user = securityTools.retrieveUserData();
        List<RoomReservation> userRooms = roomReservationRepository.findAllByUser(user);
        List<EntertainmentReservation> userEntertainments = entertainmentReservationRepository.findAllByUser(user);
        return retrieveBookingsResponse(userRooms, userEntertainments);
    }

    @Override
    public List<AllBookingsResponse> getAllByAdmin(int userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NoSuchElementException("No user found"));
        List<RoomReservation> userRooms = roomReservationRepository.findAllByUser(user);
        List<EntertainmentReservation> userEntertainments = entertainmentReservationRepository.findAllByUser(user);
        return retrieveBookingsResponse(userRooms, userEntertainments);
    }

    private List<AllBookingsResponse> retrieveBookingsResponse(List<RoomReservation> userRooms, List<EntertainmentReservation> userEntertainments) {
        List<AllBookingsResponse> bookings = new ArrayList<>();
        for(RoomReservation roomReservation : userRooms) {
            bookings.add(
                    AllBookingsResponse.builder()
                            .id(roomReservation.getId())
                            .description("Hotel room reservation")
                            .price(roomReservation.getRoom().getRoomType().getPrice())
                            .name("Room")
                            .fromDate(roomReservation.getFromDate().toString())
                            .toDate(roomReservation.getToDate().toString())
                            .timestampFrom(new Timestamp(roomReservation.getFromDate().getTime()))
                            .bookingType("Room")
                            .roomTypeId(roomReservation.getRoom().getRoomType().getId())
                            .bookingStatus(getRoomBookingStatus(roomReservation))
                            .paymentId(roomReservation.getPayment() == null ? null : roomReservation.getPayment().getId())
                            .build()
                    );
        }
        for (EntertainmentReservation entertainmentReservation : userEntertainments) {
            bookings.add(
                    AllBookingsResponse.builder()
                            .id(entertainmentReservation.getId())
                            .description(entertainmentReservation.getEntertainment().getDescription())
                            .price(entertainmentReservation.getEntertainment().getEntertainmentType().getPrice())
                            .name(entertainmentReservation.getEntertainment().getEntertainmentType().getName())
                            .fromDate(entertainmentReservation.getDateFrom().toString())
                            .toDate(entertainmentReservation.getDateTo().toString())
                            .timestampFrom(entertainmentReservation.getDateFrom())
                            .bookingType(entertainmentReservation.getEntertainment().getEntertainmentType().getName())
                            .bookingStatus(getEntertainmentBookingStatus(entertainmentReservation))
                            .accessCode(entertainmentReservation.getEntertainment().getLockCode())
                            .paymentId(entertainmentReservation.getPayment() == null ? null : entertainmentReservation.getPayment().getId())
                            .build()
            );
        }

        bookings.sort((o1, o2) -> o2.getTimestampFrom().compareTo(o1.getTimestampFrom()));
        return bookings;
    }

    @Override
    public List<CurrentRoomResponse> getAllCurrentRooms() {
        User user = securityTools.retrieveUserData();
        List<RoomReservation> userRooms = roomReservationRepository.findAllByUser(user);
        List<CurrentRoomResponse> rooms = new ArrayList<>();
        for (RoomReservation roomReservation : userRooms) {
            if (getRoomBookingStatus(roomReservation) == RoomReservation.BookingStatus.ACTIVE) {
                rooms.add(
                        CurrentRoomResponse.builder()
                                .number(roomReservation.getRoom().getNumber())
                                .datePeriod(roomReservation.getFromDate().toString() + " - " + roomReservation.getToDate().toString())
                                .accessCode(roomReservation.getRoom().getAccessCode())
                                .build()
                );
            }
        }
        return rooms;
    }

    private RoomReservation.BookingStatus getRoomBookingStatus(RoomReservation roomReservation) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        if (roomReservation.getPayment() == null) {
            return RoomReservation.BookingStatus.AWAITING_PAYMENT;
        } else if (roomReservation.getFromDate().after(timestamp)) {
            return RoomReservation.BookingStatus.UPCOMING;
        } else if (roomReservation.getToDate().before(timestamp)) {
            return RoomReservation.BookingStatus.COMPLETED;
        } else {
            return RoomReservation.BookingStatus.ACTIVE;
        }
    }

    private RoomReservation.BookingStatus getEntertainmentBookingStatus(EntertainmentReservation entertainmentReservation) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Timestamp timestampPlusOneHour = new Timestamp(System.currentTimeMillis() + 3600000);
        if (entertainmentReservation.getPayment() == null) {
            return RoomReservation.BookingStatus.AWAITING_PAYMENT;
        } else if (entertainmentReservation.getDateFrom().after(timestamp)) {
            return RoomReservation.BookingStatus.UPCOMING;
        } else if (entertainmentReservation.getDateFrom().before(timestampPlusOneHour)) {
            return RoomReservation.BookingStatus.COMPLETED;
        } else {
            return RoomReservation.BookingStatus.ACTIVE;
        }
    }

    @Override
    public void addPaymentToRoomReservation(int roomReservationId, Payment payment) {
        RoomReservation roomReservation = roomReservationRepository.findById(roomReservationId).orElseThrow(() -> new NoSuchElementException("No room reservation found"));
        roomReservation.setPayment(payment);
        roomReservationRepository.save(roomReservation);
    }

    @Override
    public void addPaymentToEntertainmentReservation(int reservationId, Payment payment) {
        EntertainmentReservation entertainmentReservation = entertainmentReservationRepository.findById(reservationId).orElseThrow(() -> new NoSuchElementException("No entertainment reservation found"));
        entertainmentReservation.setPayment(payment);
        entertainmentReservationRepository.save(entertainmentReservation);
    }

    @Override
    public void deleteUnpaidRoomReservations() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        List<RoomReservation> roomReservations = roomReservationRepository.findAllByPaymentIsNull();
        // Delete all unpaid room reservations that are in the past or 24 hours before the reservation
        for (RoomReservation roomReservation : roomReservations) {
            if (roomReservation.getFromDate().before(timestamp) ||
                roomReservation.getFromDate().before(new Timestamp(timestamp.getTime() + 86400000))) {
                roomReservationRepository.delete(roomReservation);
            }
        }
    }

    @Override
    public void deleteUnpaidEntertainmentReservations() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        List<EntertainmentReservation> entertainmentReservations = entertainmentReservationRepository.findAllByPaymentIsNull();
        // Delete all unpaid room reservations that are in the past or 5 hours before the reservation
        for (EntertainmentReservation entertainmentReservation : entertainmentReservations) {
            if (entertainmentReservation.getDateFrom().before(timestamp) ||
                    entertainmentReservation.getDateFrom().before(new Timestamp(timestamp.getTime() + 18000000))) {
                entertainmentReservationRepository.delete(entertainmentReservation);
            }
        }
    }

    @Override
    public void deleteRoomReservation(int reservationId) {
        User user = securityTools.retrieveUserData();
        RoomReservation reservation = roomReservationRepository.findById(reservationId).orElseThrow(() -> new NoSuchElementException("No room reservation found"));
        if (reservation.getUser().getId() != user.getId()) {
            throw new IllegalArgumentException("You can't delete this reservation");
        }
        if (reservation.getPayment() != null) {
            throw new IllegalArgumentException("You can't delete paid reservation");
        }
        roomReservationRepository.delete(reservation);
    }

    @Override
    public void deleteRoomReservationByAdmin(int reservationId) {
        RoomReservation reservation = roomReservationRepository.findById(reservationId).orElseThrow(() -> new NoSuchElementException("No room reservation found"));
        if (reservation.getPayment() != null) {
            throw new IllegalArgumentException("You can't delete paid reservation");
        }
        roomReservationRepository.delete(reservation);
    }

    @Override
    public void deleteEntertainmentReservation(int reservationId) {
        User user = securityTools.retrieveUserData();
        EntertainmentReservation reservation = entertainmentReservationRepository.findById(reservationId).orElseThrow(() -> new NoSuchElementException("No entertainment reservation found"));
        if (reservation.getUser().getId() != user.getId()) {
            throw new IllegalArgumentException("You can't delete this reservation");
        }
        if (reservation.getPayment() != null) {
            throw new IllegalArgumentException("You can't delete paid reservation");
        }
        entertainmentReservationRepository.delete(reservation);
    }

    @Override
    public void deleteEntertainmentReservationByAdmin(int reservationId) {
        EntertainmentReservation reservation = entertainmentReservationRepository.findById(reservationId).orElseThrow(() -> new NoSuchElementException("No entertainment reservation found"));
        if (reservation.getPayment() != null) {
            throw new IllegalArgumentException("You can't delete paid reservation");
        }
        entertainmentReservationRepository.delete(reservation);
    }
}
