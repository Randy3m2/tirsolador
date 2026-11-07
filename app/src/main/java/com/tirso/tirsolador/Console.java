package com.tirso.tirsolador;

public enum Console {
    NES("NES", "Nintendo - Nintendo Entertainment System", "nes", 0xFFB83B52),
    SNES("SNES", "Nintendo - Super Nintendo Entertainment System", "snes", 0xFF8D66BE),
    MD("Sega Mega Drive", "Sega - Mega Drive - Genesis", "megadrive", 0xFF3069AC),
    GB("Game Boy", "Nintendo - Game Boy", "gb", 0xFF5A886D),
    GBC("Game Boy Color", "Nintendo - Game Boy Color", "gbc", 0xFFB47F44),
    GBA("Game Boy Advance", "Nintendo - Game Boy Advance", "gba", 0xFF7158AC),
    N64("Nintendo 64", "Nintendo - Nintendo 64", "n64", 0xFF347E99),
    MAME("MAME", "MAME", "mame", 0xFF3578A1),
    NDS("Nintendo DS", "Nintendo - Nintendo DS", "nds", 0xFFB34C65);

    public final String title, thumbnailSystem, logo;
    public final int color;
    Console(String title, String thumbnailSystem, String logo, int color) {
        this.title = title; this.thumbnailSystem = thumbnailSystem; this.logo = logo; this.color = color;
    }
}
