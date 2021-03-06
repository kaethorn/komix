package de.wasenweg.alfred.scanner;

import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/scan")
@RequiredArgsConstructor
public class ScannerController {

  private final ScannerService scannerService;

  @GetMapping("/start")
  public Flux<ServerSentEvent<String>> startScanProgress() {
    return this.scannerService.scan();
  }

  @GetMapping("/resume")
  public Flux<ServerSentEvent<String>> resumeScanProgress() {
    return this.scannerService.resume();
  }
}