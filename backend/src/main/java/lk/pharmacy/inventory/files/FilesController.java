package lk.pharmacy.inventory.files;

import lk.pharmacy.inventory.domain.FileKind;
import lk.pharmacy.inventory.domain.StoredFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/files")
public class FilesController {

    private final FileService fileService;

    public FilesController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/sales/{saleId}/receipt")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYER')")
    public Map<String, Object> uploadReceipt(@PathVariable Long saleId,
                                             @RequestParam("file") MultipartFile file) {
        return fileService.upload(saleId, FileKind.RECEIPT, file);
    }

    @PostMapping("/sales/{saleId}/prescription")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYER')")
    public Map<String, Object> uploadPrescription(@PathVariable Long saleId,
                                                  @RequestParam("file") MultipartFile file) {
        return fileService.upload(saleId, FileKind.PRESCRIPTION, file);
    }

    @GetMapping("/{fileId}/signed-url")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYER')")
    public Map<String, String> signedUrl(@PathVariable Long fileId) {
        return fileService.createSignedViewUrl(fileId);
    }

    @GetMapping("/view/{fileId}")
    public ResponseEntity<byte[]> view(@PathVariable Long fileId,
                                       @RequestParam String token) {
        StoredFile storedFile = fileService.getBySignedUrl(fileId, token);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            mediaType = MediaType.parseMediaType(storedFile.getContentType());
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + storedFile.getOriginalFileName() + "\"")
                .body(storedFile.getData());
    }
}

