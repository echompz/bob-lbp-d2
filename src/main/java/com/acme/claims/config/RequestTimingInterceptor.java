package com.acme.claims.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Logs the duration of every API call.
 */
public class RequestTimingInterceptor extends HandlerInterceptorAdapter {

    private static final Logger log = LoggerFactory.getLogger(RequestTimingInterceptor.class);

    private static final String START_ATTRIBUTE = RequestTimingInterceptor.class.getName() + ".start";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_ATTRIBUTE, new Long(System.currentTimeMillis()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        Long start = (Long) request.getAttribute(START_ATTRIBUTE);
        if (start != null) {
            long took = System.currentTimeMillis() - start.longValue();
            log.debug(request.getMethod() + " " + request.getRequestURI() + " -> " + response.getStatus()
                    + " in " + took + " ms");
        }
    }
}
