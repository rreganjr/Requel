package com.rreganjr.requel.requirements;

import com.rreganjr.platform.command.AuthorizationException;
import com.rreganjr.requel.project.*;
import com.rreganjr.requel.project.Requirement.*;
import com.rreganjr.requel.project.impl.IdeaImpl;
import com.rreganjr.requel.project.impl.RequirementImpl;
import com.rreganjr.requel.service.auth.CurrentUserResolver;
import com.rreganjr.requel.service.audit.CommandAuditLog;
import com.rreganjr.requel.service.command.IdeaFileText;
import com.rreganjr.requel.user.User;
import com.rreganjr.requel.user.WorkflowRole;
import com.rreganjr.requel.user.impl.SystemAdminUserRole;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Local requirement review API. External generation and Jama operations fail explicitly until configured. */
@RestController
@RequestMapping("/api/requirements")
@Transactional
public class RequirementController {
    @PersistenceContext private EntityManager em;
    private final ProjectRepository projects;
    private final CurrentUserResolver users;
    public RequirementController(ProjectRepository projects, CurrentUserResolver users) {
        this.projects = projects; this.users = users;
    }
    public record Create(Long ideaId, String shallText, String requirementId) { }
    public record Edit(String shallText, Long version, String requirementId) { }
    public record Version(Long version) { }
    public record ReviewRequest(Department department, Result result, String comments, Long version) { }
    public record Submit(String sourceText) { }
    private record Caller(Project project, User user) { }

