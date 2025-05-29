package com.lookoutstl;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

public class RedirectServlet extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        
        // Don't redirect ACME challenge requests
        if (uri.startsWith("/.well-known/acme-challenge/")) {
            return;
        }
        
        // Only redirect HTTPS to HTTP
        if (request.isSecure()) {
            String redirectUrl = "http://lookoutstl.com" + uri;
            if (queryString != null) {
                redirectUrl += "?" + queryString;
            }
            response.sendRedirect(redirectUrl);
        } else {
            // Forward HTTP requests to the default servlet
            RequestDispatcher dispatcher = request.getRequestDispatcher(uri);
            dispatcher.forward(request, response);
        }
    }
}