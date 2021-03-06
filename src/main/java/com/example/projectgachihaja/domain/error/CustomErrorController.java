package com.example.projectgachihaja.domain.error;

import com.example.projectgachihaja.domain.account.Account;
import com.example.projectgachihaja.domain.account.CurrentAccount;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {
    private final String VIEW_PATH = "error/";

    @RequestMapping(value = "/error")
    public String handleError(@CurrentAccount Account account, HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        model.addAttribute(account);
        if(status != null){
            int statusCode = Integer.parseInt(status.toString());
            if(statusCode == HttpStatus.BAD_REQUEST.value()){
                return VIEW_PATH + "400";
            }
            if(statusCode == HttpStatus.NOT_FOUND.value()){
                return VIEW_PATH + "404";
            }
            if(statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()){
                return VIEW_PATH + "500";
            }
        }
        return "error";
    }

}
