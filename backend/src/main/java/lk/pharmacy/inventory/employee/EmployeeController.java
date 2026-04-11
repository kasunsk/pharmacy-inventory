package lk.pharmacy.inventory.employee;

import jakarta.validation.Valid;
import lk.pharmacy.inventory.employee.dto.CreateEmployeeRequest;
import lk.pharmacy.inventory.employee.dto.EmployeeResponse;
import lk.pharmacy.inventory.employee.dto.UpdateEmployeeRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<EmployeeResponse> list() {
        return employeeService.list();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeResponse create(@Valid @RequestBody CreateEmployeeRequest request) {
        return employeeService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EmployeeResponse update(@PathVariable Long id, @RequestBody UpdateEmployeeRequest request) {
        return employeeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        employeeService.delete(id);
    }
}
