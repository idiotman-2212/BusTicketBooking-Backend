package com.ticketbooking.BusTicketBooking.service;

import com.ticketbooking.BusTicketBooking.payload.request.AuthenticationRequest;
import com.ticketbooking.BusTicketBooking.payload.request.ChangePwdRequest;
import com.ticketbooking.BusTicketBooking.payload.request.ForgotRequest;
import com.ticketbooking.BusTicketBooking.payload.request.RegisterRequest;
import com.ticketbooking.BusTicketBooking.payload.response.AuthenticationResponse;

public interface AuthenticationService {
    AuthenticationResponse login(AuthenticationRequest authRequest);

    AuthenticationResponse register(RegisterRequest registerRequest);

    String forgot(ForgotRequest forgotRequest);

    String changePwd(ChangePwdRequest pwdRequest);

    Boolean checkExistUsername(String username);

    Boolean checkExistEmail(String email);

    Boolean checkExistPhone(String phone);

    Boolean checkActiveStatus(String username);
}
