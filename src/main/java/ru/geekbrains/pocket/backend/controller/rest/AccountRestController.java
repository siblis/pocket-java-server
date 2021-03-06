package ru.geekbrains.pocket.backend.controller.rest;

import com.mongodb.MongoWriteException;
import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.geekbrains.pocket.backend.domain.db.User;
import ru.geekbrains.pocket.backend.domain.pub.UserPub;
import ru.geekbrains.pocket.backend.exception.InvalidOldPasswordException;
import ru.geekbrains.pocket.backend.service.UserService;
import ru.geekbrains.pocket.backend.util.Constant;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@Api(tags = "Account", value = "/account")
public class AccountRestController {
    @Autowired
    private UserService userService;
    @Autowired
    private HttpRequestComponent httpRequestComponent;

    @ApiOperation(value = "Get account data",
            authorizations =  {@Authorization(value="Bearer Token")},
            notes = "Получить информацию о своем аккаунте",
            response = UserPub.class)
    @ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "token",
            required = true, dataType = "string", paramType = "header",
            example = Constant.EXAMPLE_TOKEN)})
    @GetMapping("/account") //Получить информацию о своем аккаунте
    public ResponseEntity<?> getAccount(HttpServletRequest request) {
        User user = httpRequestComponent.getUserFromToken(request);
        if (user != null)
            return new ResponseEntity<>(new UserPub(user), HttpStatus.OK);
        else
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }


    @ApiOperation(value = "Edit account data",
            notes = "Изменить данные аккаунта",
            response = UserPub.class)
    @ApiImplicitParams({@ApiImplicitParam(name = "Authorization", value = "token",
            required = true, dataType = "string", paramType = "header",
            example = Constant.EXAMPLE_TOKEN)})
    @PutMapping(path = "/account", consumes = "application/json") //Изменить данные аккаунта
    public ResponseEntity<?> editAccount(@Valid @RequestBody EditAccountRequest editAccountRequest,
                                         final BindingResult result, final HttpServletRequest request) {
        //, WebRequest webRequest, Errors errors) {
    //аргумент класса BindingResult должен быть сразу после аргумента, помеченного аннотациями @RequestBody и valid,
    // в других случаях валидация работать не будет

        if(result.hasErrors()) {
            return getResponseEntity(result);
        }
        if ((editAccountRequest.getName() == null
                && editAccountRequest.getOldPassword() == null
                && editAccountRequest.getNewPassword() == null)
            || (editAccountRequest.getOldPassword() == null
                || editAccountRequest.getNewPassword() == null) ){
            log.debug("Error: all field in editAccountRequest is null");
            return new ResponseEntity<>("Error: all field in editAccountRequest is null", HttpStatus.BAD_REQUEST);
        }
        if (editAccountRequest.getOldPassword().equals(editAccountRequest.getNewPassword())) {
            log.debug("The old & new password is match!");
            return new ResponseEntity<>("The old & new password is match!", HttpStatus.BAD_REQUEST);
        }
        User user = httpRequestComponent.getUserFromToken(request);
        if (user == null) {
            log.debug("Error: User not found!");
            return new ResponseEntity<>("Error: User not found!", HttpStatus.UNAUTHORIZED);
        }

        try {
            user = userService.updateNameAndPassword(user, editAccountRequest.getName(),
                    editAccountRequest.getOldPassword(), editAccountRequest.getNewPassword());
        } catch (InvalidOldPasswordException ex) {
            log.debug(ex.getMessage());
            return new ResponseEntity<>("The current password specified is incorrect", HttpStatus.BAD_REQUEST);
        } catch (DuplicateKeyException | MongoWriteException ex){
            log.debug(ex.getMessage());
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
        log.debug("Successfully edit user '" + user.getEmail() + "'");
        return new ResponseEntity<>(new UserPub(user), HttpStatus.OK);
    }

    private ResponseEntity<?> getResponseEntity(BindingResult result) {
        final Map<String, Object> response = new HashMap<>();
        response.put("message", "Your request contains errors");
        response.put("errors", result.getAllErrors()
                .stream()
                .map(x -> String.format("%s : %s", x.getCode(), x.getDefaultMessage()))
                .collect(Collectors.toList()));
        log.debug(response);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ApiModel(value = "Edit account request",
            description="Тело запроса в формате JSON, содержит Username если его нужно изменить и/или " +
                    "старый и новый пароль если нужно изменить пароль.")
    public static class EditAccountRequest {
        @Nullable
        @Size(min = 2, max = 32)
        @ApiModelProperty(value = "Username specified during registration. Size(min = 2, max = 32)",
                example = "petr@mail.ru", position = 0, required = true)
        private String name;
        @Nullable
        @Size(min = 8, max = 32)
        @ApiModelProperty(value = "Старый пароль. Size(min = 8, max = 32)", position = 1)
        private String oldPassword;
        @Nullable
        @Size(min = 8, max = 32)
        @ApiModelProperty(value = "Новый пароль. Size(min = 8, max = 32)", position = 2)
        private String newPassword;

        public EditAccountRequest(@Nullable @Size(min = 2, max = 32) String name) {
            this.name = name;
        }

        public EditAccountRequest(@Nullable @Size(min = 8, max = 32) String oldPassword,
                                  @Nullable @Size(min = 8, max = 32) String newPassword) {
            this.oldPassword = oldPassword;
            this.newPassword = newPassword;
        }
    }
}
