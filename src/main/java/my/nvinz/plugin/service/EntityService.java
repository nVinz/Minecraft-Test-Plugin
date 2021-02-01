package my.nvinz.plugin.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EntityService {

    // Имена созданных оцелотов
    private List<String> ocelotNames = new ArrayList<>();
    // Мапа с игроками и всеми энтити, которые были созданы ими в пакетах, для их последующего удаления
    private Map<String, List<Integer>> armorStands = new ConcurrentHashMap<>();

    public void addOcelotName(String name) {
        ocelotNames.add(name);
    }

    public void removeOcelotName(String name) {
        ocelotNames.remove(name);
    }

    public int[] getArmorStandsByName(String playerName) {
        if (armorStands.get(playerName) == null) {
            return new int[0];
        }
        List<Integer> armorStandsList = armorStands.get(playerName);
        int[] armorStandsIds = new int[armorStandsList.size()];
        for (int i = 0; i < armorStandsIds.length; i++) {
            armorStandsIds[i] = armorStandsList.get(i);
        }
        return armorStandsIds;
    }

    public void addArmorStand(String playerName, Integer standId) {
        armorStands.compute(playerName, (key, value) -> {
           if (value == null) {
               value = new ArrayList<>(Collections.singleton(standId));
           }
           else {
               value.add(standId);
           }
           return value;
        });
    }

    public void removeArmorStands(String playerName) {
        armorStands.remove(playerName);
    }

    public List<String> getOcelotNames() {
        return ocelotNames;
    }

    public Map<String, List<Integer>> getArmorStands() {
        return armorStands;
    }
}
