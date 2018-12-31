package de.wasenweg.comix;

import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("${spring.data.rest.base-path}")
@RestController
public class ComicScannerController {

    @RequestMapping("/scan")
    public String scan() throws IOException {
    	return ComicScanner.run();
    }
}