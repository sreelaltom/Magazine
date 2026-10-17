package com.magazine.controller;

import com.magazine.model.Magazine;
import com.magazine.service.MagazineService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URLConnection;
import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

    private final MagazineService magazineService;

    public HomeController(MagazineService magazineService) {
        this.magazineService = magazineService;
    }

    /**
     * Home Page: Displays modern card view of all magazines with search and category filtering
     */
    @GetMapping("/")
    public String index(@RequestParam(value = "search", required = false) String search,
                        @RequestParam(value = "category", required = false) String category,
                        Model model) {
        List<Magazine> magazines;
        if (search != null && !search.trim().isEmpty()) {
            magazines = magazineService.searchMagazines(search.trim());
            model.addAttribute("searchQuery", search.trim());
            model.addAttribute("selectedCategory", "All");
        } else if (category != null && !category.trim().isEmpty() && !"All".equalsIgnoreCase(category.trim())) {
            magazines = magazineService.getMagazinesByCategory(category.trim());
            model.addAttribute("searchQuery", "");
            model.addAttribute("selectedCategory", category.trim());
        } else {
            magazines = magazineService.getAllMagazines();
            model.addAttribute("searchQuery", "");
            model.addAttribute("selectedCategory", "All");
        }

        List<String> categories = java.util.Arrays.asList(
                "All", "NSS", "NCC", "Sports", "Technology", "Design & Art", "Academics", "Cultural", "General"
        );

        model.addAttribute("magazines", magazines);
        model.addAttribute("totalCount", magazines.size());
        model.addAttribute("categories", categories);
        return "index";
    }

    /**
     * Stream PDF for inline preview in the browser reader
     */
    @GetMapping("/view/{id}")
    public ResponseEntity<Resource> viewPdf(@PathVariable Long id) {
        Optional<Magazine> opt = magazineService.getMagazineById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Magazine magazine = opt.get();
        Resource resource = magazineService.loadFileAsResource(magazine.getPdfFileName());
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + magazine.getTitle() + ".pdf\"")
                .body(resource);
    }

    /**
     * Download PDF file as attachment
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable Long id) {
        Optional<Magazine> opt = magazineService.getMagazineById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Magazine magazine = opt.get();
        Resource resource = magazineService.loadFileAsResource(magazine.getPdfFileName());
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + magazine.getTitle() + ".pdf\"")
                .body(resource);
    }

    /**
     * Serve uploaded covers / media
     */
    @GetMapping("/uploads/{fileName:.+}")
    public ResponseEntity<Resource> serveCover(@PathVariable String fileName) {
        Resource resource = magazineService.loadFileAsResource(fileName);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        String mimeType = URLConnection.guessContentTypeFromName(fileName);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .body(resource);
    }
}
