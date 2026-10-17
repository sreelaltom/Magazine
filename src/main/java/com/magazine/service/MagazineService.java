package com.magazine.service;

import com.magazine.model.Magazine;
import com.magazine.repository.MagazineRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class MagazineService {

    private final MagazineRepository magazineRepository;
    private final Path uploadDir;

    public MagazineService(MagazineRepository magazineRepository,
                           @Value("${app.upload.dir:./uploads}") String uploadPath) {
        this.magazineRepository = magazineRepository;
        this.uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            System.err.println("Could not initialize upload folder: " + e.getMessage());
        }
    }

    public List<Magazine> getAllMagazines() {
        return magazineRepository.findAllByOrderByUploadedAtDesc();
    }

    public List<Magazine> searchMagazines(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllMagazines();
        }
        return magazineRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByUploadedAtDesc(
                keyword.trim(), keyword.trim()
        );
    }

    public List<Magazine> getMagazinesByCategory(String category) {
        if (category == null || category.trim().isEmpty() || "All".equalsIgnoreCase(category.trim())) {
            return getAllMagazines();
        }
        return magazineRepository.findByCategoryIgnoreCaseOrderByUploadedAtDesc(category.trim());
    }

    public Optional<Magazine> getMagazineById(Long id) {
        return magazineRepository.findById(id);
    }

    public Magazine saveMagazine(String title, String description, String category,
                                 MultipartFile pdfFile, MultipartFile coverImage) throws IOException {
        
        if (pdfFile == null || pdfFile.isEmpty()) {
            throw new IllegalArgumentException("PDF file is required.");
        }

        // Store PDF file
        String pdfOriginalName = StringUtils.cleanPath(Objects.requireNonNull(pdfFile.getOriginalFilename()));
        String pdfExtension = getFileExtension(pdfOriginalName);
        if (!".pdf".equalsIgnoreCase(pdfExtension)) {
            throw new IllegalArgumentException("Uploaded file must be a PDF (.pdf).");
        }
        String pdfFileName = "mag_" + UUID.randomUUID() + ".pdf";
        Path targetPdfPath = uploadDir.resolve(pdfFileName);
        Files.copy(pdfFile.getInputStream(), targetPdfPath, StandardCopyOption.REPLACE_EXISTING);

        // Store Cover image if provided
        String coverFileName = null;
        if (coverImage != null && !coverImage.isEmpty()) {
            String coverOriginalName = StringUtils.cleanPath(Objects.requireNonNull(coverImage.getOriginalFilename()));
            String coverExt = getFileExtension(coverOriginalName);
            coverFileName = "cover_" + UUID.randomUUID() + coverExt;
            Path targetCoverPath = uploadDir.resolve(coverFileName);
            Files.copy(coverImage.getInputStream(), targetCoverPath, StandardCopyOption.REPLACE_EXISTING);
        }

        Magazine magazine = new Magazine(
                title.trim(),
                description.trim(),
                category != null && !category.trim().isEmpty() ? category.trim() : "General",
                pdfFileName,
                coverFileName,
                pdfFile.getSize()
        );

        return magazineRepository.save(magazine);
    }

    public boolean deleteMagazine(Long id) {
        Optional<Magazine> opt = magazineRepository.findById(id);
        if (opt.isPresent()) {
            Magazine mag = opt.get();
            try {
                if (mag.getPdfFileName() != null) {
                    Files.deleteIfExists(uploadDir.resolve(mag.getPdfFileName()));
                }
                if (mag.getCoverFileName() != null) {
                    Files.deleteIfExists(uploadDir.resolve(mag.getCoverFileName()));
                }
            } catch (IOException ignored) {
            }
            magazineRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = uploadDir.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
        } catch (MalformedURLException e) {
            // file not found
        }
        return null;
    }

    public long getTotalMagazinesCount() {
        return magazineRepository.count();
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex);
    }
}
