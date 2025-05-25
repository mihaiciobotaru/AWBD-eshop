package com.mihaiciobotaru.eshop.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements org.springframework.boot.web.servlet.error.ErrorController {
  private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus httpStatus = null;
        String errorMessage = "An unexpected error occurred. Please try again later.";

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            httpStatus = HttpStatus.resolve(statusCode);

            switch (statusCode) {
                case 404 -> {
                    errorMessage = "The page you are looking for could not be found.";
                    logger.warn("Error 404 (Not Found): {} - URL: {}", request.getAttribute(RequestDispatcher.ERROR_MESSAGE), request.getRequestURI());
                }
                case 500 -> {
                    errorMessage = "We're sorry, but there was a problem with the server. Please try again later.";

                    Throwable exception = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
                    if (exception != null) {
                        logger.error("Error 500 (Internal Server Error): {}", exception.getMessage(), exception);
                    } else {
                        logger.error("Error 500 (Internal Server Error) without specified exception: {}", request.getAttribute(RequestDispatcher.ERROR_MESSAGE));
                    }
                }
                default -> {
                    errorMessage = "An error occurred: " + (httpStatus != null ? httpStatus.getReasonPhrase() : "unknown");
                    logger.error("Error {}: {} - URL: {}", statusCode, request.getAttribute(RequestDispatcher.ERROR_MESSAGE), request.getRequestURI());
                }
            }
        } else {
            logger.error("An error occurred, but the status code could not be determined.");
        }

        // Add attributes to the model to be displayed in the HTML page
        model.addAttribute("statusCode", status != null ? status.toString() : "N/A");
        model.addAttribute("errorTitle", httpStatus != null ? httpStatus.getReasonPhrase() : "Error");
        model.addAttribute("errorMessage", errorMessage);

        return "error"; 
    }
}
