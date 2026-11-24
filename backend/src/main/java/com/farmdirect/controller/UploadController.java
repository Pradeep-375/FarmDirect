package com.farmdirect.controller;

import com.farmdirect.service.UploadService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController @RequestMapping("/api/uploads")
public class UploadController {
  private final UploadService uploads;
  public UploadController(UploadService uploads) { this.uploads = uploads; }
  @PostMapping public Map<String, String> upload(@RequestParam("file") MultipartFile file, @RequestParam(defaultValue = "false") boolean document) {
    return Map.of("url", uploads.store(file, document));
  }
}
