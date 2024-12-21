package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ProxyCache {

    // Fonctions pour separer un string
    public static String getRealName(String request){
        if(request != null){
            String[] parts = request.split(" ");
            String result = parts[1].substring(1);
            return result;
        } 

        return "";
    }

    public static void listCacheKeys(Map<String, String> cache) {
        if (cache.isEmpty()) {
            System.out.println("Le cache est vide.");
        } else {
            System.out.println("Clés dans le cache :");
            for (String key : cache.keySet()) {
                System.out.println(key);
            }
        }
    }

    public static void listCacheElements(Map<String, String> cache) {
        if (cache.isEmpty()) {
            System.out.println("Le cache est vide.");
        } else {
            System.out.println("Contenu du cache :");
            int index = 1;
            for (Map.Entry<String, String> entry : cache.entrySet()) {
                System.out.println(index + ". " + getRealName(entry.getKey()));
                index++;
            }
        }
    }

    // Fonction pour vider le cache
    public static void clearCache(Map<String, String> cache) {
        cache.clear();
        System.out.println("Cache vidé !");
    }

    // Fonction pour vider automatiquement le cache après un délai
    public static void autoClearCache(Map<String, String> cache,long time, TimeUnit unit) {
        Timer timer = new Timer(true); 
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                clearCache(cache); 
                System.out.println("Cache vidé automatiquement !");
            }
        }, unit.toMillis(time), unit.toMillis(time));
    }

    // Fonction pour supprimer un élément par son indice
    public static void deleteElementByIndex(Map<String, String> cache,int index) {
        if (index < 1 || index > cache.size()) {
            System.out.println("Indice invalide. Veuillez entrer un indice entre 1 et " + cache.size());
            return;
        }

        int currentIndex = 1;
        Iterator<Map.Entry<String, String>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (currentIndex == index) {
                iterator.remove();
                System.out.println("Élément supprimé : " + entry.getKey());
                return;
            }
            currentIndex++;
        }
    }

    // Fonction supplémentaire : ajouter un élément au cache
    public static void addToCache(Map<String, String> cache,String key, String value) {
        cache.put(key, value);
        System.out.println("Ajouté au cache : " + key + " -> " + value);
    }

    // Fonction supplémentaire : rechercher un élément par clé
    public static void searchCacheByKey(Map<String, String> cache,String key) {
        if (cache.containsKey(key)) {
            System.out.println("Trouvé : " + key + " -> " + cache.get(key));
        } else {
            System.out.println("Clé " + key + " non trouvée dans le cache.");
        }
    }
    
    public static void searchCacheByName(Map<String, String> cache,String name) {
        int cnt =0;
        for (Map.Entry<String, String> entry : cache.entrySet()) {
            if(entry.getKey().contains(name)){
                System.out.println("Trouvé : " + name + " -> " + entry.getKey());
            }
            cnt++;
        }
        if(cnt == 0){
            System.out.println("Clé " + name + " non trouvée dans le cache.");
        }
    }

    public static TimeUnit getTimeUnit(String unit) {
        if (unit == null) {
            System.out.println("Unité de temps non spécifiée. Valeur par défaut : SECONDS.");
            return TimeUnit.SECONDS;
        }
    
        switch (unit.toLowerCase()) { // Utilisez toLowerCase pour éviter les problèmes de casse
            case "secondes":
            case "s":
                return TimeUnit.SECONDS;
            case "minutes":
            case "m":
                return TimeUnit.MINUTES;
            case "heures":
            case "h":
                return TimeUnit.HOURS;
            default:
                System.out.println("Unité de temps invalide. Utilisez 'secondes', 'minutes' ou 'heures'. Valeur par défaut : SECONDS.");
                return TimeUnit.SECONDS; // Valeur par défaut
        }
    }
    
}