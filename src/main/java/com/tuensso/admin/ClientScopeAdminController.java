package com.tuensso.admin;

import java.util.List;
import java.util.UUID;

import com.tuensso.scope.ClientScope;
import com.tuensso.scope.ClientScopeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/scopes")
public class ClientScopeAdminController {

    private final ClientScopeRepository repo;

    public ClientScopeAdminController(ClientScopeRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<ClientScope> list() { return repo.findAll(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientScope create(@RequestBody CreateScopeRequest req) {
        if (repo.existsByName(req.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Scope already exists");
        }
        ClientScope scope = new ClientScope();
        scope.setName(req.name());
        scope.setDescription(req.description());
        scope.setClaimName(req.claimName());
        scope.setClaimValue(req.claimValue());
        return repo.save(scope);
    }

    @PutMapping("/{id}")
    public ClientScope update(@PathVariable UUID id, @RequestBody CreateScopeRequest req) {
        ClientScope scope = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        scope.setDescription(req.description());
        scope.setClaimName(req.claimName());
        scope.setClaimValue(req.claimValue());
        return repo.save(scope);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        repo.deleteById(id);
    }

    public record CreateScopeRequest(String name, String description, String claimName, String claimValue) {}
}
