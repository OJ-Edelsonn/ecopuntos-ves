package pe.ecopuntosves.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FotosPuntosCriticosService {
    // Las fotos permanecen fuera del repositorio Git y de static/.
    private final Path directorio = Path.of(System.getProperty("user.home"),
            "EcoPuntos_VES", "fotos_reportes").toAbsolutePath().normalize();

    public String guardar(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) return null;
        if (foto.getSize() > 1024 * 1024)
            throw new IllegalArgumentException("La foto no puede superar 1 MB.");
        try {
            byte[] bytes = foto.getBytes();
            boolean png = bytes.length >= 8 && (bytes[0] & 255) == 0x89 && bytes[1] == 'P'
                    && bytes[2] == 'N' && bytes[3] == 'G' && bytes[4] == 13
                    && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10;
            boolean jpg = bytes.length >= 3 && (bytes[0] & 255) == 0xFF
                    && (bytes[1] & 255) == 0xD8 && (bytes[2] & 255) == 0xFF;
            if (!png && !jpg)
                throw new IllegalArgumentException("La foto debe ser PNG o JPG válido.");
            Files.createDirectories(directorio);
            String nombre = UUID.randomUUID() + (png ? ".png" : ".jpg");
            Files.write(directorio.resolve(nombre), bytes);
            return nombre;
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo guardar la fotografía. Comprueba permisos de archivos.");
        }
    }

    public byte[] leer(String nombre) {
        if (nombre == null || !nombre.matches("[0-9a-f-]{36}\\.(png|jpg)"))
            throw new IllegalArgumentException("Fotografía no disponible.");
        Path ruta = directorio.resolve(nombre).normalize();
        if (!ruta.startsWith(directorio)) throw new IllegalArgumentException("Ruta inválida.");
        try {
            return Files.readAllBytes(ruta);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Fotografía no disponible.");
        }
    }

    public MediaType tipo(String nombre) {
        return nombre.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
    }

    public void borrarSilenciosamente(String nombre) {
        if (nombre == null || !nombre.matches("[0-9a-f-]{36}\\.(png|jpg)")) return;
        try { Files.deleteIfExists(directorio.resolve(nombre)); } catch (IOException ignored) { }
    }
}
