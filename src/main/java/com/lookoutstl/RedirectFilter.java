package com.lookoutstl;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

public class RedirectFilter implements Filter {
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String uri = httpRequest.getRequestURI();
        String queryString = httpRequest.getQueryString();
        
        // Don't redirect ACME challenge requests
        if (uri.startsWith("/.well-known/acme-challenge/")) {
            chain.doFilter(request, response);
            return;
        }
        
        // Only redirect HTTPS to HTTP
        if (httpRequest.isSecure()) {
            String redirectUrl = "http://lookoutstl.com" + uri;
            if (queryString != null) {
                redirectUrl += "?" + queryString;
            }
            httpResponse.sendRedirect(redirectUrl);
        } else {
            chain.doFilter(request, response);
        }
    }

    public void destroy() {
    }
} 