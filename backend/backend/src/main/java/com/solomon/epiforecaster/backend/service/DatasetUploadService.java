package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.DatasetUpload;
import com.solomon.epiforecaster.backend.repository.DatasetUploadRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatasetUploadService {

    private final DatasetUploadRepository repository;

    public DatasetUploadService(DatasetUploadRepository repository) {
        this.repository = repository;
    }

    public List<DatasetUpload> getAll() {
        return repository.findAll();
    }

    public DatasetUpload getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public DatasetUpload create(DatasetUpload upload) {
        return repository.save(upload);
    }

    public DatasetUpload update(Long id, DatasetUpload upload) {

        DatasetUpload existing = repository.findById(id).orElse(null);

        if (existing == null) {
            return null;
        }

        existing.setDataset(upload.getDataset());
        existing.setFileName(upload.getFileName());
        existing.setFileType(upload.getFileType());
        existing.setFileSize(upload.getFileSize());
        existing.setUploadStatus(upload.getUploadStatus());

        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}