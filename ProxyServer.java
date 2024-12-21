package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import server.ProxyCache;

public class ProxyServer {
    private static int PROXY_PORT;
    private static String SERVER_IP;
    private static int APACHE_PORT;
    private static int TOMCAT_PORT;
    private static int TTC;
    private static TimeUnit UNIT;


    public static Map<String, String> cache = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        // Charger les configurations depuis le fichier
        loadConfig();

        try (ServerSocket serverSocket = new ServerSocket(PROXY_PORT)) {
            System.out.println("Serveur cache démarré sur le port " + PROXY_PORT);

            new Thread(() -> Commande()).start();
            new Thread(() -> ProxyCache.autoClearCache(cache,TTC,UNIT)).start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket, cache)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadConfig() {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("proxy.config")) {
            properties.load(fis);

            // Lire les valeurs depuis le fichier
            PROXY_PORT = Integer.parseInt(properties.getProperty("proxy_port"));
            SERVER_IP = properties.getProperty("server_ip");
            APACHE_PORT = Integer.parseInt(properties.getProperty("apache_port"));
            TOMCAT_PORT = Integer.parseInt(properties.getProperty("tomcat_port"));
            TTC = Integer.parseInt(properties.getProperty("TTC"));
            UNIT = ProxyCache.getTimeUnit(properties.getProperty("unit").toString());
            

            System.out.println("Configuration chargée avec succès !");
        } catch (IOException | NumberFormatException e) {
            System.err.println("Erreur lors du chargement du fichier de configuration : " + e.getMessage());
            System.exit(1);
        }
    }

    private static void handleClient(Socket clientSocket, Map<String,String> cache) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter Out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Lire la requête HTTP du client
            String request = in.readLine();
            if (request == null || request.trim().isEmpty()) {
                System.err.println("Requête vide ou malformée reçue.");
                return;
            }
 
            if(cache.containsKey(request)){
                System.out.println("Requete récupérée dans la cache ( " + request + " ) \n");
                Out.println(cache.get(request));
            } else{
                System.out.println("Requete non trouve dans la cache . Recherche au Apache Server");
                String reponse = getAnswerFromServer(SERVER_IP,APACHE_PORT,request);

                if(reponse == null){
                    System.out.println("Requete non trouve dans la cache . Recherche au Apache Tomcat");
                    reponse = getAnswerFromServer(SERVER_IP,TOMCAT_PORT,request);
                }

                if(reponse == null){
                    String error = "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Type: text/html; charset=UTF-8\r\n" +
                            "Connection: close\r\n\r\n" +
                            "<html><body><h1>Page Introuvable</h1><p>La page que vous recherchez est introuvable.</p></body></html>";
                    Out.print(error);
                } else{
                    cache.put(request, reponse);
                    Out.print(reponse);
                }
                Out.flush();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getAnswerFromServer(String serverIp, int serverPort, String requestLine) {
        try (Socket serverSocket = new Socket(serverIp, serverPort);
             BufferedReader serverIn = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
             PrintWriter serverOut = new PrintWriter(serverSocket.getOutputStream(), true)) {

            // Construire la requête HTTP
            StringBuilder requestBuilder = new StringBuilder();
            requestBuilder.append(requestLine).append("\r\n"); // Ligne de requête
            requestBuilder.append("Host: ").append(serverIp).append("\r\n"); // En-tête Host
            requestBuilder.append("Connection: close\r\n"); // En-tête Connection
            requestBuilder.append("\r\n"); // Ligne vide pour terminer les en-têtes

            serverOut.print(requestBuilder.toString());
            serverOut.flush();

            // Lire la réponse du serveur
            String serverResponse;
            StringBuilder responseBuilder = new StringBuilder();
            boolean is404 = false;

            while ((serverResponse = serverIn.readLine()) != null) {
                responseBuilder.append(serverResponse).append("\n");
                if (serverResponse.contains("404 Not Found")) {
                    is404 = true;
                }
            }

            // Si 404 est détecté, retourner null
            if (is404) return null;

            // Si une réponse valide est trouvée, afficher un message
            System.out.println("Réponse trouvée dans le serveur " + serverIp + " sur le port " + serverPort + "\n");

            return responseBuilder.toString(); // Retourner la réponse complète
        } catch (IOException e) {
            System.err.println("Erreur lors de la connexion au serveur " + serverIp + ":" + serverPort);
            e.printStackTrace();
            return null;
        }
    }
    
    //Fonctionnalites avec commande
    private static void Commande() {
        Scanner scanner = new Scanner(System.in);
    
        while (true) {
            System.out.print("$ ");
            String commande = scanner.nextLine().trim();
    
            if (commande.startsWith("vider")) {
                ProxyCache.clearCache(cache);
            } else if (commande.startsWith("exit")) {
                System.out.println("Byeee");
                System.exit(0);
                break;
            } else if (commande.startsWith("ls") || commande.startsWith("list")) {
                ProxyCache.listCacheElements(cache);
            } /*else if (commande.startsWith("autoremove")) {
                String[] parts = commande.split(" ");
                if (parts.length == 3) {
                    try {
                        long delay = Long.parseLong(parts[1]);
                        String unit = parts[2].toLowerCase();
                        TimeUnit timeUnit;
                        switch (unit) {
                            case "secondes":
                            case "s":
                                timeUnit = TimeUnit.SECONDS;
                                break;
                            case "minutes":
                            case "m":
                                timeUnit = TimeUnit.MINUTES;
                                break;
                            case "heures":
                            case "h":
                                timeUnit = TimeUnit.HOURS;
                                break;
                            default:
                                System.out.println("Unité de temps invalide. Utilisez 'secondes', 'minutes' ou 'heures'.");
                                continue;
                        }
                        ProxyCache.autoClearCache(cache,delay, timeUnit);
                    } catch (NumberFormatException e) {
                        System.out.println("Commande invalide. Utilisation : autoremove <temps> <unité>");
                    }
                } else {
                    System.out.println("Commande invalide. Utilisation : autoremove <temps> <unité>");
                }
            }*/ else if (commande.startsWith("rm")) {
                String[] parts = commande.split(" ");
                if (parts.length == 2) {
                    try {
                        int index = Integer.parseInt(parts[1]);
                        ProxyCache.deleteElementByIndex(cache,index);
                    } catch (NumberFormatException e) {
                        System.out.println("Commande invalide. L'indice doit être un entier.");
                    }
                } else {
                    System.out.println("Commande invalide. Utilisation : rm <indice>");
                }
            } else if (commande.startsWith("grep")) {
                String[] parts = commande.split(" ");
                if (parts.length == 2) {
                    try {
                        String index = parts[1];
                        ProxyCache.searchCacheByName(cache,index);
                    } catch (NumberFormatException e) {
                        System.out.println("Commande invalide. L'indice doit être un entier.");
                    }
                } else {
                    System.out.println("Commande invalide. Utilisation : rm <indice>");
                }
            } else {
                System.out.println("UNKNOWN COMMANDE");
            }
        }
    
        scanner.close();
    }
        
}