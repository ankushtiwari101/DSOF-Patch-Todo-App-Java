/*
 * The MIT License
 *
 *  Copyright (c) 2013, benas (md.benhassine@gmail.com)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package net.benas.todolist.web.springmvc.controller;

import net.benas.todolist.core.domain.Status;
import net.benas.todolist.core.domain.Todo;
import net.benas.todolist.core.domain.User;
import net.benas.todolist.core.service.api.TodoService;
import net.benas.todolist.core.service.api.UserService;
import net.benas.todolist.web.common.form.ChangePasswordForm;
import net.benas.todolist.web.common.form.RegistrationForm;
import net.benas.todolist.web.springmvc.util.SessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.List;
import java.util.Locale;

/**
 * Controller for user account operations.
 * @author benas (md.benhassine@gmail.com)
 */
@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TodoService todoService;

    @Autowired
    private MessageSource messageProvider;

    @Autowired
    private SessionData sessionData;

    /*
    **********************
    * Registration Process
    **********************
    */
    @RequestMapping("/register")
    public ModelAndView redirectToRegister() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("registerTabStyle", "active");
        modelAndView.addObject("registrationForm", new RegistrationForm());
        modelAndView.setViewName("user/register");
        return modelAndView;
    }

    @RequestMapping(value = "/register.do" , method = RequestMethod.POST)
    public String register(@Valid RegistrationForm registrationForm, BindingResult bindingResult, Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", messageProvider.getMessage("register.error.global", null, sessionData.getLocale()));
            return "user/register";
        }

        if (!registrationForm.getPassword().equals(registrationForm.getConfirmationPassword())) {
            model.addAttribute("error", messageProvider.getMessage("register.error.password.confirmation.error", null, sessionData.getLocale()));
            return "user/register";
        }

        if (userService.getUserByEmail(registrationForm.getEmail()) != null) {
            model.addAttribute("error", messageProvider.getMessage("register.error.global.account", new Object[]{registrationForm.getEmail()}, sessionData.getLocale()));
            return "user/register";
        }

        User user = new User();
        user.setFirstname(registrationForm.getFirstname());
        user.setLastname(registrationForm.getLastname());
        user.setEmail(registrationForm.getEmail());
        user.setPassword(registrationForm.getPassword());

        user = userService.create(user);
        sessionData.setUser(user);
        sessionData.setLocale(Locale.ENGLISH);

        return "redirect:/user/todos";
    }

    /*
    **********************
    * Home page
    **********************
    */

    @RequestMapping("/user/todos")
    public ModelAndView loadTodo() {

        ModelAndView modelAndView = new ModelAndView();
        // user login ensured by login filter/interceptor
        List<Todo> todoList = todoService.getTodoListByUser(sessionData.getUser().getId());
        modelAndView.addObject("todoList", todoList);
        modelAndView.addObject("homeTabStyle", "active");
        modelAndView.setViewName("user/home");
        return modelAndView;

    }

    /*
    **********************
    * Account details page
    **********************
    */

    @RequestMapping("/user/account")
    public ModelAndView redirectToAccountPage() {
        ModelAndView modelAndView = new ModelAndView("user/account/details");
        final User user = sessionData.getUser();
        modelAndView.addObject("user", user);
        modelAndView.addObject("todoCount", todoService.getTodoListByStatus(user.getId(), Status.TODO).size());
        modelAndView.addObject("doneCount", todoService.getTodoListByStatus(user.getId(), Status.DONE).size());
        modelAndView.addObject("totalCount", (todoService.getTodoListByUser(user.getId()).size()));
        return modelAndView;
    }

    /*
    **********************
    * Delete Account
    **********************
    */

    @RequestMapping("/user/account/delete")
    public String redirectToDeleteAccountPage() {
        return "user/account/delete";
    }

    @RequestMapping(value = "/user/account/delete.do", method = RequestMethod.POST)
    public String deleteAccount(HttpSession session) {
        userService.remove(sessionData.getUser());
        sessionData.setUser(null);
        session.invalidate();
        return "index";
    }

    /*
    **********************
    * Change password
    **********************
    */
    @RequestMapping("/user/account/password")
    public String redirectToChangePasswordPage(Model model) {
        model.addAttribute("changePasswordForm", new ChangePasswordForm());
        return "user/account/password";
    }

    @RequestMapping(value = "/user/account/password.do", method = RequestMethod.POST)
    public String changePassword(@Valid ChangePasswordForm changePasswordForm, BindingResult bindingResult, Model model) {
        final User user = sessionData.getUser();
        if (bindingResult.hasErrors()) {
            return null;
        }
        if (!changePasswordForm.getPassword().equals(changePasswordForm.getConfirmpassword())) {
            model.addAttribute("error", messageProvider.getMessage("account.password.confirmation.error", null, sessionData.getLocale()));
            return null;
        }
        if (!user.getPassword().equals(changePasswordForm.getCurrentpassword())) {
            model.addAttribute("error", messageProvider.getMessage("account.password.error", null, sessionData.getLocale()));
            return null;
        } else { // validation ok
            user.setPassword(changePasswordForm.getPassword());
            userService.update(user);
            return "redirect:/user/account";
        }
    }

    /*
    *****************************
    * Update personal information
    *****************************
    */
    @RequestMapping("/user/account/update")
    public String redirectToUpdatePersonalInformationPage(Model model) {
        final User user = sessionData.getUser();
        model.addAttribute("user", user);
        return "user/account/update";
    }

    @RequestMapping(value = "/user/account/update.do", method = RequestMethod.POST)
    public String updatePersonalInformation(@RequestParam String firstname, @RequestParam String lastname, @RequestParam String email, Model model) {
        User user = sessionData.getUser();

        if (userService.getUserByEmail(email) != null && !email.equals(user.getEmail())) {
            model.addAttribute("error", messageProvider.getMessage("account.email.alreadyUsed", new Object[]{email}, sessionData.getLocale()));
            model.addAttribute("user", user);
            return null;
        } else { // validation ok
            user.setFirstname(firstname);
            user.setLastname(lastname);
            user.setEmail(email);
            userService.update(user);
            return "redirect:/user/account";
        }
    }

}