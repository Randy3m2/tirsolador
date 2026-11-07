package com.tirso.tirsolador;

import org.json.JSONException;
import org.json.JSONObject;

public final class Game {
    public final String id, title, description, coverName;
    public final Console console;
    public Game(String id, String title, Console console, String description, String coverName) {
        this.id = id; this.title = title; this.console = console;
        this.description = description; this.coverName = coverName;
    }
    static Game fromJson(JSONObject j) throws JSONException {
        String id = j.getString("id"), title = j.getString("title");
        if (id.trim().isEmpty() || title.trim().isEmpty() || id.length() > 200 || title.length() > 300)
            throw new JSONException("ID o título inválido");
        try {
            return new Game(id, title, Console.valueOf(j.getString("console")),
                    j.optString("description", "Sin descripción"), j.optString("coverName", title));
        } catch (IllegalArgumentException e) { throw new JSONException("Consola desconocida"); }
    }
    JSONObject toJson() throws JSONException {
        return new JSONObject().put("id", id).put("title", title).put("console", console.name())
                .put("description", description).put("coverName", coverName);
    }
}
