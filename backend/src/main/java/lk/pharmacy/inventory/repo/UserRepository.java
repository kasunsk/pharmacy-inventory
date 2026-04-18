package lk.pharmacy.inventory.repo;

import lk.pharmacy.inventory.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findAllByUsername(String username);
    Optional<User> findByUsernameAndTenantIsNull(String username);

    /** Eagerly joins tenant so listUsers() never triggers lazy-loading proxy issues. */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.tenant ORDER BY u.username ASC")
    List<User> findAllWithTenantEager();
    Optional<User> findByUsernameAndTenant_Id(String username, Long tenantId);
    Optional<User> findByUsernameAndTenant_CodeIgnoreCase(String username, String tenantCode);
    List<User> findByTenant_Id(Long tenantId);
    Page<User> findByTenant_Id(Long tenantId, Pageable pageable);
    List<User> findByTenantIsNull();
    Optional<User> findByIdAndTenant_Id(Long id, Long tenantId);
    @Query("select distinct u from User u left join fetch u.assignedPharmacies left join fetch u.defaultPharmacy where u.id = :id")
    Optional<User> findByIdWithPharmacies(Long id);
    @Query("select distinct u from User u left join fetch u.assignedPharmacies left join fetch u.defaultPharmacy where u.username = :username and u.tenant.id = :tenantId")
    Optional<User> findByUsernameAndTenant_IdWithPharmacies(String username, Long tenantId);
    @Query("select distinct u from User u left join fetch u.assignedPharmacies left join fetch u.defaultPharmacy where u.username = :username and lower(u.tenant.code) = lower(:tenantCode)")
    Optional<User> findByUsernameAndTenant_CodeIgnoreCaseWithPharmacies(String username, String tenantCode);
    @Query("select distinct u from User u left join fetch u.assignedPharmacies left join fetch u.defaultPharmacy where u.id = :id and u.tenant.id = :tenantId")
    Optional<User> findByIdAndTenant_IdWithPharmacies(Long id, Long tenantId);
    boolean existsByUsernameAndTenant_Id(String username, Long tenantId);
    long countByTenant_Id(Long tenantId);
}

