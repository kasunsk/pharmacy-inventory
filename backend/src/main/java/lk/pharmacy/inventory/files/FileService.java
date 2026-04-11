package lk.pharmacy.inventory.files;

import lk.pharmacy.inventory.domain.FileKind;
import lk.pharmacy.inventory.domain.Sale;
import lk.pharmacy.inventory.domain.StoredFile;
import lk.pharmacy.inventory.domain.User;
import lk.pharmacy.inventory.exception.ApiException;
import lk.pharmacy.inventory.repo.SaleRepository;
import lk.pharmacy.inventory.repo.StoredFileRepository;
import lk.pharmacy.inventory.util.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class FileService {

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final StoredFileRepository storedFileRepository;
    private final SaleRepository saleRepository;
    private final CurrentUserService currentUserService;
    private final SignedUrlService signedUrlService;

    public FileService(StoredFileRepository storedFileRepository,
                       SaleRepository saleRepository,
                       CurrentUserService currentUserService,
                       SignedUrlService signedUrlService) {
        this.storedFileRepository = storedFileRepository;
        this.saleRepository = saleRepository;
        this.currentUserService = currentUserService;
        this.signedUrlService = signedUrlService;
    }

    public Map<String, Object> upload(Long saleId, FileKind kind, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException("File is empty");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ApiException("File too large");
        }

        Sale sale = saleRepository.findById(saleId).orElseThrow(() -> new ApiException("Sale not found"));
        User currentUser = currentUserService.getCurrentUser();

        StoredFile storedFile = new StoredFile();
        storedFile.setKind(kind);
        storedFile.setSale(sale);
        storedFile.setUploadedBy(currentUser);
        storedFile.setOriginalFileName(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        storedFile.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        storedFile.setSize(file.getSize());
        try {
            storedFile.setData(file.getBytes());
        } catch (IOException e) {
            throw new ApiException("Unable to read uploaded file");
        }
        storedFileRepository.save(storedFile);

        return Map.of(
                "id", storedFile.getId(),
                "saleId", saleId,
                "kind", storedFile.getKind(),
                "size", storedFile.getSize(),
                "contentType", storedFile.getContentType());
    }

    public Map<String, String> createSignedViewUrl(Long fileId) {
        StoredFile storedFile = storedFileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException("File not found"));

        String token = signedUrlService.createToken(fileId);
        String url = "/files/view/" + storedFile.getId() + "?token=" + token;
        return Map.of("url", url, "expiresInMs", "300000");
    }

    public StoredFile getBySignedUrl(Long fileId, String token) {
        signedUrlService.validateToken(fileId, token);
        return storedFileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException("File not found"));
    }
}

