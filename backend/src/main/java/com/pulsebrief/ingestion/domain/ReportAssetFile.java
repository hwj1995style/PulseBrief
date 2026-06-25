package com.pulsebrief.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "report_asset_file")
public class ReportAssetFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_hash")
    private String fileHash;

    @Column(name = "storage_provider")
    private String storageProvider;

    @Column(name = "storage_path")
    private String storagePath;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "downloaded_at")
    private LocalDateTime downloadedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected ReportAssetFile() {
    }

    public ReportAssetFile(
            String fileHash,
            String storageProvider,
            String storagePath,
            String fileName,
            Long fileSizeBytes,
            String mimeType
    ) {
        LocalDateTime now = LocalDateTime.now();
        this.fileHash = fileHash;
        this.storageProvider = storageProvider;
        this.storagePath = storagePath;
        this.fileName = fileName;
        this.fileSizeBytes = fileSizeBytes;
        this.mimeType = mimeType;
        this.downloadedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getFileHash() {
        return fileHash;
    }

    public String getStorageProvider() {
        return storageProvider;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getFileName() {
        return fileName;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getMimeType() {
        return mimeType;
    }

    public LocalDateTime getDownloadedAt() {
        return downloadedAt;
    }
}
