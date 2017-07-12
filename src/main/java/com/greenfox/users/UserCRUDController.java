package com.greenfox.users;

import com.greenfox.guardian.model.Error;
import com.greenfox.guardian.model.ErrorResponse;
import com.greenfox.register.model.Account;
import com.greenfox.register.model.Data;
import com.greenfox.register.model.RequestData;
import com.greenfox.register.repository.AccountRepository;
import com.greenfox.users.model.Links;
import com.greenfox.users.service.UserCrudService;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserCRUDController {

  @Autowired
  AccountRepository accountRepository;
  Page<Account> responsePage;
  UserCrudService userCrudService;

  @Autowired
  public UserCRUDController(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  @GetMapping("/api/users")
  public ResponseEntity returnUsers(
      @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
      @RequestParam(value = "admin", required = false) boolean admin,
      HttpServletRequest request) {

    if (userCrudService.isInQuery(request, "admin")) {
      responsePage = userCrudService.adminFilterService(admin, page);
      System.out.println(userCrudService.getQueryValue(request, "admin"));
    } else {
      responsePage = accountRepository.findAll(new PageRequest(page, 20));
    }

    Links links = new Links();

    links.setSelf(
        request.getRequestURL().toString() + (request.getQueryString() != null ? "?" + request
            .getQueryString() : ""));
    if (responsePage.hasNext()) {
      links.setNext(request.getRequestURL() + "?page=" + (page + 1));
      links.setLast(request.getRequestURL() + "?page=" + responsePage.getTotalPages());
    }
    if (responsePage.hasPrevious()) {
      links.setPrev(request.getRequestURL() + (request.getQueryString().endsWith("page=1") ? ""
          : "?page=" + (page - 1)));
    }

    Data data = new Data(links, "user", responsePage.getContent());
    RequestData requestData = new RequestData(data);
    return new ResponseEntity<>(requestData, HttpStatus.OK);
  }


  @GetMapping("/api/users/{userId}")
  public ResponseEntity returnUser(@PathVariable(required = false) Long userId,
      HttpServletRequest request) {
    Links links = new Links();

    if (!(accountRepository.findOneById(userId, new PageRequest(0, 1)).getContent().size() == 0)) {
      responsePage = accountRepository.findOneById(userId, new PageRequest(0, 1));
    } else {
      List<Error> tempList = new ArrayList<>();
      Error tempError = new Error("404",
          "Not Found",
          "No users found by id: " + userId);
      tempList.add(tempError);
      ErrorResponse tempResp = new ErrorResponse(tempList);
      return new ResponseEntity<>(tempResp, HttpStatus.NOT_FOUND);
    }

    links.setSelf(
        request.getRequestURL().toString() + (request.getQueryString() != null ? "?" + request
            .getQueryString() : ""));

    Data data = new Data(links, "user", responsePage.getContent());
    RequestData requestData = new RequestData(data);
    return new ResponseEntity<>(requestData, HttpStatus.OK);
  }

  @DeleteMapping(value = "/api/users/{userId}", produces = "application/json")
  public ResponseEntity deleteUser(@PathVariable(required = false) Long userId,
      HttpServletRequest request) {
    if (!(accountRepository.findOneById(userId, new PageRequest(0, 1)).getContent().size() == 0)) {
      accountRepository.delete(userId);
      return new ResponseEntity<>("{}", HttpStatus.OK);
    } else {
      List<Error> tempList = new ArrayList<>();
      Error tempError = new Error("404",
          "Not Found",
          "No users found by id: " + userId);
      tempList.add(tempError);
      ErrorResponse tempResp = new ErrorResponse(tempList);
      return new ResponseEntity<>(tempResp, HttpStatus.NOT_FOUND);
    }
  }

  @PatchMapping(value = "/api/users/{userId}")
  public ResponseEntity updateUser(@PathVariable(required = false) Long userId,
      HttpServletRequest request, @RequestBody RequestData requestData) {
    if (!(accountRepository.findOneById(userId, new PageRequest(0, 1)).getContent().size() == 0)) {
      System.out.println(requestData);
      Account accountToUpdate = accountRepository.findOne(userId);
      accountToUpdate.setId(userId + 1);
      accountToUpdate.setAdmin(false);
      accountRepository.save(accountToUpdate);
      return new ResponseEntity<>("{}", HttpStatus.OK);
    } else {
      List<Error> tempList = new ArrayList<>();
      Error tempError = new Error("404",
          "Not Found",
          "No users found by id: " + userId);
      tempList.add(tempError);
      ErrorResponse tempResp = new ErrorResponse(tempList);
      return new ResponseEntity<>(tempResp, HttpStatus.NOT_FOUND);
    }
  }
}
