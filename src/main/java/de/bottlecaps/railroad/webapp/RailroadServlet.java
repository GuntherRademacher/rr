package de.bottlecaps.railroad.webapp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
