package com.greenfox.register.controller;

import com.greenfox.register.exception.InvalidPasswordException;
import com.greenfox.register.exception.NoSuchAccountException;
import com.greenfox.register.model.Attributes;
import com.greenfox.register.service.JwtCreator;
import com.greenfox.register.model.Account;
import com.greenfox.register.model.Data;
import com.greenfox.register.model.RequestData;
import com.greenfox.register.repository.AccountRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class UserServiceController {

  AccountRepository accountRepository;
  JwtCreator jwtCreator;

  @Autowired
  public UserServiceController(AccountRepository accountRepository, JwtCreator jwtCreator) {
    this.jwtCreator = jwtCreator;
    this.accountRepository = accountRepository;
  }

  @PostMapping("/register")
  public ResponseEntity saveAccount(@RequestBody RequestData data) throws Exception {
    String jwt = jwtCreator.createJwt("hotel-booking-user-service","new user", 300000);
    Attributes attributes = (Attributes) data.getData().getAttributes();
    String email = attributes.getEmail();
    String password = attributes.getPassword();

    if (checkAccount(email)) {
      return new ResponseEntity<>(HttpStatus.CONFLICT);
    } else {
      String pw_hashed = BCrypt
          .hashpw(password, BCrypt.gensalt((Integer.parseInt(System.getenv("LOG_ROUNDS")))));
      accountRepository.save(new Account(email, false, jwt, pw_hashed));
      RequestData response = buildJson(email);
      return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
  }

  @PostMapping("/login")
  public ResponseEntity authenticateAccount(@RequestBody RequestData data) throws Exception {
    // get object from json
    Attributes attributes = (Attributes) data.getData().getAttributes();
    String email = attributes.getEmail();
    String password = attributes.getPassword();

    try {
      authenticate(email,password);
      RequestData response = buildJson(email);
      return new ResponseEntity<>(response, HttpStatus.OK);
    } catch (NoSuchAccountException|InvalidPasswordException e) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
  }

  private void authenticate(String email, String password) throws Exception {
    Account account;
    if (checkAccount(email)) {
      throw new NoSuchAccountException("Invalid email");
    } else {
      account = accountRepository.findAccountByEmail(email);
    }
    if (!checkPassword(password, account.getPassword())) {
      throw new InvalidPasswordException("Invalid password");
    };
  }

  private RequestData buildJson(String email) {
    Account responseAccount = accountRepository.findAccountByEmail(email);
    Data responseData = new Data("user",responseAccount);
    return new RequestData(responseData);
  }

  private boolean checkAccount(String email) {
    return (accountRepository.findAccountByEmail(email) == null);
  }

  private boolean checkPassword(String password, String pw_hashed) {
    return BCrypt.checkpw(password, pw_hashed);
  }
}