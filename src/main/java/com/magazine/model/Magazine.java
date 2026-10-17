package com.magazine.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "magazines")
public class Magazine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    private String category;

    @Column(nullable = false)
    private String pdfFileName;

    private String coverFileName;

    private long fileSizeBytes;

    private LocalDateTime uploadedAt;

    public Magazine() {
        this.uploadedAt = LocalDateTime.now();
    }

    public Magazine(String title, String description, String category, String pdfFileName, String coverFileName, long fileSizeBytes) {
        this.title = title;
        this.description = description;
        this.category = category != null && !category.trim().isEmpty() ? category : "General";
        this.pdfFileName = pdfFileName;
        this.coverFileName = coverFileName;
        this.fileSizeBytes = fileSizeBytes;
        this.uploadedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPdfFileName() {
        return pdfFileName;
    }

    public void setPdfFileName(String pdfFileName) {
        this.pdfFileName = pdfFileName;
    }

    public String getCoverFileName() {
        return coverFileName;
    }

    public void setCoverFileName(String coverFileName) {
        this.coverFileName = coverFileName;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getFormattedDate() {
        if (uploadedAt == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        return uploadedAt.format(formatter);
    }

    public String getFormattedSize() {
        if (fileSizeBytes < 1024) {
            return fileSizeBytes + " B";
        } else if (fileSizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", fileSizeBytes / 1024.0);
        } else {
            return String.format("%.2f MB", fileSizeBytes / (1024.0 * 1024.0));
        }
    }
}
