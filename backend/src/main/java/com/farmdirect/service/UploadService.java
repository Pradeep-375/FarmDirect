package com.farmdirect.service;

import com.farmdirect.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/** Validates by size and file signature (not the client-supplied name/type) and stores under a random name. */
@Service
public class UploadService {
  @Value("${app.upload-dir}") String dir;

  public String store(MultipartFile f, boolean allowPdf) {
    if (f.isEmpty() || f.getSize() > 5 * 1024 * 1024) throw ApiException.badRequest("File must be between 1 byte and 5 MB");
    try {
      byte[] head = f.getInputStream().readNBytes(12);
      String ext = ext(head, allowPdf);
      if (ext == null) throw ApiException.badRequest(allowPdf ? "Only JPG, PNG, WEBP or PDF files are allowed" : "Only JPG, PNG or WEBP images are allowed");
      Path root = Path.of(dir).toAbsolutePath(); Files.createDirectories(root);
      String name = UUID.randomUUID() + "." + ext;
      Files.copy(f.getInputStream(), root.resolve(name), StandardCopyOption.REPLACE_EXISTING);
      return "/uploads/" + name;
    } catch (IOException e) { throw ApiException.badRequest("Upload failed"); }
  }
  private static String ext(byte[] h, boolean pdf) {
    if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return "jpg";
    if (h.length >= 4 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return "png";
    if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F' && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') return "webp";
    if (pdf && h.length >= 4 && h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F') return "pdf";
    return null;
  }
}
