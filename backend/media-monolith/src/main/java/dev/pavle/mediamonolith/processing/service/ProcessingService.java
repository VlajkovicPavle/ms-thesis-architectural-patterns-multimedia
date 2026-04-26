package dev.pavle.mediamonolith.processing.service;

import dev.pavle.mediamonolith.processing.repository.FileRepository;
import org.springframework.stereotype.Service;

@Service
public class ProcessingService {

    private final FileRepository fileRepository;

    public ProcessingService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public void extractMetadata(){
    }
}
