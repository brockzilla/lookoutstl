package com.lookoutstl;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

public class RedirectServlet extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/.well-known/acme-challenge/")) {
            response.sendRedirect("http://lookoutstl.com" + uri);
        }
    }
}