package lk.pharmacy.inventory.employee;

import jakarta.validation.Valid;
import lk.pharmacy.inventory.employee.dto.CreateEmployeeRequest;
import lk.pharmacy.inventory.employee.dto.EmployeeResponse;
import lk.pharmacy.inventory.employee.dto.UpdateEmployeeRequest;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<EmployeeResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return employeeService.list(page, size);
    }

    @PostMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeResponse create(@Valid @RequestBody CreateEmployeeRequest request) {
        return employeeService.create(request);
    }

    @PutMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeResponse update(@PathVariable Long id, @RequestBody UpdateEmployeeRequest request) {
        return employeeService.update(id, request);
    }

    @DeleteMapping("/employees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        employeeService.delete(id);
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public EmployeeResponse profile() {
        return employeeService.profile();
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public EmployeeResponse updateProfile(@RequestBody UpdateEmployeeRequest request) {
        return employeeService.updateProfile(request);
    }
}
