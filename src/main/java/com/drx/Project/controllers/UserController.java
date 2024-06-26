package com.drx.Project.controllers;

import com.drx.Project.model.User;
import com.drx.Project.model.UserDto;
import com.drx.Project.security.PasswordService;
import com.drx.Project.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;
import java.util.List;

@Controller
@AllArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordService passwordService;

    @GetMapping("/userList")
    public String showAdminPanel(Model model, Principal principal) {
        model.addAttribute("username", getUserFullName(principal));
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "users/userList";
    }

    @GetMapping("/userUpdate/{id}")
    public String updateUser(@PathVariable("id") int id, Model model, Principal principal) {
        model.addAttribute("username", getUserFullName(principal));
        User user = userService.getUserById(id).get();

        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setPassword(user.getPassword());
        userDto.setFullName(user.getFullName());
        userDto.setRoles(user.getRoles());

        model.addAttribute("userDto", userDto);
        return "users/userUpdate";
    }

    @PostMapping("/userUpdate")
    public String updateUser(@Valid @ModelAttribute UserDto userDto, BindingResult result) {
        // get user from db by ID
        User existingUser = userService.getUserById(userDto.getId()).get();
        // verify if username already exists
        if (!existingUser.getUsername().equals(userDto.getUsername()) &&
                userService.existsByUsername(userDto.getUsername())) {
            result.addError(new FieldError("itemDto", "username", "username already exists"));
            return "users/userUpdate"; // return form with error
        } else {
            if (result.hasErrors()) {
                return "users/userUpdate";
            }
            // saving to db
            userService.saveToDb(convertToUserDto(userDto, existingUser));

            return "redirect:/userList";
        }
    }

    @GetMapping("/userCreate")
    public String createUserForm(Model model, Principal principal) {
        //Add user full name to html page
        model.addAttribute("username", getUserFullName(principal));
        model.addAttribute("userDto", new UserDto());
        return "users/userCreate";
    }

    @PostMapping("/userCreate")
    public String createUser(@Valid @ModelAttribute UserDto userDto, BindingResult result) {
        if (userService.existsByUsername(userDto.getUsername())) {
            result.addError(new FieldError("user", "username", "username already exists"));
            return "users/userCreate"; // return form with error
        } else {

            if (result.hasErrors()) {
                return "users/userCreate";
            }

            userService.saveToDb(convertToUserDto(userDto, new User()));
            return "redirect:/userList";
        }
    }

    @GetMapping("userDelete/{id}")
    public String deleteUser(@PathVariable("id") int id) {
        userService.deleteUserAndRoles(id);
        return "redirect:/userList";
    }


    private String getUserFullName(Principal principal) {
        User user = userService.getUserByUsername(principal.getName()).get();
        return user.getFullName();
    }

    private User convertToUserDto(UserDto userDto, User user) {
        //         update fields
        user.setUsername(userDto.getUsername());
        user.setPassword(encryptPassword(userDto.getPassword()));
        user.setFullName(userDto.getFullName());
        user.setRoles(userDto.getRoles());
        return user;
    }

    //encrypting pass
    private String encryptPassword(String password) {
        return passwordService.encryptPassword(password);
    }

}
