/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tests.support;

import java.io.IOException;
import java.io.InputStream;

/*
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
*/

public class Support_Jetty {
    /*
    public static Server DEFAULT_SERVER = null;

    public static int DEFAULT_PORT = 0;

    public static Server DEFAULT_SERVLET = null;

    public static int DEFAULT_SERVLET_PORT = 0;

    public static Server SERVER = null;

    public static int PORT = 0;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    stopDefaultServer();
                    stopServer();
                    stopDefaultServlet();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static class HYDefaultServlet extends HttpServlet
    {
        private static final long serialVersionUID = -7650071946216123835L;

        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            InputStream in = request.getInputStream();
            int i;
            StringBuilder s = new StringBuilder();
            while((i = in.read())!=-1){
                s.append((char)i);
            }
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print(s);
        }
    }

    private static class HYDefaultHandler extends AbstractHandler
    {
        public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException
        {
            Request base_request = (request instanceof Request) ? (Request)request:HttpConnection.getCurrentConnection().getRequest();
            base_request.setHandled(true);
            response.setContentType("text/html");
            response.addDateHeader("Date", System.currentTimeMillis());
            response.addDateHeader("Last-Modified", Support_Configuration.URLConnectionLastModified);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print("<h1>Hello OneHandler</h1>");
        }
    }
*/
    public static int startDefaultHttpServer() throws Exception{
        /*
        if (DEFAULT_SERVER != null){
            return DEFAULT_PORT;
        }
        DEFAULT_SERVER = new Server(0);
        DEFAULT_SERVER.setHandler(new HYDefaultHandler());
        Context context = new Context(DEFAULT_SERVER,"/",Context.SESSIONS);
        context.addServlet(new ServletHolder(new HYDefaultServlet()), "/servlet");
        DEFAULT_SERVER.start();
        DEFAULT_PORT = DEFAULT_SERVER.getConnectors()[0].getLocalPort();
        return DEFAULT_PORT;
        */
        return -1;
    }

    public static int startDefaultServlet() throws Exception{
        /*
        if (DEFAULT_SERVLET != null){
            return DEFAULT_SERVLET_PORT;
        }
        DEFAULT_SERVLET = new Server(0);
        Context context = new Context(DEFAULT_SERVLET,"/",Context.SESSIONS);
        context.addServlet(new ServletHolder(new HYDefaultServlet()), "/*");
        DEFAULT_SERVLET.start();
        DEFAULT_SERVLET_PORT = DEFAULT_SERVLET.getConnectors()[0].getLocalPort();
        return DEFAULT_SERVLET_PORT;
        */
        return -1;
    }

    public static int startHttpServerWithDocRoot(String root) throws Exception {
        /*
        if (SERVER != null) {
            SERVER.stop();
            SERVER = null;
        }
        SERVER = new Server(0);
        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setResourceBase(root);
        SERVER.setHandler(resource_handler);
        SERVER.start();
        PORT = SERVER.getConnectors()[0].getLocalPort();
        return PORT;
        */
        return -1;
    }

    private static void stopDefaultServer() throws Exception {
        /*
        if (DEFAULT_SERVER != null) {
            DEFAULT_SERVER.stop();
            DEFAULT_SERVER = null;
        }
        */
    }

    private static void stopServer() throws Exception {
        /*
        if (SERVER != null) {
            SERVER.stop();
            SERVER = null;
        }
        */
    }

    private static void stopDefaultServlet() throws Exception {
        /*
        if (DEFAULT_SERVLET != null) {
            DEFAULT_SERVLET.stop();
            DEFAULT_SERVLET = null;
        }
        */
    }
}
