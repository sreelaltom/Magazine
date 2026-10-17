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
            seedInitialDataIfEmpty();
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

    /**
     * Seeds two starter magazines with valid lightweight sample PDFs so the project looks amazing right after launch!
     */
    private void seedInitialDataIfEmpty() {
        try {
            // Seed Tech Innovator if missing or update sample
            String samplePdfName1 = "sample_tech_innovator.pdf";
            Path samplePdfPath1 = uploadDir.resolve(samplePdfName1);
            if (!Files.exists(samplePdfPath1) || Files.size(samplePdfPath1) < 2000) {
                PdfGeneratorUtil.generateMagazinePdf(samplePdfPath1.toFile(),
                        "Tech Innovator Monthly", "Technology",
                        "Explore modern Java, AI frameworks, cloud native services and microarchitectures.");
            }

            // Seed Future Design
            String samplePdfName2 = "sample_future_design.pdf";
            Path samplePdfPath2 = uploadDir.resolve(samplePdfName2);
            if (!Files.exists(samplePdfPath2) || Files.size(samplePdfPath2) < 2000) {
                PdfGeneratorUtil.generateMagazinePdf(samplePdfPath2.toFile(),
                        "Design Spectrum - Summer Edition", "Design & Art",
                        "Modern typography, UI design systems, responsive layout guidelines, and digital products.");
            }

            // Seed NSS Chronicle
            String samplePdfName3 = "sample_nss_chronicle.pdf";
            Path samplePdfPath3 = uploadDir.resolve(samplePdfName3);
            if (!Files.exists(samplePdfPath3) || Files.size(samplePdfPath3) < 2000) {
                PdfGeneratorUtil.generateMagazinePdf(samplePdfPath3.toFile(),
                        "NSS Chronicle - Youth & Service", "NSS",
                        "Special camp reports, social service drives, blood donation camps, and community outreach.");
            }

            // Seed NCC Cadets
            String samplePdfName4 = "sample_ncc_cadets.pdf";
            Path samplePdfPath4 = uploadDir.resolve(samplePdfName4);
            if (!Files.exists(samplePdfPath4) || Files.size(samplePdfPath4) < 2000) {
                PdfGeneratorUtil.generateMagazinePdf(samplePdfPath4.toFile(),
                        "NCC Cadets - Unity and Discipline", "NCC",
                        "Commemorating Annual Training Camps, Republic Day parade contingents, and leadership honors.");
            }

            // Seed Sports Arena
            String samplePdfName5 = "sample_sports_arena.pdf";
            Path samplePdfPath5 = uploadDir.resolve(samplePdfName5);
            if (!Files.exists(samplePdfPath5) || Files.size(samplePdfPath5) < 2000) {
                PdfGeneratorUtil.generateMagazinePdf(samplePdfPath5.toFile(),
                        "Sports Arena - Championship Edition", "Sports",
                        "Championships, track and field records, inter-college tournament highlights, and athlete spotlights.");
            }

            if (magazineRepository.count() == 0) {
                magazineRepository.save(new Magazine(
                        "Tech Innovator Monthly - Issue 24",
                        "A comprehensive guide to modern Java developments, microservices architecture, and emerging trends in cloud computing.",
                        "Technology",
                        samplePdfName1,
                        null,
                        Files.size(samplePdfPath1)
                ));

                magazineRepository.save(new Magazine(
                        "Design Spectrum - Summer Edition",
                        "Discover modern typography, UI design systems, responsive layout guidelines, and sustainable digital products.",
                        "Design & Art",
                        samplePdfName2,
                        null,
                        Files.size(samplePdfPath2)
                ));
            }

            if (magazineRepository.findByCategoryIgnoreCaseOrderByUploadedAtDesc("NSS").isEmpty()) {
                magazineRepository.save(new Magazine(
                        "NSS Chronicle - Youth & Service",
                        "Highlights of annual rural immersion camps, tree plantation drives, health awareness rallies, and volunteer initiatives.",
                        "NSS",
                        samplePdfName3,
                        null,
                        Files.size(samplePdfPath3)
                ));
            }

            if (magazineRepository.findByCategoryIgnoreCaseOrderByUploadedAtDesc("NCC").isEmpty()) {
                magazineRepository.save(new Magazine(
                        "NCC Cadets - Unity and Discipline",
                        "Commemorating Annual Training Camps, Republic Day parade contingents, adventure training, and leadership achievements.",
                        "NCC",
                        samplePdfName4,
                        null,
                        Files.size(samplePdfPath4)
                ));
            }

            if (magazineRepository.findByCategoryIgnoreCaseOrderByUploadedAtDesc("Sports").isEmpty()) {
                magazineRepository.save(new Magazine(
                        "Sports Arena - Championship Edition",
                        "A celebration of sporting excellence: track & field records, inter-college tournament highlights, and athlete interviews.",
                        "Sports",
                        samplePdfName5,
                        null,
                        Files.size(samplePdfPath5)
                ));
            }

            System.out.println(">>> Multi-page magazines generated and seeded.");
        } catch (Exception e) {
            System.err.println("Sample seed skipped: " + e.getMessage());
        }
    }

    /**
     * Writes a minimal standard compliant PDF file (PDF 1.4) without requiring external heavy PDF libraries
     */
    private void createMinimalPdf(File file, String title, String body) throws IOException {
        String pdfContent = "%PDF-1.4\n" +
                "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n" +
                "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n" +
                "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj\n" +
                "4 0 obj << /Length 200 >> stream\n" +
                "BT\n" +
                "/F1 24 Tf\n" +
                "50 720 Td\n" +
                "(" + escapePdfText(title) + ") Tj\n" +
                "/F1 12 Tf\n" +
                "0 -40 Td\n" +
                "(" + escapePdfText(body) + ") Tj\n" +
                "0 -30 Td\n" +
                "(Digital Magazine Edition - Developed with Java & Spring Boot) Tj\n" +
                "ET\n" +
                "endstream\n" +
                "endobj\n" +
                "5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n" +
                "xref\n" +
                "0 6\n" +
                "0000000000 65535 f \n" +
                "0000000009 00000 n \n" +
                "0000000056 00000 n \n" +
                "0000000111 00000 n \n" +
                "0000000212 00000 n \n" +
                "0000000465 00000 n \n" +
                "trailer << /Size 6 /Root 1 0 R >>\n" +
                "startxref\n" +
                "536\n" +
                "%%EOF\n";

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(pdfContent.getBytes());
        }
    }

    private String escapePdfText(String text) {
        return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
