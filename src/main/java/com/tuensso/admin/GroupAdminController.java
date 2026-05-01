package com.tuensso.admin;

import java.util.List;
import java.util.UUID;

import com.tuensso.group.GroupAttribute;
import com.tuensso.group.GroupAttributeRepository;
import com.tuensso.group.UserGroup;
import com.tuensso.group.UserGroupRepository;
import com.tuensso.user.UserAccount;
import com.tuensso.user.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/groups")
public class GroupAdminController {

    private final UserGroupRepository groupRepo;
    private final UserAccountRepository userRepo;
    private final GroupAttributeRepository attrRepo;

    public GroupAdminController(UserGroupRepository groupRepo, UserAccountRepository userRepo,
                                GroupAttributeRepository attrRepo) {
        this.groupRepo = groupRepo;
        this.userRepo = userRepo;
        this.attrRepo = attrRepo;
    }

    @GetMapping
    public List<GroupView> list() {
        return groupRepo.findAll().stream()
                .map(group -> new GroupView(
                        group.getId(),
                        group.getName(),
                        group.getDescription(),
                        userRepo.countByGroups_Id(group.getId())))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupView create(@RequestBody CreateGroupRequest request) {
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (groupRepo.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "group already exists");
        }

        UserGroup group = new UserGroup();
        group.setName(name);
        group.setDescription(request.description());
        UserGroup saved = groupRepo.save(group);
        return new GroupView(saved.getId(), saved.getName(), saved.getDescription(), 0);
    }

    @GetMapping("/{groupId}")
    public GroupView get(@PathVariable UUID groupId) {
        UserGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new GroupView(group.getId(), group.getName(), group.getDescription(),
                userRepo.countByGroups_Id(group.getId()));
    }

    @PutMapping("/{groupId}")
    public GroupView update(@PathVariable UUID groupId, @RequestBody CreateGroupRequest req) {
        UserGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (req.name() != null && !req.name().isBlank()) group.setName(req.name().trim());
        if (req.description() != null) group.setDescription(req.description().trim());
        UserGroup saved = groupRepo.save(group);
        return new GroupView(saved.getId(), saved.getName(), saved.getDescription(),
                userRepo.countByGroups_Id(saved.getId()));
    }

    @PostMapping("/{groupId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void addMember(@PathVariable UUID groupId, @PathVariable UUID userId) {
        UserAccount user = userRepo.findWithGroupsById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        UserGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        user.getGroups().add(group);
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void removeMember(@PathVariable UUID groupId, @PathVariable UUID userId) {
        UserAccount user = userRepo.findWithGroupsById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.getGroups().removeIf(group -> group.getId().equals(groupId));
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID groupId) {
        if (!groupRepo.existsById(groupId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found");
        }
        groupRepo.deleteById(groupId);
    }

    public record CreateGroupRequest(String name, String description) {
    }

    public record GroupView(UUID id, String name, String description, long memberCount) {
    }

    // Group attributes
    @GetMapping("/{groupId}/attributes")
    public List<GroupAttribute> getAttributes(@PathVariable UUID groupId) {
        if (!groupRepo.existsById(groupId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return attrRepo.findByGroupId(groupId);
    }

    @PutMapping("/{groupId}/attributes")
    public GroupAttribute setAttribute(@PathVariable UUID groupId, @RequestBody AttributeRequest req) {
        if (!groupRepo.existsById(groupId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        GroupAttribute attr = attrRepo.findByGroupIdAndKey(groupId, req.key()).orElseGet(() -> {
            GroupAttribute a = new GroupAttribute();
            a.setGroupId(groupId);
            a.setKey(req.key());
            return a;
        });
        attr.setValue(req.value());
        return attrRepo.save(attr);
    }

    @Transactional
    @DeleteMapping("/{groupId}/attributes/{key}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttribute(@PathVariable UUID groupId, @PathVariable String key) {
        attrRepo.deleteByGroupIdAndKey(groupId, key);
    }

    public record AttributeRequest(String key, String value) {}
}