    private Caller caller(String name, String permission, Class<?> type) {
        Project p = projects.findProjectByName(name);
        User u = users.resolve();
        if (permission != null) {
            StakeholderAuthorizationChecker.require(p, u, type, permission);
        } else if (!u.hasRole(SystemAdminUserRole.class)) {
            try {
                if (p.getUserStakeholder(u) == null) throw new AuthorizationException("Not a stakeholder");
            } catch (RuntimeException e) { throw new AuthorizationException("Not a stakeholder of this project"); }
        }
        return new Caller(p, u);
    }
    private static List<Department> allowedReviewDepartments(User user) {
        if (user.hasRole(SystemAdminUserRole.class)) return List.of(Department.values());
        WorkflowRole role = user.getWorkflowRole();
        if (role == null) return List.of();
        return switch (role) {
            case ANALYST -> List.of(Department.QUALITY);
            case ENGINEER -> List.of(Department.FEASIBILITY);
            case TEST_ENGINEER -> List.of(Department.TESTABILITY);
            case PRODUCT_MANAGER -> List.of();
        };
    }
    private boolean manager(Caller c) { return c.user.getWorkflowRole() == WorkflowRole.PRODUCT_MANAGER; }
    private void requireManager(Caller c) {
        if (!manager(c)) throw new AuthorizationException("Only Product Managers can approve or reopen requirements.");
    }
    private RequirementImpl requirement(Caller c, long id) {
        RequirementImpl r = em.find(RequirementImpl.class, id);
        if (r == null || !r.getProjectId().equals(c.project.getId())) throw missing();
        return r;
    }
    private IdeaImpl idea(Caller c, long id) {
        IdeaImpl i = em.find(IdeaImpl.class, id);
        if (i == null || !i.getProjectId().equals(c.project.getId())) throw missing();
        return i;
    }
    private static ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found in this project."); }
    private static void version(RequirementImpl r, Long expected) {
        if (expected == null) throw new IllegalArgumentException("A version is required.");
        if (r.getVersion() != expected) throw new ResponseStatusException(HttpStatus.CONFLICT, "This requirement changed. Reload before saving.");
    }
    private void audit(Caller c, String operation, Long id) {
        em.persist(new CommandAuditLog(c.user.getId(), Instant.now(), operation,
            getClass().getName(), c.project.getId(), "{\"entityId\":" + id + "}"));
    }
    private Map<String, Object> view(Caller c, RequirementImpl r) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", r.getId()); v.put("version", r.getVersion());
        v.put("requirementId", r.getRequirementCode()); v.put("shallText", r.getShallText());
        v.put("status", r.getStatus()); v.put("ideaId", r.getIdea() == null ? null : r.getIdea().getId());
        v.put("ideaName", r.getIdea() == null ? null : "Idea " + r.getIdea().getId()); v.put("jamaItemId", null);
        v.put("createdBy", r.getCreatedBy()); v.put("createdAt", r.getCreatedAt()); v.put("updatedAt", r.getUpdatedAt());
        v.put("allReviewsPassed", r.allReviewsPassed());
        v.put("allowedReviewDepartments", allowedReviewDepartments(c.user));
        v.put("canApproveOrReopen", manager(c));
        v.put("isApproved", r.getStatus() == Status.APPROVED || r.getStatus() == Status.PUBLISHED);
        v.put("isPublished", r.getStatus() == Status.PUBLISHED);
        v.put("reviews", r.getReviews().entrySet().stream().map(e -> {
            var review = e.getValue();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId() * 10 + e.getKey().ordinal()); item.put("version", r.getVersion());
            item.put("department", e.getKey()); item.put("result", review.getResult());
            item.put("reviewer", review.getReviewer()); item.put("reviewerRole", review.getReviewerRole());
            item.put("comments", review.getComments()); item.put("reviewedAt", review.getReviewedAt());
            item.put("createdAt", review.getReviewedAt()); item.put("updatedAt", review.getReviewedAt());
            return item;
        }).toList());
        return v;
    }
    private Map<String, Object> ideaView(IdeaImpl i) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("id", i.getId()); v.put("version", i.getVersion()); v.put("externalId", null);
        v.put("sourceText", i.getText()); v.put("status", i.getStatus());
        v.put("createdBy", i.getCreatedBy() == null ? null : i.getCreatedBy().getUsername());
        v.put("createdAt", i.getDateCreated());
        v.put("derivedRequirements", i.getDerivedRequirements().stream().map(r -> Map.of(
            "id", r.getId(), "name", "REQ-" + r.getId(), "type", "Requirement")).toList());
        return v;
    }
    @GetMapping
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(@RequestParam String projectName, @RequestParam(required = false) Status status) {
        Caller c = caller(projectName, null, null);
        return em.createQuery("select r from RequirementImpl r where r.projectId = :p order by r.id desc", RequirementImpl.class)
            .setParameter("p", c.project.getId()).getResultList().stream()
            .filter(r -> status == null || r.getStatus() == status).map(r -> view(c, r)).toList();
    }
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public Map<String, Object> get(@RequestParam String projectName, @PathVariable long id) {
        Caller c = caller(projectName, null, null);
        return view(c, requirement(c, id));
    }
    @PostMapping
    public Map<String, Object> create(@RequestParam String projectName, @RequestBody Create body) {
        Caller c = caller(projectName, "Edit", Project.class);
        RequirementImpl r = new RequirementImpl(c.project.getId(), body.ideaId == null ? null : idea(c, body.ideaId), body.shallText, c.user.getUsername());
        r.setRequirementCode(body.requirementId);
        em.persist(r); em.flush(); audit(c, "CreateRequirement", r.getId()); return view(c, r);
    }
    @PutMapping("/{id}")
    public Map<String, Object> edit(@RequestParam String projectName, @PathVariable long id, @RequestBody Edit body) {
        Caller c = caller(projectName, "Edit", Project.class);
        RequirementImpl r = requirement(c, id); version(r, body.version); r.revise(body.shallText); r.setRequirementCode(body.requirementId);
        em.flush(); audit(c, "EditRequirement", id); return view(c, r);
    }
    @PostMapping("/{id}/reviews")
    public Map<String, Object> review(@RequestParam String projectName, @PathVariable long id, @RequestBody ReviewRequest body) {
        Caller c = caller(projectName, null, null);
        if (body.department == null) throw new IllegalArgumentException("A department is required.");
        if (!allowedReviewDepartments(c.user).contains(body.department)) {
            throw new AuthorizationException("Your workflow role cannot submit this department review.");
        }
        RequirementImpl r = requirement(c, id); version(r, body.version);
        String reviewerRole = c.user.hasRole(SystemAdminUserRole.class)
                ? "System Administrator" : c.user.getWorkflowRole().getDisplayName();
        r.review(body.department, body.result, body.comments, c.user.getUsername(), reviewerRole);
        em.flush(); audit(c, "ReviewRequirement", id); return view(c, r);
    }
    @PostMapping("/{id}/approve")
    public Map<String, Object> approve(@RequestParam String projectName, @PathVariable long id, @RequestBody Version body) {
        Caller c = caller(projectName, null, null); requireManager(c);
        RequirementImpl r = requirement(c, id); version(r, body.version); r.approve();
        em.flush(); audit(c, "ApproveRequirement", id); return view(c, r);
    }
    @PostMapping("/{id}/reopen")
    public Map<String, Object> reopen(@RequestParam String projectName, @PathVariable long id, @RequestBody Version body) {
        Caller c = caller(projectName, null, null); requireManager(c);
        RequirementImpl r = requirement(c, id); version(r, body.version); r.reopen();
        em.flush(); audit(c, "ReopenRequirement", id); return view(c, r);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam String projectName, @PathVariable long id, @RequestParam long version) {
        Caller c = caller(projectName, "Delete", Project.class);
        RequirementImpl r = requirement(c, id); version(r, version);
        if (r.getStatus() != Status.DRAFT) throw new IllegalStateException("Only drafts may be deleted.");
        em.remove(r); audit(c, "DeleteRequirement", id);
    }
    @GetMapping("/ideas")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> ideas(@RequestParam String projectName) {
        Caller c = caller(projectName, null, null);
        return em.createQuery("select i from IdeaImpl i where i.projectId = :p order by i.id desc", IdeaImpl.class)
            .setParameter("p", c.project.getId()).getResultList().stream().map(this::ideaView).toList();
    }
    @GetMapping("/ideas/{id}")
    @Transactional(readOnly = true)
    public Map<String, Object> getIdea(@RequestParam String projectName, @PathVariable long id) {
        return ideaView(idea(caller(projectName, null, null), id));
    }
    @PostMapping(value = "/ideas", consumes = "application/json")
    public Map<String, Object> submit(@RequestParam String projectName, @RequestBody Submit body) {
        return submit(projectName, body.sourceText, null);
    }
    @PostMapping(value = "/ideas", consumes = "multipart/form-data")
    public Map<String, Object> submitFile(@RequestParam String projectName,
            @RequestParam(defaultValue = "") String sourceText, @RequestParam(required = false) MultipartFile file) {
        String text = sourceText + (file == null ? "" : "\n\n" + IdeaFileText.read(file));
        return submit(projectName, text, file == null ? null : file.getOriginalFilename());
    }
    private Map<String, Object> submit(String projectName, String text, String filename) {
        Caller c = caller(projectName, "Edit", Project.class);
        if (text == null || text.strip().length() < 10) throw new IllegalArgumentException("An idea needs at least 10 characters.");
        IdeaImpl i = new IdeaImpl(c.project.getId(), text.strip(), filename, c.user);
        em.persist(i); em.flush(); audit(c, "SubmitWorkflowIdea", i.getId()); return ideaView(i);
    }
    @PostMapping("/{id}/publish")
    public void publish(@RequestParam String projectName, @PathVariable long id, @RequestBody Version body) {
        Caller c = caller(projectName, "Edit", Project.class);
        RequirementImpl r = requirement(c, id); version(r, body.version);
        if (r.getStatus() != Status.APPROVED) throw new IllegalStateException("Approve this requirement before publishing.");
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Jama publishing is not configured. Nothing has been published.");
    }
    @GetMapping("/jama/search")
    public void jamaSearch(@RequestParam String projectName) {
        caller(projectName, null, null);
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Jama search is not configured.");
    }
    @PostMapping({"/{id}/compare", "/comparisons/{id}/resolve", "/ideas/{id}/breakdown-review", "/ideas/clarifications/{id}/answer"})
    public void unconfigured(@RequestParam String projectName, @PathVariable long id) {
        caller(projectName, "Edit", Project.class);
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "AI clarification, decomposition and comparison are not connected to this workflow yet.");
    }
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<?> forbidden(AuthorizationException e) { return ResponseEntity.status(403).body(Map.of("message", e.getMessage())); }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalid(IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("message", e.getMessage())); }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> conflict(IllegalStateException e) { return ResponseEntity.status(409).body(Map.of("message", e.getMessage())); }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", Objects.requireNonNullElse(e.getReason(), "Request failed.")));
    }
}
