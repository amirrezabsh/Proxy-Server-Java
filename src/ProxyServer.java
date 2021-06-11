import javax.imageio.IIOException;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyServer {
    public ArrayList<String> visitedUrls = new ArrayList<>();
    public Frame frame;
    protected ServerSocket server;
    protected ExecutorService executorService;
    protected static int PROXY_PORT = 9999;
    protected ArrayList<String> users = new ArrayList<>();
    public File visitedUrlsFile = new File("visitedUrlsFile.txt");
    public Scanner reader = new Scanner(visitedUrlsFile);
    static HashMap<String, File> cache;

    public ProxyServer(int port) throws FileNotFoundException {
//        try {
//            File cachedSites = new File("cachedSites.txt");
//            if (!cachedSites.exists()) {
//                System.out.println("No cached sites found - creating new file");
//                cachedSites.createNewFile();
//            } else {
//                FileInputStream fileInputStream = new FileInputStream(cachedSites);
//                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
//                cache = (HashMap<String, File>) objectInputStream.readObject();
//                fileInputStream.close();
//                objectInputStream.close();
//            }
//        } catch (IOException | ClassNotFoundException e) {
//            System.out.println("Error loading previously cached sites file");
//            e.printStackTrace();
//        }
        frame = new Frame();
        String url;
        while (reader.hasNextLine()) {
            url = reader.nextLine();
            if (!visitedUrls.contains(url)) visitedUrls.add(url);
        }
        frame.updateVisitedUrls(visitedUrls);
        executorService = Executors.newCachedThreadPool();
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void accept() {
        while (true) {
            try {
                executorService.execute(new RequestHandler(server.accept(), frame.getBlockedUrls(), cache));
                String url;
                while (reader.hasNextLine()) {
                    url = reader.nextLine();
                    if (!visitedUrls.contains(url)) visitedUrls.add(url);
                }
                frame.updateVisitedUrls(visitedUrls);
            } catch (IOException e) {
            }
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        System.out.println("ProxyServer is listening to port " + PROXY_PORT);
        ProxyServer proxy = new ProxyServer(PROXY_PORT);
        proxy.accept();
    }
    public static void updateCache(String key,File value){
        cache.put(key, value);
    }
}
