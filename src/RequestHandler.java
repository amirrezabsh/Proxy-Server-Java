import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.StringTokenizer;


public class RequestHandler implements Runnable {
    //    protected DataInputStream clientInputStream;
    protected File visitedUrlsFile;
    protected ArrayList<String> visitedUrls;
    protected ArrayList<String> blockedUrls;
    protected OutputStream clientOutputStream;
    protected InputStream remoteInputStream;
    protected OutputStream remoteOutputStream;
    protected Socket clientSocket;
    protected Socket remoteSocket;
    protected String requestType = "";
    protected String url = "";
    protected String uri = "";
    protected String httpVersion = "";
    protected HashMap<String, String> headers;
    protected BufferedReader clientBufferedReader;
    protected BufferedWriter clientBufferedWriter;
    protected boolean isBlocked = false;
    protected boolean isExist = false;
    protected boolean isCashed = false;
    static HashMap<String, File> cache;
    protected String fileName;

    static String EOF = "\r\n";

    public RequestHandler(Socket clientSocket, ArrayList<String> blockedUrls, HashMap<String, File> cache) {
        this.blockedUrls = blockedUrls;
        this.cache = cache;
//        System.out.println(this.blockedUrls);
        headers = new HashMap<String, String>();
        this.clientSocket = clientSocket;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public void run() {
        try {
            clientBufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            clientOutputStream = clientSocket.getOutputStream();
            clientBufferedWriter = new BufferedWriter(new OutputStreamWriter(clientOutputStream));
            clientToProxy();

            proxyToRemote();

            remoteToClient();

            System.out.println();

            if (remoteOutputStream != null) remoteOutputStream.close();
            if (remoteInputStream != null) remoteInputStream.close();
            if (remoteSocket != null) remoteSocket.close();

            if (clientOutputStream != null) clientOutputStream.close();
            if (clientBufferedReader != null) clientBufferedReader.close();
            if (clientSocket != null) clientSocket.close();
            isCashed = false;
            isBlocked = false;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clientToProxy() {
        String line, key, value;
        StringTokenizer tokens;


        try {
            if ((line = clientBufferedReader.readLine()) != null) {
                tokens = new StringTokenizer(line);
                requestType = tokens.nextToken();
                url = tokens.nextToken();
//                System.out.println(url);
                httpVersion = tokens.nextToken();
                try {
                    visitedUrlsFile = new File("visitedUrlsFile.txt");
                    Scanner reader = new Scanner(visitedUrlsFile);
                    while (reader.hasNextLine()) {
                        if (reader.nextLine().equals(url)) {
                            isExist = true;
                            break;
                        }
                    }
                    if (!isExist) {
                        FileWriter writer = new FileWriter(visitedUrlsFile, true);
                        writer.write(url + "\n");
                        writer.close();
                    }
                    isExist = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (blockedUrls.contains(url)) {
                isBlocked = true;
                String blockedResponse = "HTTP/1.0 403 Access Forbidden \n" +
                        "User-Agent: ProxyServer/1.0\n" +
                        EOF;
                clientOutputStream.write(blockedResponse.getBytes());
                clientOutputStream.flush();
                return;
            }
            while ((line = clientBufferedReader.readLine()) != null) {

                if (line.trim().length() == 0) {
                    break;
                }

                tokens = new StringTokenizer(line);
                key = tokens.nextToken(":");
                value = line.replaceAll(key, "").replace(": ", "");
                headers.put(key.toLowerCase(), value);
            }
            getUri();

            int fileExtensionIndex = url.lastIndexOf(".");
            String fileExtension;
            fileExtension = url.substring(fileExtensionIndex, url.length());
            String fileName = url.substring(0);
            fileName = fileName.substring(fileName.indexOf('.') + 1);
            fileName = fileName.replace("/", "__");
            fileName = fileName.replace('.', '_');
            if (fileExtension.contains("/")) {
                fileExtension = fileExtension.replace("/", "__");
                fileExtension = fileExtension.replace('.', '_');
                fileExtension += ".html";
            }
            fileName = fileName + fileExtension;
            File file = new File(fileName);
//            try {
//                if (file.exists()) {
//                    System.out.println("Found " + url + "in the cache!");
//                    isCashed = true;
////                    fileExtension = file.getName().substring(file.getName().lastIndexOf('.'));
//                    String response;
//                    if ((fileExtension.contains(".png")) || fileExtension.contains(".jpg") ||
//                            fileExtension.contains(".jpeg") || fileExtension.contains(".gif")) {
//                        BufferedImage image = ImageIO.read(file);
//                        if (image == null) {
//                            System.out.println("Image " + file.getName() + " was null");
//                            response = "HTTP/1.0 404 NOT FOUND \n" +
//                                    "Proxy-agent: ProxyServer/1.0\n" +
//                                    "\r\n";
//                            clientBufferedWriter.write(response);
//                            clientBufferedWriter.flush();
//                        } else {
//                            response = "HTTP/1.0 200 OK\n" +
//                                    "Proxy-agent: ProxyServer/1.0\n" +
//                                    "\r\n";
//                            clientBufferedWriter.write(response);
//                            clientBufferedWriter.flush();
//                            ImageIO.write(image, fileExtension.substring(1), clientSocket.getOutputStream());
//                            System.out.println("Used the image in the Cache!");
//                        }
//                    } else {
//                        isCashed = true;
//                        BufferedReader cachedFileBufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
//
//                        response = "HTTP/1.0 200 OK\n" +
//                                "Proxy-agent: ProxyServer/1.0\n" +
//                                "\r\n";
//                        clientBufferedWriter.write(response);
//                        clientBufferedWriter.flush();
//
//                        while ((line = cachedFileBufferedReader.readLine()) != null) {
//                            clientBufferedWriter.write(line);
//                        }
//                        clientBufferedWriter.write(EOF);
//                        clientBufferedWriter.flush();
//                        System.out.println("Used the text in the cache!");
//                        if (cachedFileBufferedReader != null) {
//                            cachedFileBufferedReader.close();
//                        }
//                    }
////                    if (clientBufferedWriter != null) {
////                        clientBufferedWriter.close();
////                        clientOutputStream.close();
////                    }
//                }
//            } catch (Exception e) {
//            }
        } catch (
                UnknownHostException e) {
            return;
        } catch (
                SocketException e) {
            return;
        } catch (
                IOException e) {
            return;
        }

    }

    private void proxyToRemote() {
        if (isCashed) {
            return;
        }
        if (isBlocked) {
            return;
        }
        try {
            System.out.println("Request:");
            if (headers.get("host") == null) return;
            if (requestType.startsWith("CONNECT")) {
                try {
                    remoteSocket = new Socket(headers.get("host").split(":")[0], 443);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String ConnectResponse = "HTTP/1.0 200 OK\n" +
                        EOF;
                remoteOutputStream = remoteSocket.getOutputStream();

                checkRemoteStreams();
                checkClientStreams();

                String request = requestType + " " + uri + " " + httpVersion + EOF;
                remoteOutputStream.write(request.getBytes());
                System.out.print(request);

                for (String key : headers.keySet()) {
                    String line = key + ": " + headers.get(key) + EOF;
                    remoteOutputStream.write(line.getBytes());
                    System.out.print(line);
                }
                remoteOutputStream.write(EOF.getBytes());
                remoteOutputStream.flush();

                clientOutputStream.write(ConnectResponse.getBytes());
                clientOutputStream.flush();
//                return;
            } else {
                remoteSocket = new Socket(headers.get("host"), 80);
                remoteOutputStream = remoteSocket.getOutputStream();

                checkRemoteStreams();
                checkClientStreams();


                String request = requestType + " " + uri + " " + httpVersion + EOF;
                remoteOutputStream.write(request.getBytes());
                System.out.print(request);

                for (String key : headers.keySet()) {
                    String line = key + ": " + headers.get(key) + EOF;
                    System.out.print(line);
                    remoteOutputStream.write(line.getBytes());
                }

                remoteOutputStream.write(EOF.getBytes());
                remoteOutputStream.flush();

                if (requestType.startsWith("POST")) {
                    int dataLength = Integer.parseInt(headers.get("content-length"));
                    for (int i = 0; i < dataLength; i++) {
                        remoteOutputStream.write(clientBufferedReader.read());
                    }
                }

                remoteOutputStream.write(EOF.getBytes());
                remoteOutputStream.flush();

                System.out.println("=============================");

            }
        } catch (UnknownHostException e) {
            return;
        } catch (SocketException e) {
            return;
        } catch (IOException e) {
            return;
        }
    }

    private void remoteToClient() {
        if (isCashed) return;
        if (isBlocked) return;
        String line;
        DataInputStream remoteHeader;
        try {
            System.out.println("Response:");

            if (remoteSocket == null) return;
            remoteHeader = new DataInputStream(remoteSocket.getInputStream());

            while ((line = remoteHeader.readLine()) != null) {

                if (line.trim().length() == 0) {
                    break;
                }
                if (line.toLowerCase().startsWith("proxy")) continue;
                if (line.contains("keep-alive")) continue;

                System.out.println(line);
                clientOutputStream.write(line.getBytes());
                clientOutputStream.write(EOF.getBytes());
            }

            clientOutputStream.write(EOF.getBytes());
            clientOutputStream.flush();

            int fileExtensionIndex = url.lastIndexOf(".");
            String fileExtension;
            fileExtension = url.substring(fileExtensionIndex, url.length());
            String fileName = url.substring(0);
            fileName = fileName.substring(fileName.indexOf('.') + 1);

            fileName = fileName.replace("/", "__");
            fileName = fileName.replace('.', '_');
            if (fileExtension.contains("/")) {
                fileExtension = fileExtension.replace("/", "__");
                fileExtension = fileExtension.replace('.', '_');
                fileExtension += ".html";
            }
            fileName = fileName + fileExtension;


            boolean caching = true;
            File fileToCache = null;
            BufferedWriter fileToCacheBW = null;

            try {
                fileToCache = new File(fileName);

                if (!fileToCache.exists()) {
                    fileToCache.createNewFile();
                }

                fileToCacheBW = new BufferedWriter(new FileWriter(fileToCache));
            } catch (IOException e) {
                System.out.println("Couldn't cache: " + fileName);
                caching = false;
//                e.printStackTrace();
            } catch (NullPointerException e) {
                System.out.println("NPE opening file");
            }
            if ((fileExtension.contains(".png")) || fileExtension.contains(".jpg") ||
                    fileExtension.contains(".jpeg") || fileExtension.contains(".gif")) {
                URL remoteURL = new URL(url);
                BufferedImage image = ImageIO.read(remoteURL);
                if (image != null) {
                    ImageIO.write(image, fileExtension.substring(1), fileToCache);
                    line = "HTTP/1.0 200 OK\n" +
                            "Proxy-agent: ProxyServer/1.0\n" +
                            "\r\n";
                    clientBufferedWriter.write(line);
                    clientBufferedWriter.flush();
                    ImageIO.write(image, fileExtension.substring(1), clientSocket.getOutputStream());

                } else {
                    System.out.println("Sending 404 to client as image wasn't received from server"
                            + fileName);
                    String error = "HTTP/1.0 404 NOT FOUND\n" +
                            "Proxy-agent: ProxyServer/1.0\n" +
                            "\r\n";
                    clientBufferedWriter.write(error);
                    clientBufferedWriter.flush();
                    return;
                }
            } else {
                remoteInputStream = remoteSocket.getInputStream();
                byte[] bodyBuffer = new byte[1024];
                for (int i; (i = remoteInputStream.read(bodyBuffer)) != -1; ) {
                    clientOutputStream.write(bodyBuffer, 0, i);
                    clientOutputStream.flush();

                    if (caching) {
                        String string = new String(bodyBuffer);
                        fileToCacheBW.write(string);
                        fileToCacheBW.flush();
                    }
                }
            }
            System.out.println("==============================");
            if (caching) {
                fileToCacheBW.flush();
//                cache.put(url,fileToCache);
//                ProxyServer.updateCache(url,fileToCache);
//                FileOutputStream fileOutputStream = new FileOutputStream("cachedSites.txt");
//                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
//
//                objectOutputStream.writeObject(cache);
//                objectOutputStream.close();
//                fileOutputStream.close();
//                System.out.println("Cached Sites written");
            }
        } catch (UnknownHostException e) {
            return;
        } catch (SocketException e) {
            return;
        } catch (IOException e) {
            return;
        }
    }

    private void checkClientStreams() {

        try {
            if (clientSocket.isOutputShutdown()) clientOutputStream = clientSocket.getOutputStream();
            if (clientSocket.isInputShutdown())
                clientBufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (UnknownHostException e) {
            return;
        } catch (SocketException e) {
            return;
        } catch (IOException e) {
            return;
        }
    }

    private void checkRemoteStreams() {

        try {
            if (remoteSocket.isOutputShutdown()) remoteOutputStream = remoteSocket.getOutputStream();
            if (remoteSocket.isInputShutdown())
                remoteInputStream = new DataInputStream(remoteSocket.getInputStream());
        } catch (UnknownHostException e) {
            return;
        } catch (SocketException e) {
            return;
        } catch (IOException e) {
            return;
        }
    }

    private void getUri() {
        if (headers.containsKey("host")) {
            int temp = url.indexOf(headers.get("host"));
            temp += headers.get("host").length();
//                System.out.println(temp);
            if (temp < 0) {
                uri = url;
            } else {
                uri = url.substring(temp);
            }
        }
    }

    private String getThreadID() {
        return Thread.currentThread().getId() + "";
    }

    private String getFileName(String uri) {
        return uri.substring(uri.lastIndexOf("/"));
    }
}
