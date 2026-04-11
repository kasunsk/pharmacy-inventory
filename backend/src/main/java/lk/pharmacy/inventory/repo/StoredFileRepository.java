package lk.pharmacy.inventory.repo;

import lk.pharmacy.inventory.domain.FileKind;
import lk.pharmacy.inventory.domain.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    List<StoredFile> findBySaleIdAndKind(Long saleId, FileKind kind);
}

