package de.bottlecaps.railroad.webapp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import com.sun.net.httpserver.HttpServer;

import de.bottlecaps.webapp.server.HttpRequest;
import de.bottlecaps.webapp.server.HttpResponse;

@SuppressWarnings({"all"})
public class RailroadServer
{
  private HttpServer httpServer;
  private int port;
  private CountDownLatch stop;

  public RailroadServer(int port) throws IOException
  {
    RailroadWebApp railroadWebApp = new RailroadWebApp();
    stop = new CountDownLatch(1);
    httpServer = HttpServer.create(new InetSocketAddress(port), 0);
    httpServer.createContext("/", httpExchange ->
    {
      String path = httpExchange.getRequestURI().getPath();
      boolean isStopRequest = "/stop".equals(path);
      try
      {
        if (isStopRequest)
        {
          httpExchange.getResponseHeaders().set("Content-Type", "text/plain");
          httpExchange.sendResponseHeaders(200, 0);
          httpExchange.getResponseBody().write("OK".getBytes(StandardCharsets.UTF_8));
        }
        else
        {
          railroadWebApp.doRequest(new HttpRequest(httpExchange), new HttpResponse(httpExchange));
        }
      }
      catch (Exception e)
      {
        System.err.println(new Date() + " Caught exception while processing " + httpExchange.getRequestMethod() + " request for " + path);
        e.printStackTrace(System.err);
      }
      finally
      {
        httpExchange.getResponseBody().close();
        httpExchange.getRequestBody().close();
        httpExchange.close();
        if (isStopRequest)
          stop.countDown();
      }
    });
    httpServer.start();
    this.port = httpServer.getAddress().getPort();
  }

  public void waitForStopRequest() throws InterruptedException
  {
    stop.await();
    httpServer.stop(1);
  }

  public int getPort()
  {
    return port;
  }
}
