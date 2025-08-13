package com.example.cookies;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@SpringBootApplication
public class CookiesApplication {

    public static void main(String[] args) {
        SpringApplication.run(CookiesApplication.class, args);
    }

}



@Controller
class SessionController {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String NAME = "name";

    @ResponseBody
    @GetMapping("/session")
    Map<String, Object> session(HttpServletRequest request) {
        return Map.of(
                "name" , request.getSession().getAttribute(NAME),
                "sessionId", request.getSession().getId());
    }

    @GetMapping("/")
    String durable(@RequestParam String name, Model model , HttpServletRequest request) {
        var session = request.getSession(true);
        if (session.isNew() || session.getAttribute(NAME) == null) {
            session.setAttribute(NAME, name);
            this.log.info("installing a durable value: {}", name);
        } //
        else {
            log.info("got a request for {}", name);
        }
        model.addAttribute(NAME,session.getAttribute(NAME)) ;
        return "index";
    }

}