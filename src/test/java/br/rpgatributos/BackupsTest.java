package br.rpgatributos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupsTest {

    @Test
    void copiaUmaVezPorDiaEGuardaSoOsUltimos(@TempDir Path pasta) throws Exception {
        Files.writeString(pasta.resolve("colonias.yml"), "a: 1");
        Files.createDirectories(pasta.resolve("jogadores"));
        Files.writeString(pasta.resolve("jogadores/x.yml"), "b: 2");
        Files.writeString(pasta.resolve("pacote.zip"), "não é yml");
        LocalDate d = LocalDate.of(2026, 10, 1);

        assertEquals(2, Backups.fazer(pasta, 3, d));
        assertTrue(Files.exists(pasta.resolve("backups/2026-10-01/jogadores/x.yml")));
        assertFalse(Files.exists(pasta.resolve("backups/2026-10-01/pacote.zip")));
        // No mesmo dia não copia de novo (nem os próprios backups).
        assertEquals(0, Backups.fazer(pasta, 3, d));

        for (int i = 1; i <= 4; i++) Backups.fazer(pasta, 3, d.plusDays(i));
        try (var s = Files.list(pasta.resolve("backups"))) {
            assertEquals(3, s.count());
        }
        assertFalse(Files.exists(pasta.resolve("backups/2026-10-01")));
        assertTrue(Files.exists(pasta.resolve("backups/2026-10-05/colonias.yml")));
        assertEquals(0, Backups.fazer(pasta, 0, d.plusDays(9)));
    }
}
