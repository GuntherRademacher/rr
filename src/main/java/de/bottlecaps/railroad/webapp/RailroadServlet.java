package de.bottlecaps.railroad.webapp;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import de.bottlecaps.webapp.servlet.ServletRequest;
import de.bottlecaps.webapp.servlet.ServletResponse;

@MultipartConfig
public class RailroadServlet extends HttpServlet
{
  private static final long serialVersionUID = -8907590040450734599L;

  private RailroadWebApp railroadWebApp = new RailroadWebApp();

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException
  {
    ServletRequest servletRequest = new ServletRequest(request);
    railroadWebApp.doRequest(servletRequest, new ServletResponse(response));
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException
  {
    ServletRequest servletRequest = new ServletRequest(request);
    railroadWebApp.doRequest(servletRequest, new ServletResponse(response));
  }

}
